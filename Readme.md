# Configuration for Service A

# Spring Application Name - Used for metrics and logging
spring:
  application:
    name: service-a

# Server settings
server:
  port: 8080

# OpenTelemetry Configuration
management:
  endpoints:
    web:
      exposure:
        include: "*"
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
    metrics:
      endpoint: http://otel-collector:4318/v1/metrics
  metrics:
    export:
      prometheus:
        enabled: true

# Resilience4j Circuit Breaker Configuration
resilience4j:
  circuitbreaker:
    configs:
      default: # A default configuration that other instances can inherit from
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
        permittedNumberOfCallsInHalfOpenState: 3
    instances:
      serviceB: # Define a specific circuit breaker instance named 'serviceB'
        baseConfig: default
        recordExceptions:
          - org.springframework.web.reactive.function.client.WebClientRequestException

  # NEW: Resilience4j Retry Configuration
  retry:
    configs:
      default: # Default retry configuration
        maxAttempts: 3 # Maximum number of retry attempts
        waitDuration: 1s # Initial wait duration between retries (1 second)
        retryExceptions: # Exceptions that should trigger a retry
          - org.springframework.web.reactive.function.client.WebClientRequestException
          - java.util.concurrent.TimeoutException # Example: if a timeout occurs
    instances:
      serviceBRetry: # Define a specific retry instance named 'serviceBRetry'
        baseConfig: default # Inherit from the default config
