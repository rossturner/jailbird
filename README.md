# Jailbird - AI-Powered Virtual YouTuber (Java Implementation)

Jailbird is an AI-powered virtual YouTuber (VTuber) built on a skills-based microservice architecture using Java and Spring Boot. This is the Java implementation of the Python-based Lyrebird project.

## Quick Start

### Prerequisites
- Java 21+
- Docker and Docker Compose
- Gradle 8.5+ (or use the wrapper)

### Running the Text Generation Service

1. **Start the infrastructure services:**
```bash
docker-compose up -d redis rabbitmq
```

2. **Run the text-generation service:**
```bash
cd skills/text-generation
./gradlew bootRun
```

Or using Docker:
```bash
docker-compose up text-generation
```

### Testing the Service

#### Health Check (Spring Boot Actuator)
```bash
curl http://localhost:50008/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

#### Text Generation (HTTP)
```bash
curl -X POST http://localhost:50008/generate_text \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Hello, how are you?",
    "persona": "default",
    "conversationId": "test-123",
    "userId": "user-456"
  }'
```

Expected response:
```json
{
  "generatedText": "Hello! I'm default, nice to meet you! How can I help you today?",
  "success": true,
  "tokenCount": 13
}
```

#### Spring Boot Actuator Health Check
```bash
curl http://localhost:50008/actuator/health
```

#### gRPC Testing

Using `grpcurl` (if installed):
```bash
# Health check
grpcurl -plaintext localhost:9090 jailbird.common.HealthService/Check

# Text generation
grpcurl -plaintext -d '{"prompt": "Hello world"}' \
  localhost:9090 jailbird.skills.textgeneration.TextGenerationService/GenerateText
```

## Architecture

### Current Implementation

The text-generation skill demonstrates the complete jailbird architecture:

- **Spring Boot 3.2.1** with Java 21
- **gRPC services** for inter-service communication
- **REST APIs** for external access
- **Health check endpoints** for monitoring
- **Mock LLM integration** (ready for real LLM)
- **Persona-aware responses**
- **Docker containerization**

### Project Structure

```
jailbird/
├── proto/                          # Protobuf definitions
│   ├── common/                     # Shared proto files
│   │   ├── health.proto
│   │   └── types.proto
│   └── skills/
│       └── text_generation.proto
├── libs/                           # Shared libraries
│   ├── common/                     # Common utilities
│   └── proto-generated/            # Generated gRPC code
├── skills/
│   └── text-generation/            # Only implemented skill
│       ├── src/main/java/com/jailbird/textgeneration/
│       │   ├── TextGenerationApplication.java
│       │   ├── service/
│       │   │   ├── TextGenerationGrpcService.java
│       │   │   └── TextGenerationBusinessLogic.java
│       │   └── controller/
│       │       └── TextGenerationController.java
│       ├── src/main/resources/
│       │   └── application.yml
│       └── src/test/java/
├── docker-compose.yml
├── build.gradle.kts
└── settings.gradle.kts
```

## Services and Ports

| Service | HTTP Port | gRPC Port | Status |
|---------|-----------|-----------|---------|
| text-generation | 50008 | 50005 | ✅ Implemented |

### Other Skills (Planned)
Additional skills like speech, conversation, memory, orchestrator, avatar, twitch, and admin are planned for future implementation. Currently only text-generation is fully implemented.

### Infrastructure Services

| Service | Port | Purpose |
|---------|------|---------|
| Redis | 6379 | Memory store and caching |
| RabbitMQ | 5672 | Async messaging |
| RabbitMQ Management | 15672 | Management UI |

## Features Implemented

### ✅ Text Generation Service
- **gRPC API** with health checks and streaming support
- **REST API** with JSON request/response
- **Mock LLM integration** (easily replaceable with real LLM)
- **Simple persona system** with NicolePersona as default
- **Health monitoring** with Spring Boot Actuator
- **Unit tests** with Spring Boot Test

### ✅ Infrastructure
- **Docker Compose** setup with Redis and RabbitMQ
- **Gradle multi-project** build with Kotlin DSL
- **Protobuf code generation** for type-safe gRPC
- **Hot reload** development with Spring Boot DevTools

### 🔄 Next Steps
- Replace mock text generation with real LLM integration
- Implement additional skills (speech, conversation, memory, orchestrator, avatar, twitch, admin)
- Add RabbitMQ messaging between services
- Implement avatar integration with VTube Studio
- Add Twitch chat integration

## Development

### Adding a New Skill

1. **Add to settings.gradle.kts:**
```kotlin
include("skills:new-skill")
```

2. **Create skill structure:**
```bash
mkdir -p skills/new-skill/src/main/java/com/jailbird/newskill
mkdir -p skills/new-skill/src/main/resources
mkdir -p skills/new-skill/src/test/java
```

3. **Add proto definition:**
```bash
# Create proto/skills/new_skill.proto
# Run ./gradlew :libs:proto-generated:generateProto
```

4. **Implement Spring Boot service** following the text-generation pattern

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific skill tests
./gradlew :skills:text-generation:test
```

### Building
```bash
# Build all projects
./gradlew build

# Build specific skill
./gradlew :skills:text-generation:build
```

## Integration with LLM

The current implementation includes a mock text generation service. To integrate with a real LLM:

1. **Update TextGenerationBusinessLogic.java**
2. **Add LLM client dependencies** to build.gradle.kts
3. **Configure LLM endpoint** in application.yml
4. **Replace mock responses** with actual LLM calls

Example integration points are ready for:
- OpenAI API
- Local LLM servers (Ollama, text-generation-webui)
- Google Gemma
- Other LLM APIs

## Monitoring and Operations

### Health Checks
- **Actuator Health**: `/actuator/health`
- **gRPC Health**: `grpcurl localhost:9090 jailbird.common.HealthService/Check`

### Metrics
- **Actuator Metrics**: `/actuator/metrics`
- **Custom Metrics**: Token counts, generation times, error rates

### Logs
- **Application Logs**: Structured JSON logging
- **Access Logs**: HTTP and gRPC request logs
- **Error Tracking**: Exception handling and reporting

---

This completes the initial jailbird implementation with a working text-generation service that demonstrates the full architecture pattern for building additional skills.