# Jailbird — AI-Powered Virtual YouTuber (Java Implementation)

## Unified Architecture & Implementation Plan

---

## 1. Project Overview

**Jailbird** is an AI-powered virtual YouTuber (VTuber) built on a skills-based microservice architecture using Java and Spring Boot. The system combines specialized AI agents with real-time streaming capabilities to create an engaging, interactive virtual personality.

### Core Goals
- **Modularity**: Independent "skills" as Spring Boot microservices for different capabilities
- **Efficiency**: Async communication with gRPC and RabbitMQ for local deployment
- **Intelligence**: Advanced memory system with RAG for context awareness
- **Interactivity**: Real-time Twitch integration with live avatar responses
- **Extensibility**: Easy addition of new skills and capabilities
- **Hot Reload**: Fast development iteration with Spring Boot DevTools

## 2. Skills-Based Microservice Architecture

### What is a "Skill"?
A **skill** is an independent Spring Boot microservice representing a specific capability the AI VTuber can perform. Each skill operates autonomously while communicating with other skills through well-defined gRPC interfaces.

### Core Skills
- **text-generation** - LLM integration for text generation and conversation
- **speech** - Text-to-speech generation and audio processing
- **conversation** - Dialogue management and engagement strategies
- **memory** - Context storage, retrieval, and relationship management
- **moderation** - Content filtering and safety controls
- **orchestrator** - Task routing and coordination between skills
- **avatar** - Visual representation and animation control
- **twitch** - Real-time chat integration, OAuth authentication, and stream events
- **admin** - Monitoring, control, and debugging interface

### Communication Patterns
- **gRPC**: Fast synchronous calls between skills for immediate responses
- **RabbitMQ**: Asynchronous pub/sub for longer operations and event broadcasting
- **HTTP REST**: Automatically generated Spring Boot REST endpoints alongside gRPC

### Service Architecture
Each skill follows a consistent Spring Boot pattern:
```
skill-name/
├── src/main/java/com/jailbird/skillname/
│   ├── SkillNameApplication.java          # Spring Boot main class
│   ├── service/
│   │   ├── SkillNameService.java          # gRPC service implementation
│   │   └── SkillNameBusinessLogic.java    # Core business logic
│   ├── controller/
│   │   └── SkillNameController.java       # REST API controller
│   ├── config/
│   │   ├── GrpcConfig.java                # gRPC configuration
│   │   └── RabbitMQConfig.java            # RabbitMQ configuration
│   └── messaging/
│       └── SkillNameMessageHandler.java   # RabbitMQ integration
├── src/main/resources/
│   ├── application.yml                    # Spring Boot configuration
│   └── logback-spring.xml                 # Logging configuration
├── src/test/java/                         # Comprehensive test suite
│   ├── integration/                       # Integration tests
│   └── unit/                             # Unit tests
├── build.gradle.kts                       # Gradle build configuration
└── Dockerfile                             # Containerization
```

## 3. Project Structure

### Monorepo Layout
```
jailbird/
├── proto/                          # Centralized proto definitions
│   ├── common/
│   │   ├── events.proto            # Cross-skill event messages
│   │   ├── types.proto             # Shared data types
│   │   └── health.proto            # Health check interface
│   └── skills/
│       ├── text_generation.proto
│       ├── speech.proto
│       ├── conversation.proto
│       ├── memory.proto
│       ├── orchestrator.proto
│       ├── moderation.proto
│       ├── avatar.proto
│       ├── twitch.proto
│       └── admin.proto
├── libs/                           # Shared libraries
│   ├── common/                     # Common utilities
│   │   ├── src/main/java/com/jailbird/common/
│   │   │   ├── logging/            # Structured logging
│   │   │   ├── config/             # Configuration management
│   │   │   ├── persona/            # Persona configuration
│   │   │   └── messaging/          # RabbitMQ utilities
│   │   ├── build.gradle.kts
│   │   └── src/test/
│   └── proto-generated/            # Generated gRPC code
│       ├── src/main/java/
│       └── build.gradle.kts
├── personas/                       # Persona configuration files
│   ├── default.yaml
│   └── [additional personas]
├── skills/                         # All skills (microservices)
│   ├── text-generation/            # Starting skill
│   ├── speech/
│   ├── conversation/
│   ├── memory/
│   ├── moderation/
│   ├── orchestrator/
│   ├── avatar/
│   ├── twitch/
│   └── admin/
├── docker-compose.yml              # Production orchestration
├── docker-compose.override.yml     # Development with hot-reload
├── build.gradle.kts                # Root Gradle build
├── settings.gradle.kts             # Gradle multi-project settings
├── gradle.properties               # Gradle properties
└── README.md
```

### Build System & Task Management

**Jailbird** uses Gradle with Kotlin DSL for build management and task orchestration:

**Root-Level Configuration** (build.gradle.kts):
```kotlin
plugins {
    id("java-library")
    id("com.google.protobuf") version "0.9.4"
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.jailbird"
    version = "0.1.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.9")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    }
}

tasks.register("startDev") {
    group = "application"
    description = "Start all services in development mode"
    doLast {
        exec {
            commandLine("docker-compose", "up", "-d")
        }
    }
}

tasks.register("genProto") {
    group = "build"
    description = "Generate protobuf code"
    dependsOn(":libs:proto-generated:generateProto")
}
```

**Per-Skill Configuration** (skills/*/build.gradle.kts):
```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:proto-generated"))
    
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("net.devh:grpc-spring-boot-starter:2.15.0.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:rabbitmq:1.19.3")
    testImplementation("org.testcontainers:redis:1.19.3")
}

tasks.register("dev") {
    group = "application"
    description = "Run skill in development mode with hot reload"
    dependsOn("bootRun")
}
```

---

## 4. Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Language** | Java 17+ | Modern Java with strong typing and performance |
| **Framework** | Spring Boot 3.2+ | Microservice framework with auto-configuration |
| **gRPC Framework** | grpc-java + grpc-spring-boot-starter | Java gRPC with Spring Boot integration |
| **HTTP Framework** | Spring Web MVC | REST endpoints alongside gRPC services |
| **Message Queue** | RabbitMQ + Spring AMQP | Async pub/sub for longer operations |
| **LLM Runtime** | Google Gemma (12B/27B) | Local inference with quantization (24GB VRAM) |
| **TTS Engine** | F5-TTS | High-quality voice synthesis via CLI integration |
| **Avatar System** | Live2D + VTube Studio + Java WebSocket client | Real-time 2D avatar control |
| **Twitch Integration** | Twitch4J | OAuth2 authentication and real-time chat events |
| **Memory Store** | Redis with Jedis/Lettuce | Fast vector/text hybrid search |
| **Frontend** | React + Tailwind | Admin dashboard and monitoring |
| **Build System** | Gradle with Kotlin DSL | Multi-project build management |
| **Containerization** | Docker + Docker Compose | Local development environment |
| **Testing** | JUnit 5 + TestContainers | Comprehensive testing framework |

---

## 5. Memory & RAG System

Jailbird implements a sophisticated memory system inspired by the AIRI project, featuring hierarchical memory layers, contextual retrieval, and temporal weighting for human-like memory patterns.

**Key Features:**
- **Hierarchical Architecture**: Working, short-term, and long-term memory layers
- **Hybrid Search**: Dense + sparse vector search with Redis
- **Temporal Weighting**: Importance decay and access frequency scoring
- **Context Windows**: Conversation continuity and narrative coherence
- **Emotional Impact**: Memory significance scoring (-10 to +10 scale)

**Implementation Phases:**
1. **Foundation**: Basic hybrid search with Redis
2. **Enhanced**: Full temporal weighting and memory layers  
3. **Advanced**: Emotional integration and relationship tracking

For detailed technical specifications, implementation details, and AIRI-inspired architecture patterns, see **[Memory System Design](memory-system-design.md)**.

### Avatar Integration

Jailbird integrates with Live2D avatars through VTube Studio's WebSocket API for real-time avatar control and animation. The avatar skill provides programmatic control over facial expressions, parameter updates, and speech-synchronized mouth movement using Java WebSocket clients.

For detailed technical specifications and implementation details, see **[Live2D and VTube Studio Integration](live2d-vtube-studio-integration.md)**.

---

## 6. Development Workflow

### Proto-First Development with Spring Boot
1. **Define API Contract**: Write `.proto` files in centralized `/proto` directory
2. **Generate Code**: Use Gradle protobuf plugin to generate Java classes and gRPC stubs
3. **Implement Service**: Use grpc-spring-boot-starter for both gRPC and REST endpoints
4. **Test Interfaces**: Use TestContainers for comprehensive integration testing
5. **Deploy Services**: Docker containers with Spring Boot patterns

### Centralized Code Generation
All proto files are processed together for consistency:
```kotlin
// In libs/proto-generated/build.gradle.kts
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.59.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}
```

### Spring Boot gRPC Service Example
```java
@GrpcService
public class TextGenerationService extends TextGenerationServiceGrpc.TextGenerationServiceImplBase {
    
    @Override
    public void generateText(TextGenerationRequest request,
                           StreamObserver<TextGenerationResponse> responseObserver) {
        try {
            String generatedText = businessLogic.generateText(request.getPrompt());
            
            TextGenerationResponse response = TextGenerationResponse.newBuilder()
                .setGeneratedText(generatedText)
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}

// Automatically available as REST endpoint at /api/v1/text-generation
@RestController
@RequestMapping("/api/v1/text-generation")
public class TextGenerationController {
    
    @Autowired
    private TextGenerationBusinessLogic businessLogic;
    
    @PostMapping("/generate")
    public ResponseEntity<TextGenerationResponse> generateText(
            @RequestBody TextGenerationRequest request) {
        String result = businessLogic.generateText(request.getPrompt());
        return ResponseEntity.ok(new TextGenerationResponse(result));
    }
}
```

---

## 7. Development Workflow & Hot Reload

### Gradle Task Management
Streamlined task management with Gradle:

**Project-Level Commands**:
```bash
# Main orchestration
./gradlew startDev           # Start all services with Docker Compose
./gradlew stopDev            # Stop all services
./gradlew restartDev         # Restart all services
./gradlew logs               # View logs

# Development
./gradlew genProto           # Generate protobuf files
./gradlew test               # Run tests across all skills
./gradlew build              # Build all projects
```

**Per-Skill Development**:
```bash
# In any skill directory
./gradlew bootRun            # Start skill with Spring Boot
./gradlew dev                # Start with hot reload (Spring Boot DevTools)
./gradlew test               # Run skill tests
./gradlew check              # Run tests and code quality checks
```

### Spring Boot DevTools Hot Reload
```yaml
# application-dev.yml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
  
# Automatic restart when:
# - Java classes change
# - Resources change  
# - Dependencies change
```

### Docker Compose Development
```yaml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  redis:
    image: redis/redis-stack:latest
    ports:
      - "6379:6379"
      - "8001:8001"

  # Skills with hot-reload
  skill-text-generation:
    build: ./skills/text-generation
    depends_on: [rabbitmq, redis]
    volumes:
      - ./skills/text-generation/src:/app/src
      - ./skills/text-generation/build:/app/build
    command: ./gradlew bootRun --continuous
    ports:
      - "9090:9090"  # gRPC
      - "8080:8080"  # HTTP
    environment:
      - JAILBIRD_PERSONA=${JAILBIRD_PERSONA:-default}
      - SPRING_PROFILES_ACTIVE=dev
```

---

## 8. Implementation Roadmap

### Phase 1: Foundation (MVP)
**Goal**: Basic working system with core text-generation skill

**Skills to Implement**:
- `text-generation`: Basic LLM integration with health check endpoints
- `orchestrator`: Basic task routing with persona context
- `admin`: Simple monitoring interface

**Key Features**:
- Proto-first development workflow
- Docker Compose local development
- Basic gRPC communication with REST endpoints
- Spring Boot hot reload development
- Single persona support with startup configuration
- Health check endpoints for all services

### Phase 2: Enhanced Features
**Goal**: Complete skill ecosystem

**Additional Skills**:
- `conversation`: Dialogue management
- `memory`: Redis-based storage with persona namespacing
- `speech`: F5-TTS integration
- `avatar`: Live2D control via VTube Studio
- `twitch`: Real-time chat integration

### Phase 3: Advanced Intelligence
**Goal**: Full memory system and advanced features

**Enhancements**:
- AIRI-inspired RAG memory system
- Advanced conversation context
- RabbitMQ async messaging
- Comprehensive testing with TestContainers

---

## 9. Key Design Principles

### Spring Boot Best Practices
- Configuration externalization via application.yml
- Auto-configuration for gRPC and messaging
- Actuator endpoints for health checks and metrics
- Profile-based configuration (dev, prod)

### Hot Reload Development
- Spring Boot DevTools for automatic restart
- Gradle continuous build for immediate feedback
- Docker volume mounts for development

### Testability
- TestContainers for integration testing
- Mock-friendly Spring architecture
- Separate unit and integration test suites

### Local Development Focus
- **Local-only deployment**: System runs entirely on local machine
- **Trusted environment**: No security concerns for local access
- **Performance optimization**: When needed, not premature
- **Simple configuration**: Minimal configuration systems

---

This unified plan provides a comprehensive roadmap for building Jailbird as a sophisticated, maintainable Java-based AI-powered virtual YouTuber system for local deployment.