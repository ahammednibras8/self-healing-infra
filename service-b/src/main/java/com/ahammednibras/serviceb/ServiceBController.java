package com.ahammednibras.serviceb;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/service-b")
public class ServiceBController {
    private static final Logger logger = LoggerFactory.getLogger(ServiceBController.class);

    @GetMapping("/hello")
    public String helloB() {
        return "Hello from Service B!";
    }

    // This endpoint introduces an artificial delay to simulate a slow response.
    @GetMapping("/slow")
    public Mono<String> slowB() {
        long delayMillis = 3000;
        logger.info("Service B: Recieved /slow request, delaying for {} ms", delayMillis);
        return Mono.delay(java.time.Duration.ofMillis(delayMillis))
                .map(l -> {
                    logger.info("Service B: Responding to /slow request after delay.");
                    return "Hello from Service B (after a delay)!";
                });
    }
}
