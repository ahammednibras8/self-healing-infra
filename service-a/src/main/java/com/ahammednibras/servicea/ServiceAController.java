package com.ahammednibras.servicea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/service-a")
public class ServiceAController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAController.class);
    private final WebClient.Builder webClientBuilder;

    @Value("${SERVICEB_URL}")
    private String serviceBUrl;

    public ServiceAController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/hello")
    public String helloA() {
        return "Hello from Service A!";
    }

    @GetMapping("/call-service-b")
    @CircuitBreaker(name = "serviceB", fallbackMethod = "callServiceBFallback")
    public Mono<String> callServiceB() {
        logger.info("Service A: Attempting to call Service B at {}", serviceBUrl);
        return webClientBuilder.baseUrl(serviceBUrl)
        .build()
        .get()
        .uri("/service-b/hello")
        .retrieve()
        .bodyToMono(String.class)
        .doOnError(e -> logger.error("Service A: Error during WebClient call to Service B: {}", e.getMessage()));
    }

    // Fallback Method for the circuit breaker
    public Mono<String> callServiceBFallback(Throwable t) {
        logger.warn("Service A: Fallback executed for Service B call. Circuit Breaker state change or error: {}", t.getMessage());
        return Mono.just("Service B is currently unavailable or experiencing issue. Returning a fallback message from Service A.");
    }
}
