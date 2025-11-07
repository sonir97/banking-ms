Built a simple Banking Microservices System using Spring Boot and Spring Cloud to learn core microservice concepts.
The application includes separate services for Accounts, Cards, and Loans, each exposing REST APIs and managing its own data.

Integrated a Spring Cloud Config Server for centralized configuration and Eureka Server for service discovery and registry.
A Gateway Server handles routing and security, integrated with Keycloak for authentication and authorization.

Added basic rate limiting, request filters, and fallback methods at the gateway level.
Used RabbitMQ as a message broker to enable communication between services.

This project helped in understanding service discovery, configuration management, gateway routing, security, and inter-service messaging in a microservices architecture.
