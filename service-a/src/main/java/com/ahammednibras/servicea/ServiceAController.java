package com.ahammednibras.servicea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/service-a")
public class ServiceAController {
    private static final Logger logger = LoggerFactory.getLogger(ServiceAController.class);

    private final WebClient.Builder webClientBuilder;
    private final ServiceBService serviceBService;

    @Value("${SERVICEB_URL}")
    private String serviceBUrl;

    public ServiceAController(WebClient.Builder webClientBuilder, ServiceBService serviceBService) {
        this.webClientBuilder = webClientBuilder;
        this.serviceBService = serviceBService;
    }

    @GetMapping("/hello")
    public String helloA() {
        return "Hello from Service A!";
    }

    @GetMapping("/call-service-b")
    @Retry(name = "serviceBRetry")
    public Mono<String> callServiceB() {
        logger.info("Service A: Initiating call to Service B (via Retry)");
        return serviceBService.callServiceBProtectedByCircuitBreaker(serviceBUrl)
                .onErrorResume(t -> {
                    logger.warn("Service A: Final Fallback executed. Error: {}", t.getMessage());
                    return Mono.just(
                            "Service B is currently unavailable or experiencing issues. Returning a fallback message from Service A.");
                });
    }
}

// A separate component to encapsulate the circuit breaker
@Component
class ServiceBService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceBService.class);
    private final WebClient.Builder webClientBuilder;

    public ServiceBService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    // This method is protected by circuit breaker
    @TimeLimiter(name = "serviceBTimeLimiter")
    @CircuitBreaker(name = "serviceB")
    public Mono<String> callServiceBProtectedByCircuitBreaker(String serviceBUrl) {
        logger.info("Service B Service: Attempting to call Service B at {}", serviceBUrl);
        return webClientBuilder.baseUrl(serviceBUrl)
                .build()
                .get()
                .uri("/service-b/slow")
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> logger.error("Service B Service: Error during WebClient call to Service B: {}",
                        e.getMessage()));
    }
}
