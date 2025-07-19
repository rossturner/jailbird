# Live2D and VTube Studio Integration (Java Implementation)

## Overview

This document outlines the technical approach for integrating Live2D avatar control with VTube Studio for the Jailbird AI VTuber system using Java and Spring Boot. VTube Studio provides a comprehensive WebSocket API that allows programmatic control of Live2D models and parameters.

## VTube Studio API Architecture

### Core Communication
- **Protocol**: WebSocket API 
- **Default Endpoint**: `ws://localhost:8001`
- **Authentication**: Plugin-based authentication system
- **Data Format**: JSON messages with structured request/response patterns

### Key Capabilities
1. **Parameter Control**: Create and modify custom tracking parameters
2. **Hotkey Triggers**: Programmatically trigger model expressions and animations
3. **Model Management**: Load, switch, and manage Live2D models
4. **Live2D Items**: Control additional Live2D elements (accessories, effects)
5. **Expression Control**: Trigger facial expressions and body movements
6. **Real-time Animation**: Send continuous parameter updates for smooth animation

## Java Integration Approach

### Primary WebSocket Client: Java-WebSocket
- **Library**: `org.java-websocket:Java-WebSocket`
- **Spring Integration**: Custom Spring component with WebSocket client
- **Features**: Full async WebSocket support with Spring Boot integration
- **Installation**: Add to `build.gradle.kts`

### Alternative Options
- **Spring WebSocket**: Native Spring WebSocket client
- **OkHttp WebSocket**: OkHttp's WebSocket implementation
- **Tyrus**: Reference implementation of Java API for WebSocket

## Implementation Strategy for Jailbird

### Avatar Skill Spring Boot Architecture
```java
// avatar/src/main/java/com/jailbird/avatar/service/AvatarService.java
@Service
@Slf4j
public class AvatarService {
    
    private final VTubeStudioClient vtsClient;
    private final ObjectMapper objectMapper;
    private volatile boolean connected = false;
    
    @Autowired
    public AvatarService(VTubeStudioClient vtsClient, ObjectMapper objectMapper) {
        this.vtsClient = vtsClient;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void initialize() {
        connectToVTubeStudio();
    }
    
    @Async
    public CompletableFuture<Void> connectToVTubeStudio() {
        return CompletableFuture.runAsync(() -> {
            try {
                vtsClient.connect();
                authenticatePlugin();
                connected = true;
                log.info("Successfully connected to VTube Studio");
            } catch (Exception e) {
                log.error("Failed to connect to VTube Studio", e);
            }
        });
    }
    
    public CompletableFuture<Void> triggerExpression(String expressionName) {
        if (!connected) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                HotkeyTriggerRequest request = new HotkeyTriggerRequest(expressionName);
                vtsClient.sendRequest(request);
            } catch (Exception e) {
                log.error("Failed to trigger expression: {}", expressionName, e);
            }
        });
    }
    
    public CompletableFuture<Void> updateParameters(Map<String, Float> parameters) {
        if (!connected) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            parameters.forEach((paramName, value) -> {
                try {
                    ParameterUpdateRequest request = new ParameterUpdateRequest(paramName, value);
                    vtsClient.sendRequest(request);
                } catch (Exception e) {
                    log.error("Failed to update parameter {} to {}", paramName, value, e);
                }
            });
        });
    }
}
```

### WebSocket Client Implementation
```java
// avatar/src/main/java/com/jailbird/avatar/client/VTubeStudioClient.java
@Component
@Slf4j
public class VTubeStudioClient {
    
    private static final URI VTS_ENDPOINT = URI.create("ws://localhost:8001");
    private WebSocketClient client;
    private Session session;
    private final ObjectMapper objectMapper;
    private final Queue<CompletableFuture<JsonNode>> pendingRequests = new ConcurrentLinkedQueue<>();
    
    @Autowired
    public VTubeStudioClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = ContainerProvider.getWebSocketContainer().connectToServer(
            new VTSEndpoint(), VTS_ENDPOINT);
    }
    
    public void connect() throws Exception {
        if (session == null || !session.isOpen()) {
            session = client.connectToServer(new VTSEndpoint(), VTS_ENDPOINT);
            log.info("Connected to VTube Studio WebSocket");
        }
    }
    
    public CompletableFuture<JsonNode> sendRequest(VTSRequest request) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.offer(future);
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(request);
            session.getBasicRemote().sendText(jsonMessage);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    @ClientEndpoint
    public class VTSEndpoint {
        
        @OnMessage
        public void onMessage(String message) {
            try {
                JsonNode response = objectMapper.readTree(message);
                CompletableFuture<JsonNode> future = pendingRequests.poll();
                if (future != null) {
                    future.complete(response);
                }
            } catch (Exception e) {
                log.error("Error processing VTS response", e);
            }
        }
        
        @OnError
        public void onError(Session session, Throwable throwable) {
            log.error("VTube Studio WebSocket error", throwable);
            connected = false;
        }
        
        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            log.info("VTube Studio WebSocket closed: {}", closeReason.getReasonPhrase());
            connected = false;
        }
    }
}
```

### Live2D Parameter Mapping

#### Standard Parameters
```java
public enum Live2DParameter {
    FACE_ANGLE_X("FaceAngleX"),
    FACE_ANGLE_Y("FaceAngleY"), 
    FACE_ANGLE_Z("FaceAngleZ"),
    EYE_OPEN_LEFT("EyeOpenLeft"),
    EYE_OPEN_RIGHT("EyeOpenRight"),
    MOUTH_FORM("MouthForm"),
    BODY_ANGLE_X("BodyAngleX"),
    BODY_ANGLE_Y("BodyAngleY"),
    BODY_ANGLE_Z("BodyAngleZ");
    
    private final String parameterName;
    
    Live2DParameter(String parameterName) {
        this.parameterName = parameterName;
    }
    
    public String getParameterName() {
        return parameterName;
    }
}
```

#### Custom Parameters for Jailbird
```java
public enum JailbirdParameter {
    EMOTION_INTENSITY("EmotionIntensity"),
    SPEECH_ACTIVITY("SpeechActivity"), 
    ATTENTION_LEVEL("AttentionLevel"),
    RESPONSE_TYPE("ResponseType");
    
    private final String parameterName;
    
    JailbirdParameter(String parameterName) {
        this.parameterName = parameterName;
    }
}
```

### Expression Control System

#### Hotkey-Based Expressions
```java
@Component
public class ExpressionController {
    
    private final AvatarService avatarService;
    private static final Map<String, String> EXPRESSION_HOTKEYS = Map.of(
        "happy", "Happy_Expression",
        "sad", "Sad_Expression", 
        "excited", "Excited_Expression",
        "thinking", "Thinking_Expression",
        "confused", "Confused_Expression"
    );
    
    @Autowired
    public ExpressionController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }
    
    public CompletableFuture<Void> expressEmotion(String emotion, float intensity) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        
        if (EXPRESSION_HOTKEYS.containsKey(emotion)) {
            tasks.add(avatarService.triggerExpression(EXPRESSION_HOTKEYS.get(emotion)));
        }
        
        // Fine-tune with custom parameters
        Map<String, Float> parameters = Map.of(
            JailbirdParameter.EMOTION_INTENSITY.getParameterName(), intensity,
            Live2DParameter.FACE_ANGLE_Y.getParameterName(), calculateHeadTilt(emotion)
        );
        
        tasks.add(avatarService.updateParameters(parameters));
        
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }
    
    private float calculateHeadTilt(String emotion) {
        return switch (emotion) {
            case "happy" -> 5.0f;
            case "sad" -> -10.0f;
            case "thinking" -> 15.0f;
            default -> 0.0f;
        };
    }
}
```

#### Speech-Synchronized Animation
```java
@Service
public class SpeechAnimationService {
    
    private final AvatarService avatarService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    @Autowired
    public SpeechAnimationService(AvatarService avatarService) {
        this.avatarService = avatarService;
    }
    
    public CompletableFuture<Void> animateSpeech(String text, List<Phoneme> phonemes) {
        return CompletableFuture.runAsync(() -> {
            for (Phoneme phoneme : phonemes) {
                float mouthShape = phonemeToMouthShape(phoneme);
                
                Map<String, Float> parameters = Map.of(
                    Live2DParameter.MOUTH_FORM.getParameterName(), mouthShape,
                    JailbirdParameter.SPEECH_ACTIVITY.getParameterName(), 1.0f
                );
                
                avatarService.updateParameters(parameters);
                
                try {
                    Thread.sleep((long) (phoneme.getDuration() * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Return to neutral
            Map<String, Float> neutralParams = Map.of(
                Live2DParameter.MOUTH_FORM.getParameterName(), 0.0f,
                JailbirdParameter.SPEECH_ACTIVITY.getParameterName(), 0.0f
            );
            
            avatarService.updateParameters(neutralParams);
        });
    }
    
    private float phonemeToMouthShape(Phoneme phoneme) {
        return switch (phoneme.getType()) {
            case "A", "O" -> 0.8f;
            case "E", "I" -> 0.5f;
            case "U" -> 0.3f;
            case "M", "B", "P" -> 0.0f;
            default -> 0.2f;
        };
    }
}
```

## Spring Boot Integration

### Configuration
```java
@Configuration
@EnableAsync
public class AvatarConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
    
    @Bean
    public TaskExecutor avatarTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("avatar-");
        executor.initialize();
        return executor;
    }
}
```

### Application Properties
```yaml
# application.yml
jailbird:
  avatar:
    vtube-studio:
      endpoint: "ws://localhost:8001"
      plugin-name: "Jailbird"
      plugin-developer: "Jailbird Team"
      connection-timeout: 5000
      reconnect-attempts: 3
      reconnect-delay: 2000
    
    expressions:
      happy: "Happy_Expression"
      sad: "Sad_Expression"
      excited: "Excited_Expression"
      thinking: "Thinking_Expression"
      confused: "Confused_Expression"
    
    parameters:
      emotion-intensity-max: 1.0
      speech-activity-threshold: 0.8
      head-tilt-range: 20.0
```

### gRPC Service Implementation
```java
@GrpcService
public class AvatarGrpcService extends AvatarServiceGrpc.AvatarServiceImplBase {
    
    @Autowired
    private ExpressionController expressionController;
    
    @Autowired
    private SpeechAnimationService speechAnimationService;
    
    @Override
    public void triggerExpression(TriggerExpressionRequest request,
                                StreamObserver<TriggerExpressionResponse> responseObserver) {
        try {
            expressionController.expressEmotion(
                request.getEmotion(), 
                request.getIntensity()
            ).get();
            
            TriggerExpressionResponse response = TriggerExpressionResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Expression triggered successfully")
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to trigger expression: " + e.getMessage())
                .asRuntimeException());
        }
    }
    
    @Override
    public void updateParameters(UpdateParametersRequest request,
                               StreamObserver<UpdateParametersResponse> responseObserver) {
        try {
            Map<String, Float> parameters = request.getParametersMap()
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().floatValue()
                ));
                
            avatarService.updateParameters(parameters).get();
            
            UpdateParametersResponse response = UpdateParametersResponse.newBuilder()
                .setSuccess(true)
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to update parameters: " + e.getMessage())
                .asRuntimeException());
        }
    }
}
```

### REST Controller
```java
@RestController
@RequestMapping("/api/v1/avatar")
public class AvatarController {
    
    @Autowired
    private ExpressionController expressionController;
    
    @PostMapping("/expression")
    public ResponseEntity<Map<String, Object>> triggerExpression(
            @RequestBody ExpressionRequest request) {
        try {
            expressionController.expressEmotion(
                request.getEmotion(), 
                request.getIntensity()
            ).get();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Expression triggered successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/parameters")
    public ResponseEntity<Map<String, Object>> updateParameters(
            @RequestBody Map<String, Float> parameters) {
        try {
            avatarService.updateParameters(parameters).get();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "updated_parameters", parameters.keySet()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean connected = avatarService.isConnected();
        return ResponseEntity.ok(Map.of(
            "service", "avatar",
            "status", connected ? "healthy" : "disconnected",
            "vtube_studio_connected", connected
        ));
    }
}
```

## Technical Considerations

### Error Handling & Reconnection
```java
@Component
public class VTubeStudioConnectionManager {
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void healthCheck() {
        if (!avatarService.isConnected()) {
            log.warn("VTube Studio connection lost, attempting reconnection...");
            avatarService.connectToVTubeStudio();
        }
    }
    
    @EventListener
    public void handleConnectionLost(VTSConnectionLostEvent event) {
        // Implement exponential backoff reconnection strategy
        scheduleReconnection(1000); // Start with 1 second delay
    }
    
    private void scheduleReconnection(long delayMs) {
        scheduler.schedule(() -> {
            try {
                avatarService.connectToVTubeStudio().get();
            } catch (Exception e) {
                long nextDelay = Math.min(delayMs * 2, 30000); // Max 30 seconds
                scheduleReconnection(nextDelay);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }
}
```

### Integration Points with Other Skills

#### With Speech Skill
```java
@EventListener
public void onSpeechStart(SpeechStartEvent event) {
    Map<String, Float> params = Map.of(
        JailbirdParameter.SPEECH_ACTIVITY.getParameterName(), 1.0f
    );
    avatarService.updateParameters(params);
}

@EventListener  
public void onSpeechEnd(SpeechEndEvent event) {
    Map<String, Float> params = Map.of(
        JailbirdParameter.SPEECH_ACTIVITY.getParameterName(), 0.0f
    );
    avatarService.updateParameters(params);
}
```

#### With Conversation Skill
```java
@EventListener
public void onConversationResponse(ConversationResponseEvent event) {
    expressionController.expressEmotion(
        event.getEmotion(), 
        event.getIntensity()
    );
    
    Map<String, Float> params = Map.of(
        JailbirdParameter.RESPONSE_TYPE.getParameterName(), 
        mapResponseTypeToFloat(event.getResponseType())
    );
    avatarService.updateParameters(params);
}
```

## Testing Strategy

### TestContainers Integration
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class AvatarServiceIntegrationTest {
    
    @MockBean
    private VTubeStudioClient vtsClient;
    
    @Autowired
    private AvatarService avatarService;
    
    @Test
    @Order(1)
    void shouldConnectToVTubeStudio() {
        // Mock successful connection
        when(vtsClient.connect()).thenReturn(CompletableFuture.completedFuture(null));
        
        CompletableFuture<Void> result = avatarService.connectToVTubeStudio();
        
        assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS));
        verify(vtsClient).connect();
    }
    
    @Test
    void shouldTriggerExpressionSuccessfully() {
        when(vtsClient.sendRequest(any())).thenReturn(
            CompletableFuture.completedFuture(createMockResponse()));
            
        CompletableFuture<Void> result = avatarService.triggerExpression("happy");
        
        assertDoesNotThrow(() -> result.get(1, TimeUnit.SECONDS));
    }
}
```

## Future Enhancements

### Advanced Features
- **Emotion Recognition**: Analyze conversation context for automatic expressions
- **Physics Integration**: Dynamic physics based on movement patterns  
- **Multi-Model Support**: Support for multiple Live2D models per persona
- **Custom Animation Sequences**: Complex animation chains for storytelling

### Performance Optimizations
- **Parameter Batching**: Batch multiple parameter updates for efficiency
- **Connection Pooling**: Multiple WebSocket connections for high-frequency updates
- **Caching**: Cache frequently used expressions and parameters

This Java implementation provides a robust foundation for integrating Live2D avatars with the Jailbird system while maintaining Spring Boot best practices and ensuring reliable real-time avatar control.