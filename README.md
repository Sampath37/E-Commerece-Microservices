# E-Commerce Microservices Platform

A robust, highly scalable E-Commerce platform built using the **Spring Boot Microservices Architecture**. This project leverages industry-standard patterns and modern technologies to ensure reliability, security, and maintainability.

## 🏗️ Architecture Overview

The platform consists of several loosely coupled microservices communicating asynchronously via Apache Kafka and synchronously via Spring Cloud OpenFeign.

### Core Services
- **User Service**: Acts as the Identity Provider (IdP). Manages user registration, profile updates, and secure JWT-based authentication.
- **Product / Inventory Service**: Manages catalog and stock levels.
- **Order Service**: Handles order creation and lifecycle management.
- **Payment Service**: Processes transactions and manages payment states.
- **Notification & Email Services**: Handles asynchronous communication with users.
- **Analytics Service**: Aggregates platform data.

### Infrastructure Services
- **API Gateway**: (Spring Cloud Gateway) The single entry point for all client requests. It implements Global routing, JWT authentication validation, and Circuit Breaking (Resilience4j).
- **Service Registry**: (Eureka) Enables dynamic service discovery.
- **Config Server**: Centralized configuration management pulling from a Git repository (`ecommerce-config-repo`).

---

## 🚀 Key Features

### 🔐 Secure JWT Authentication
- **Centralized Security**: Authentication is handled by the `user-service` which generates encrypted JSON Web Tokens (JWT) using `BCrypt` hashing.
- **Gateway Validation**: The API Gateway natively intercepts all incoming requests, validates the JWT signature (using a shared secret from the config server), and securely mutates the downstream request to include `X-Auth-User-Id` headers.
- **Protected Endpoints**: Core business logic is protected behind the Gateway, ensuring zero unauthenticated traffic reaches downstream microservices.

### 🌐 Universal AOP Logging & Exception Handling
- **Zero-Boilerplate Logs**: Leveraging Aspect-Oriented Programming (AOP), a central `@Around` aspect automatically intercepts every `@RestController` and `@Service` method across all 10 microservices.
- **Execution Tracking**: Automatically logs method entry, arguments, and total execution time (`StopWatch`).
- **Global Error Handling**: Unhandled exceptions are automatically caught by the aspect, logged at the `ERROR` level, and then cleanly translated into standardized JSON responses via a shared `@RestControllerAdvice` (`GlobalExceptionHandler`).

### 🔄 Event-Driven Saga Pattern
- **Apache Kafka**: Complex distributed transactions (like creating an order, reserving inventory, and processing payment) are handled asynchronously using Kafka topics (e.g., `inventory-reserved`, `payment-success`). This ensures eventual consistency and high availability.
- **OpenFeign**: Synchronous internal communication (e.g., validating a user or product before placing an order) is handled cleanly via Spring Cloud OpenFeign clients.

---

## 🛠️ Technology Stack
- **Backend Core**: Java 17, Spring Boot 3.x, Spring Cloud
- **Security**: JJWT (JSON Web Token), Spring Security Crypto
- **Messaging & Communication**: Apache Kafka, Spring Cloud OpenFeign
- **Resilience**: Resilience4j (Circuit Breakers & Time Limiters)
- **Database**: Spring Data JPA / Hibernate
- **Build Tool**: Maven

## 🚦 Getting Started

### Prerequisites
- JDK 17
- Maven
- Apache Kafka (Running on `localhost:9092`)
- Git

### Running the Ecosystem
1. **Start Infrastructure Services**: 
   - Start `discovery-server` (Eureka).
   - Start `config-server`. Ensure your `ecommerce-config-repo` is accessible.
   - Start `api-gateway`.
2. **Start Core Services**:
   - Start `user-service`, `order-service`, `inventory-service`, etc.
3. **Update IDE Classpath**: If you are using Eclipse or STS, ensure you run **Maven -> Update Project (Alt+F5)** on all modules (especially `common-exception` and `api-gateway`) so the IDE syncs the AOP and JJWT dependencies.

## 📝 API Endpoints (User Service)
All requests should be routed through the API Gateway (port `8080`).

- `POST /api/users` - Create a new user (Open)
- `POST /api/users/login` - Authenticate and receive JWT (Open)
- `POST /api/users/change-password` - Change password (Secured)
- `DELETE /api/users/{userId}` - Delete user profile (Secured)

## 📁 Version Control
A comprehensive `.gitignore` is included at the project root to ensure compiled classes, logs, IDE configurations, and `target/` build directories are kept out of the version history, maintaining a clean repository.