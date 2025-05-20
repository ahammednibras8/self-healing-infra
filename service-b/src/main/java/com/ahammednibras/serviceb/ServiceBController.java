package com.ahammednibras.serviceb;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/service-b")
public class ServiceBController {
    @GetMapping("/hello")
    public String helloB() {
        return "Hello from Service B!";
    }

    // TODO: Endpoint to simulate failure or latency
}
