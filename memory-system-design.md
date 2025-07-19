# Jailbird Memory & RAG System Design (Java Implementation)

*Based on the AIRI project (available at ../airi) — a production AI VTuber system with battle-tested memory architecture*

---

## Memory System Design

**AIRI (Artificial Intelligence Real-time Intelligence)** is an open-source AI VTuber project that serves as an excellent reference implementation for sophisticated memory systems in production use with real-time chat interaction, game playing, and multi-platform integration. Jailbird draws inspiration from AIRI's proven memory patterns while implementing its own solution within the skills-based microservice architecture using Java and Spring Boot.

**Key Concepts Inspired by AIRI:**
- **Hierarchical memory layers** with different retention and access patterns
- **Episodic memory system** for event-based storage and context reconstruction
- **Contextual retrieval** with conversation windows for narrative coherence
- **Goal-oriented memory** for planning and task management
- **Emotional impact scoring** and temporal decay for human-like memory patterns
- **Multi-dimensional approaches** to memory importance and relevance

---

## Multi-Phase RAG Strategy

### Phase 1: Hybrid Dense + Sparse Foundation
- **Dense Vectors**: Using sentence-transformers via Python bridge or native Java embeddings (384d)
- **Sparse Search**: Lucene/Elasticsearch integration for exact matches (usernames, commands, facts)
- **Fusion Scoring**: Weighted combination (0.7 * dense + 0.3 * sparse)
- **Storage**: Redis with Jedis/Lettuce for vector + full-text search

### Phase 2: Temporal Weighting System
- **Importance Decay**: Exponential decay for memory relevance over time
- **Access Frequency**: Boost based on retrieval patterns
- **Emotional Impact**: Weight memories by significance (-10 to +10 scale)
- **Recency Bias**: Recent memories get automatic importance boost

### Phase 3: Hierarchical Memory Layers
```
Working Memory (last 10 messages)
├── Dense vectors only for speed
├── In-memory cache (Caffeine)
└── Direct context injection

Short-term Memory (last hour)
├── Hybrid dense + sparse retrieval
├── Redis caching layer
└── Conversation continuity focus

Long-term Memory (everything else)
├── Importance-filtered hybrid search
├── Full temporal + emotional weighting
└── Relationship and fact storage
```

---

## Java Spring Boot Implementation Architecture

### Memory Service Structure
```java
@Service
public class MemoryService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    @Autowired
    private MemoryConsolidationService consolidationService;
    
    @Cacheable("working-memory")
    public List<MemoryFragment> getWorkingMemory(String personaId) {
        // Retrieve last 10 messages from cache
    }
    
    public List<MemoryFragment> searchMemories(SearchQuery query) {
        // Multi-dimensional hybrid search
    }
    
    @Async
    public CompletableFuture<Void> storeMemory(MemoryFragment memory) {
        // Async memory storage with embedding generation
    }
}
```

### Spring Data Redis Integration
```java
@RedisHash("memory_fragment")
public class MemoryFragment {
    @Id
    private String id;
    
    @Indexed
    private String personaId;
    
    private String content;
    private float[] embedding;  // 384-dimensional vector
    private Map<String, Double> sparseFeatures;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private MemoryMetadata metadata;
    
    // Constructors, getters, setters
}

@Repository
public interface MemoryRepository extends CrudRepository<MemoryFragment, String> {
    List<MemoryFragment> findByPersonaIdAndTimestampBetween(
        String personaId, LocalDateTime start, LocalDateTime end);
        
    @Query("SELECT * FROM memory_fragment WHERE persona_id = ?1 " +
           "ORDER BY importance DESC, timestamp DESC LIMIT ?2")
    List<MemoryFragment> findTopMemoriesByImportance(String personaId, int limit);
}
```

---

## AIRI-Enhanced Retrieval Pipeline (Java)

1. **Query Analysis**: Extract intent, entities, emotional context using NLP libraries
```java
@Component
public class QueryAnalyzer {
    
    @Autowired
    private NLPProcessor nlpProcessor;
    
    public AnalyzedQuery analyzeQuery(String query, String personaId) {
        return AnalyzedQuery.builder()
            .originalQuery(query)
            .intent(nlpProcessor.extractIntent(query))
            .entities(nlpProcessor.extractEntities(query))
            .emotionalContext(nlpProcessor.analyzeEmotion(query))
            .personaId(personaId)
            .build();
    }
}
```

2. **Multi-Dimensional Vector Search**: 
```java
@Service
public class VectorSearchService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public List<MemoryFragment> vectorSimilaritySearch(
            float[] queryEmbedding, String personaId, int limit) {
        // Use Redis vector similarity search
        // Apply similarity threshold (0.5+) for relevance filtering
        String luaScript = """
            local results = redis.call('FT.SEARCH', 'memory_idx', 
                '@persona_id:{%s} @embedding:[VECTOR_RANGE %f %s]', 
                ARGV[1], ARGV[2], ARGV[3])
            return results
            """;
        
        return executeVectorSearch(luaScript, personaId, queryEmbedding, limit);
    }
}
```

3. **Contextual Window Reconstruction**: 
```java
@Service
public class ContextualRetrievalService {
    
    public List<MemoryFragment> reconstructConversationContext(
            MemoryFragment centerMemory, int windowSize) {
        LocalDateTime centerTime = centerMemory.getTimestamp();
        LocalDateTime start = centerTime.minusMinutes(windowSize);
        LocalDateTime end = centerTime.plusMinutes(windowSize);
        
        return memoryRepository.findByPersonaIdAndTimestampBetween(
            centerMemory.getPersonaId(), start, end);
    }
}
```

4. **AIRI Hybrid Scoring**: 
```java
@Component
public class AIRIScoring {
    
    public double calculateHybridScore(MemoryFragment memory, AnalyzedQuery query) {
        double similarity = cosineSimilarity(query.getEmbedding(), memory.getEmbedding());
        double timeRelevance = calculateTimeRelevance(memory.getTimestamp());
        double importanceScore = memory.getMetadata().getImportance() / 10.0;
        double accessFrequency = memory.getMetadata().getAccessCount() / 100.0;
        double relationshipStrength = calculateRelationshipStrength(memory, query);
        
        double baseScore = 1.2 * similarity +
                          0.2 * timeRelevance +
                          0.1 * importanceScore +
                          0.1 * accessFrequency +
                          0.1 * relationshipStrength;
                          
        double emotionalModifier = memory.getMetadata().getEmotionalImpact() / 10.0;
        
        return baseScore * (1.0 + emotionalModifier);
    }
    
    private double calculateTimeRelevance(LocalDateTime timestamp) {
        Duration age = Duration.between(timestamp, LocalDateTime.now());
        double daysSinceCreation = age.toDays();
        return Math.max(0, 1.0 - (daysSinceCreation / 30.0)); // 30-day sliding window
    }
}
```

---

## Memory Consolidation Pipeline (Java)

1. **Real-time Embedding**: Generate embeddings during conversation
```java
@Service
public class EmbeddingService {
    
    @Async
    @EventListener
    public void handleNewMessage(MessageEvent event) {
        CompletableFuture.supplyAsync(() -> {
            float[] embedding = generateEmbedding(event.getContent());
            MemoryFragment memory = createMemoryFragment(event, embedding);
            memoryService.storeMemory(memory);
            return memory;
        });
    }
    
    private float[] generateEmbedding(String text) {
        // Call to embedding service (sentence-transformers via HTTP or native Java)
        // For now, can use a Python microservice or wait for native Java solutions
        return embeddingClient.generateEmbedding(text);
    }
}
```

2. **Batch Processing**: Use Spring's task scheduling
```java
@Component
public class MemoryConsolidationService {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void consolidateMemories() {
        List<MemoryFragment> recentMemories = memoryRepository
            .findRecentUnprocessedMemories();
            
        recentMemories.parallelStream()
            .forEach(this::updateImportanceScores);
    }
    
    private void updateImportanceScores(MemoryFragment memory) {
        double newImportance = calculateImportanceScore(memory);
        memory.getMetadata().setImportance(newImportance);
        memoryRepository.save(memory);
    }
}
```

---

## Data Schemas (Java)

### Memory Fragment Structure
```java
@RedisHash("memory_fragment")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryFragment {
    @Id
    private String id;
    
    @Indexed
    private String personaId;
    
    private String content;
    private float[] embedding;  // 384d dense vector
    private Map<String, Double> sparseFeatures;  // BM25 term frequencies
    
    private LocalDateTime timestamp;
    private MemoryMetadata metadata;
}

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMetadata {
    private MemoryType memoryType;  // WORKING, SHORT_TERM, LONG_TERM
    private MemoryCategory category; // CHAT, ACTION, EVENT, RELATIONSHIP
    private String source;          // "twitch_chat", "tts_output", "action_log"
    private String userId;
    private double importance;      // 1-10 scale
    private int emotionalImpact;    // -10 to +10 scale
    private int accessCount;
    private LocalDateTime lastAccessed;
}

public enum MemoryType {
    WORKING, SHORT_TERM, LONG_TERM
}

public enum MemoryCategory {
    CHAT, ACTION, EVENT, RELATIONSHIP, CONTEXT
}
```

---

## Spring Boot Integration Architecture

### Memory Service Integration
```java
@GrpcService
public class MemoryGrpcService extends MemoryServiceGrpc.MemoryServiceImplBase {
    
    @Autowired
    private MemoryService memoryService;
    
    @Override
    public void searchMemories(SearchMemoriesRequest request,
                             StreamObserver<SearchMemoriesResponse> responseObserver) {
        try {
            List<MemoryFragment> memories = memoryService.searchMemories(
                SearchQuery.from(request));
                
            SearchMemoriesResponse response = SearchMemoriesResponse.newBuilder()
                .addAllMemories(memories.stream()
                    .map(this::toProtoMemory)
                    .collect(Collectors.toList()))
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}

@RestController
@RequestMapping("/api/v1/memory")
public class MemoryController {
    
    @Autowired
    private MemoryService memoryService;
    
    @PostMapping("/search")
    public ResponseEntity<List<MemoryFragment>> searchMemories(
            @RequestBody SearchRequest request) {
        List<MemoryFragment> results = memoryService.searchMemories(
            SearchQuery.from(request));
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Memory service is healthy");
    }
}
```

### Configuration
```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

jailbird:
  memory:
    embedding-dimension: 384
    max-working-memory: 10
    consolidation-interval: 300000  # 5 minutes
    importance-threshold: 5.0
    similarity-threshold: 0.5
    
  personas:
    default:
      memory-namespace: "persona:default:memory"
      max-long-term-memories: 10000
```

---

## Performance Considerations (Java)

### Optimization Strategies
- **Connection Pooling**: Redis connection pooling with Lettuce
- **Async Processing**: CompletableFuture for non-blocking operations
- **Caching**: Caffeine cache for frequently accessed memories
- **Batch Operations**: Redis pipelining for bulk operations

### Spring Boot Specific
```java
@Configuration
@EnableCaching
public class MemoryConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES));
        return cacheManager;
    }
    
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(
            LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    @Bean
    public TaskExecutor memoryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("memory-");
        executor.initialize();
        return executor;
    }
}
```

---

This detailed design document provides the technical foundation for implementing Jailbird's sophisticated memory and RAG system using Java and Spring Boot, drawing from proven patterns in the AIRI project while adapting to the Spring-based microservice architecture.