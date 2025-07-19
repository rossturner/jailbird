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
curl http://localhost:50008/actuator/health

# Text generation via REST (simplified paths)
curl -X POST http://localhost:50008/generate_text \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Hello", "context": {"userId": "test"}}'

# Service health check via REST
curl http://localhost:50008/health

# Get available models
curl http://localhost:50008/get_models

# gRPC health check
grpcurl -plaintext localhost:50005 grpc.health.v1.Health/Check
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
- **Persona system**: Simple interface-based persona system (NicolePersona default)

### 🚧 Planned/Placeholder
- **Other skills**: admin, avatar, conversation, memory, moderation, orchestrator, speech, twitch (not yet implemented - only text-generation exists)
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
- **Port allocation**: Uses 50000-59999 range with consistent endings (HTTP ends in 8, gRPC ends in 5)
- **Persona system**: Uses `Persona` interface from common library (defaults to NicolePersona)
- **Health checks**: All services expose `/actuator/health` and gRPC health service
- **Testing**: TestContainers ready for integration tests with Redis/RabbitMQ
- **Dependency Injection**: Use constructor injection instead of field injection for better testability and immutability

### Port Allocation Scheme

| Skill | HTTP Port | gRPC Port | Purpose |
|-------|-----------|-----------|---------|
| **text-generation** | 50008 | 50005 | Text/content generation |
| **speech** | 50018 | 50015 | Text-to-speech/speech-to-text |
| **memory** | 50028 | 50025 | RAG and conversation memory |
| **avatar** | 50038 | 50035 | Avatar control and animation |
| **conversation** | 50048 | 50045 | Conversation flow management |
| **moderation** | 50058 | 50055 | Content moderation and safety |
| **twitch** | 50068 | 50065 | Twitch integration |
| **orchestrator** | 50078 | 50075 | Main orchestrator service |
| **admin** | 50088 | 50085 | Admin dashboard and monitoring |

**Port Pattern**: All HTTP ports end in **8**, all gRPC ports end in **5** (10-port increments)

## Simple Persona System

The persona system has been simplified to a clean interface-based approach:

### Using Personas in Services
```java
@Service  
public class YourService {
    private final Persona persona;  // Constructor injection (preferred)
    
    public YourService(Persona persona) {
        this.persona = persona;  // Spring injects NicolePersona by default
    }
    
    public String getGreeting() {
        return persona.getGreeting();  // "Hello! I'm Nicole, nice to meet you!"
    }
}
```

### Current Implementation
- **NicolePersona**: Default persona with name "Nicole"
- **Interface**: `com.jailbird.common.persona.Persona`
- **No configuration needed**: Automatically available to all skills

### Adding New Personas (Future)
To add new personas, simply implement the `Persona` interface and add Spring conditional annotations:
```java
@Component
@ConditionalOnProperty(name = "jailbird.persona", havingValue = "cheerful")
public class CheerfulPersona implements Persona { ... }
```

The text-generation skill serves as the complete reference implementation for the architecture - examine it thoroughly when implementing new skills.