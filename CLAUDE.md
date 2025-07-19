# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Jailbird** is an AI-powered virtual YouTuber system built with Java 21 and Spring Boot 3.2.1, using a skills-based microservice architecture. Each "skill" is an independent Spring Boot service that handles specific capabilities (text generation, speech, memory, avatar control, etc.).

**Current Status**: Only the `text-generation` skill is fully implemented and serves as the reference pattern for all other skills.

## Essential Commands

### Development Workflow
```bash
# Start infrastructure services (Redis, RabbitMQ)
./gradlew startDev
# Alternative: docker-compose up -d

# Generate protobuf code (run after proto changes)
./gradlew genProto

# Run the text-generation service with hot reload
cd skills/text-generation && ./gradlew bootRun

# Build everything
./gradlew build

# Run all tests
./gradlew test

# Run specific skill tests
./gradlew :skills:text-generation:test
```

### Testing Endpoints
```bash
# Health check (Spring Boot Actuator)
curl http://localhost:8080/actuator/health

# Text generation via REST
curl -X POST http://localhost:8080/api/v1/text-generation/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Hello", "context": {"userId": "test"}}'

# gRPC health check
grpcurl -plaintext localhost:9090 jailbird.common.HealthService/Check
```

## Architecture Overview

### Multi-Module Structure
- **`libs/common/`** - Shared Spring Boot utilities, configuration, and persona management
- **`libs/proto-generated/`** - Generated gRPC code from protobuf definitions
- **`skills/`** - Independent Spring Boot microservices
- **`proto/`** - Protobuf definitions (source of truth for APIs)

### Proto-First Development
1. Define APIs in `proto/skills/[skill_name].proto`
2. Run `./gradlew genProto` to generate Java classes and gRPC stubs
3. Implement Spring Boot service using generated interfaces
4. Both gRPC and REST endpoints are automatically available

### Service Pattern (Follow text-generation)
Each skill follows this Spring Boot structure:
- **Application class** with `@SpringBootApplication`
- **gRPC service** with `@GrpcService` annotation
- **Business logic service** with `@Service`
- **REST controller** (auto-generated from protobuf)
- **Configuration** classes for OpenAPI/Swagger
- **Health checks** via Spring Boot Actuator
- **Unit tests** with Spring Boot Test

### Key Configuration Files
- **`settings.gradle.kts`** - Defines all modules (add new skills here)
- **`build.gradle.kts`** - Root build configuration with shared dependencies
- **`docker-compose.yml`** - Infrastructure services (Redis port 6379, RabbitMQ ports 5672/15672)
- **`skills/*/application.yml`** - Per-service configuration

## Implementation Status

### ✅ Fully Implemented
- **text-generation skill**: Complete Spring Boot service with mock LLM integration
- **gRPC communication**: Health checks and text generation
- **REST API**: Auto-generated with OpenAPI documentation
- **Docker setup**: Containerization with health checks
- **Build system**: Multi-module Gradle with hot reload
- **Persona system**: Configuration-driven personality responses

### 🚧 Planned/Placeholder
- **Other skills**: admin, avatar, conversation, memory, moderation, orchestrator, speech, twitch (only build directories exist)
- **Real LLM integration**: Currently returns mock responses
- **RabbitMQ messaging**: Infrastructure ready but not used
- **Memory/RAG system**: Detailed design documents exist

## Adding New Skills

1. **Add to settings.gradle.kts**: `include("skills:new-skill")`
2. **Create protobuf definition**: `proto/skills/new_skill.proto`
3. **Generate code**: `./gradlew genProto`
4. **Copy text-generation structure**: Follow the established pattern
5. **Implement business logic**: Replace mock implementations
6. **Add to Docker Compose**: For service orchestration

## Key Dependencies & Versions
- **Java 21** (minimum requirement)
- **Spring Boot 3.2.1** with WebFlux and Actuator
- **gRPC Spring Boot Starter 2.15.0**
- **Redis and RabbitMQ** for infrastructure
- **Gradle 8.5+** with Kotlin DSL

## Development Notes

- **Hot reload**: Spring Boot DevTools enabled in development mode
- **Port standardization**: HTTP (8080), gRPC (9090) for all skills
- **Persona configuration**: Uses `JailbirdProperties` class from common library
- **Health checks**: All services expose `/actuator/health` and gRPC health service
- **Testing**: TestContainers ready for integration tests with Redis/RabbitMQ

The text-generation skill serves as the complete reference implementation for the architecture - examine it thoroughly when implementing new skills.