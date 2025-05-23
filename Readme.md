# Self-Healing Infrastructure for Microservices

> Building Resilient, Fault-Tolerant Systems That Never Go Down

## The Problem We Obliterate: Cascading Failures in Distributed Systems

In today's complex microservice architectures, the failure of a single downstream service can trigger a catastrophic chain reaction, leading to widespread outages, degraded user experience, and significant operational costs. Traditional error handling often falls short, allowing a small hiccup to bring down an entire system. This project tackles that fundamental pain point head-on by demonstrating how to build truly self-healing infrastructure.

## Our Approach: Proactive Resilience with Observability

We're not just building a system; we're building an immune system for microservices. This project showcases how to integrate robust fault-tolerance patterns with comprehensive observability to create applications that can detect, isolate, and recover from failures autonomously.

## Core Technologies Used

- **Spring Boot 3 (Java 21)**: For building lightweight, performant microservices (service-a, service-b)
- **Spring WebFlux**: For building reactive, non-blocking web applications, crucial for efficient resource utilization in distributed systems
- **Resilience4j**: A lightweight, easy-to-use fault tolerance library for Java, providing patterns like Circuit Breaker, Retry, Rate Limiter, and Time Limiter. This is our core "healing" mechanism
- **OpenTelemetry**: Vendor-neutral APIs, SDKs, and tooling for generating, collecting, and exporting telemetry data (metrics, traces, logs). This provides the "eyes and ears" for our self-healing system
- **OpenTelemetry Collector**: A powerful, vendor-agnostic proxy for processing and exporting telemetry data to various backends
- **Prometheus**: A leading open-source monitoring system for collecting and storing time-series data (metrics)
- **Grafana**: The open-source platform for analytics and interactive visualization, used to create dashboards for real-time system health monitoring
- **Jaeger**: An open-source, end-to-end distributed tracing system for monitoring and troubleshooting complex microservice environments
- **Docker & Docker Compose**: For containerizing applications and orchestrating the entire observability stack and microservices

## Self-Healing in Action: The Circuit Breaker

The Circuit Breaker is a critical pattern for preventing cascading failures. It acts as a safety mechanism, preventing a service from repeatedly invoking a failing dependency, thereby protecting both the calling service and the overloaded dependency.

### How It Works Here:

#### Problem
`service-a` depends on `service-b`. If `service-b` becomes unresponsive (e.g., stopped, network issues), `service-a` would typically hang or throw connection errors, potentially exhausting its own resources or thread pools.

#### Solution (Resilience4j Circuit Breaker)

- `service-a`'s call to `service-b` (`/service-a/call-service-b`) is wrapped with a Resilience4j Circuit Breaker
- When `service-b` fails (e.g., connection refused), the Circuit Breaker detects these failures
- After a configured number of failures (`minimumNumberOfCalls`, `failureRateThreshold`), the circuit transitions from CLOSED to OPEN
- In the OPEN state, subsequent calls to `service-b` are immediately rejected (fast-fail) without attempting a network call. This prevents `service-a` from wasting resources and provides an immediate fallback response
- After a `waitDurationInOpenState`, the circuit moves to HALF_OPEN, allowing a few "test" calls to `service-b`
- If these test calls succeed, the circuit returns to CLOSED. If they fail, it immediately re-opens

#### Graceful Degradation
A `fallbackMethod` is configured, ensuring that even when `service-b` is down or the circuit is OPEN, `service-a` returns a user-friendly message instead of an error, maintaining a positive user experience.

#### Observability
All Circuit Breaker state transitions (CLOSED, OPEN, HALF_OPEN) and call metrics (successful, failed, not_permitted) are automatically exported to Prometheus via Micrometer and visualized in real-time in Grafana. This provides immediate insight into the health and resilience of your services.

## Demonstration: Witnessing Self-Healing

Follow these steps to observe the Circuit Breaker in action and see the self-healing capabilities:

### Prerequisites

- Docker and Docker Compose installed
- Maven installed
- Java 21 JDK installed

### Clone the Repository

```bash
git clone <YOUR_REPOSITORY_URL_HERE>
cd self-healing-infra
```

### Build the Project

```bash
mvn clean install
```

This will compile `service-a` and `service-b` and package them into JARs.

### Start the Entire Stack

```bash
docker compose up --build -d
```

This will build and start all containers: `service-a`, `service-b`, `otel-collector`, `prometheus`, `jaeger`, and `grafana`. Confirm all are Up with `docker ps`.

### Access Dashboards

- **Grafana**: http://localhost:3000 (admin/admin) - Import dashboard ID 19004 (Spring Boot Statistics) or 18675
- **Prometheus**: http://localhost:9090
- **Jaeger**: http://localhost:16686

### Verify Initial State (Circuit CLOSED)

1. In your terminal, hit `service-a`:
   ```bash
   curl http://localhost:8080/service-a/call-service-b
   ```
   You should see: `Service A called Service B: Hello from Service B!`

2. In Grafana, query `resilience4j_circuitbreaker_state{job="service-a", name="serviceB"}`. It should show `0` (CLOSED).

### Induce Failure (Circuit OPEN)

1. Stop `service-b`:
   ```bash
   docker stop self-healing-infra-service-b-1
   ```

2. Rapidly hit `service-a` repeatedly (e.g., 10-15 times quickly):
   ```bash
   for i in {1..15}; do curl http://localhost:8080/service-a/call-service-b; sleep 0.2; done
   ```

3. Observe Terminal: You will consistently receive the fallback message: `Service B is currently unavailable or experiencing issues. Returning a fallback message from Service A.`

4. Observe Grafana:
   - Query `resilience4j_circuitbreaker_state{job="service-a", name="serviceB"}`. You will see it transition from `0` (CLOSED) to `1` (OPEN).
   - Query `resilience4j_circuitbreaker_calls_total{job="service-a", name="serviceB", kind="failed"}`. This counter will increase during the initial failures that cause the circuit to open.
   - Query `resilience4j_circuitbreaker_calls_total{job="service-a", name="serviceB", kind="not_permitted"}`. This counter will rapidly increase once the circuit is OPEN, showing calls being fast-failed.

### Test Recovery (Circuit HALF_OPEN then CLOSED)

1. Restart `service-b`:
   ```bash
   docker start self-healing-infra-service-b-1
   ```

2. Wait for ~5 seconds (`waitDurationInOpenState` for the circuit to transition to HALF_OPEN).

3. Hit `service-a` a few times slowly (e.g., 3-5 times):
   ```bash
   curl http://localhost:8080/service-a/call-service-b
   ```

4. Observe Terminal: You might get the fallback for the first few calls (test calls in HALF_OPEN), then you should start seeing: `Service A called Service B: Hello from Service B!`

5. Observe Grafana:
   - Query `resilience4j_circuitbreaker_state{job="service-a", name="serviceB"}`. You will see it transition from `1` (OPEN) to `2` (HALF_OPEN), and then back to `0` (CLOSED).
   - Query `resilience4j_circuitbreaker_calls_total{job="service-a", name="serviceB", kind="successful"}`. This counter will increase as calls succeed in HALF_OPEN and CLOSED states.

## Future Enhancements & Roadmap

This project lays the foundation for a truly self-healing system. Future work includes:

- **Retry Pattern**: Implement automatic retries for transient failures (e.g., network glitches) to recover without human intervention
- **Time Limiter**: Add timeouts to external calls to prevent indefinite hangs and ensure `service-a` remains responsive
- **Rate Limiter**: Protect downstream services (`service-b`) from being overwhelmed by `service-a` by limiting the rate of requests
- **Bulkhead Pattern**: Isolate calls to `service-b` into separate thread pools to prevent resource exhaustion in `service-a` if `service-b` becomes slow
- **Automated Remediation**: Explore integrating alert-driven actions (e.g., via Grafana alerts or custom scripts) to automatically scale, restart, or reconfigure services based on observed resilience metrics
- **Chaos Engineering**: Introduce controlled failures (e.g., using Chaos Mesh, ToxiProxy) to systematically test and validate the resilience patterns
- **Centralized Configuration**: Integrate Spring Cloud Config or similar for dynamic updates of resilience policies without redeployments

## About the Author

[https://www.github.com/ahammednibras8]Ahammed Nibras - [https://www.linkedin.com/in/ahammednibras8/] Contact on LinkedIn
[Your GitHub Profile (Optional)]

This project demonstrates a deep understanding of building robust, observable, and self-healing distributed systems – a critical skill for any modern tech organization.
