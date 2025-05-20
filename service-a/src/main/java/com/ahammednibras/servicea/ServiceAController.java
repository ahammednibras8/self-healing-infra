package com.ahammednibras.servicea;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/service-a")
public class ServiceAController {
    private final WebClient webClient;

    public ServiceAController(@Value("${serviceb.url:http://localhost:8081}") String serviceBUrl) {
        this.webClient = WebClient.builder().baseUrl(serviceBUrl).build();
    }

    @GetMapping("/hello")
    public String helloA() {
        return "Hello from Service A!";
    }

    @GetMapping("/call-service-b")
    public Mono<String> callServiceB() {
        return webClient.get()
                .uri("/service-b/hello")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> "Service A called Service B: " + response)
                .onErrorReturn("Service A failed to call Service B");
    }

    // TODO: Add Methods to demonstrate Resilience4j patterns
}
