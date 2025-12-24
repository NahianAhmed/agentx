# Chat Memory Implementation Plan with PostgreSQL & PGVector

## Overview
Implement a custom chat memory solution using PostgreSQL with pgvector extension for semantic search of conversation history. This enables context-aware conversations by retrieving relevant past messages based on semantic similarity.

## Architecture

### Components
1. **PostgreSQL Database** - Store conversation messages
2. **PGVector Extension** - Vector similarity search for embeddings
3. **OpenAI Embeddings** - text-embedding-3-small model for message embeddings
4. **Chat Memory Service** - Manages conversation history and retrieval
5. **Message Repository** - Data access layer for PostgreSQL

### Data Flow
```
User Message
  → Generate Embedding (text-embedding-3-small)
  → Find Similar Messages (pgvector similarity search)
  → Build Context (current conversation + similar messages)
  → Send to LLM with context
  → Store Response with Embedding
```

## Database Schema

### Table: `chat_messages`
```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'USER' or 'ASSISTANT'
    content TEXT NOT NULL,
    embedding vector(1536), -- text-embedding-3-small produces 1536-dimensional vectors
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB -- For storing additional context, user info, etc.
);

-- Index for conversation retrieval
CREATE INDEX idx_conversation_id ON chat_messages(conversation_id);
CREATE INDEX idx_created_at ON chat_messages(created_at);

-- Vector similarity index (HNSW for fast approximate nearest neighbor search)
CREATE INDEX idx_embedding_hnsw ON chat_messages
USING hnsw (embedding vector_cosine_ops);

-- Alternative: IVFFlat index (faster insertion, slower search)
-- CREATE INDEX idx_embedding_ivfflat ON chat_messages
-- USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

### Table: `conversations`
```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255), -- Optional: for multi-user support
    title VARCHAR(255), -- Optional: conversation title
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB -- Additional conversation metadata
);

CREATE INDEX idx_user_id ON conversations(user_id);
CREATE INDEX idx_updated_at ON conversations(updated_at);
```

## Implementation Steps

### Phase 1: Dependencies & Configuration

#### 1.1 Add Dependencies to `build.gradle.kts`
```kotlin
dependencies {
    // Existing dependencies...

    // PostgreSQL & Micronaut Data
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // Liquibase for database migrations
    implementation("io.micronaut.liquibase:micronaut-liquibase")

    // OpenAI for embeddings
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2") // Fallback
    // Note: OpenAI embeddings are already available via langchain4j-open-ai
}
```

#### 1.2 Update `application.properties`
```properties
# Database Configuration
datasources.default.url=jdbc:postgresql://postgresql.postgresql:5432/ideascale
datasources.default.username=ideascale
datasources.default.password=brewski01
datasources.default.driver-class-name=org.postgresql.Driver
datasources.default.dialect=POSTGRES

# Liquibase Configuration
liquibase.datasources.default.change-log=classpath:db/changelog/db.changelog-master.xml

# OpenAI Embeddings Configuration
langchain4j.open-ai.embedding-model.model-name=text-embedding-3-small
langchain4j.open-ai.embedding-model.dimensions=1536

# Chat Memory Configuration
chat-memory.max-messages=20
chat-memory.max-similar-messages=5
chat-memory.similarity-threshold=0.7
```

### Phase 2: Database Setup

#### 2.1 Create Liquibase Changelog
**File: `src/main/resources/db/changelog/db.changelog-master.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="classpath:db/changelog/01-create-pgvector-extension.xml"/>
    <include file="classpath:db/changelog/02-create-conversations-table.xml"/>
    <include file="classpath:db/changelog/03-create-chat-messages-table.xml"/>
</databaseChangeLog>
```

#### 2.2 Create Individual Changesets
- `01-create-pgvector-extension.xml`
- `02-create-conversations-table.xml`
- `03-create-chat-messages-table.xml`

### Phase 3: Domain Models

#### 3.1 Create Entity Classes
```kotlin
// ChatMessage.kt
@MappedEntity("chat_messages")
data class ChatMessage(
    @field:Id
    val id: UUID? = null,

    @field:Column("conversation_id")
    val conversationId: UUID,

    val role: MessageRole,

    val content: String,

    @field:Column("embedding")
    @field:TypeDef(type = DataType.OBJECT)
    val embedding: FloatArray?,

    @field:DateCreated
    val createdAt: Instant? = null,

    @field:TypeDef(type = DataType.JSON)
    val metadata: Map<String, Any>? = null
)

enum class MessageRole {
    USER, ASSISTANT
}

// Conversation.kt
@MappedEntity("conversations")
data class Conversation(
    @field:Id
    val id: UUID? = null,

    @field:Column("user_id")
    val userId: String?,

    val title: String?,

    @field:DateCreated
    val createdAt: Instant? = null,

    @field:DateUpdated
    val updatedAt: Instant? = null,

    @field:TypeDef(type = DataType.JSON)
    val metadata: Map<String, Any>? = null
)
```

### Phase 4: Data Access Layer

#### 4.1 Create Repositories
```kotlin
// ChatMessageRepository.kt
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ChatMessageRepository : CrudRepository<ChatMessage, UUID> {

    fun findByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<ChatMessage>

    @Query("""
        SELECT * FROM chat_messages
        WHERE conversation_id = :conversationId
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun findRecentMessages(conversationId: UUID, limit: Int): List<ChatMessage>

    @Query("""
        SELECT *, (embedding <=> cast(:embedding as vector)) as distance
        FROM chat_messages
        WHERE conversation_id = :conversationId
          AND embedding IS NOT NULL
        ORDER BY distance
        LIMIT :limit
    """)
    fun findSimilarMessages(
        conversationId: UUID,
        embedding: FloatArray,
        limit: Int
    ): List<ChatMessage>

    @Query("""
        SELECT *, (embedding <=> cast(:embedding as vector)) as distance
        FROM chat_messages
        WHERE embedding IS NOT NULL
          AND (embedding <=> cast(:embedding as vector)) < :threshold
        ORDER BY distance
        LIMIT :limit
    """)
    fun findSimilarMessagesWithThreshold(
        embedding: FloatArray,
        threshold: Double,
        limit: Int
    ): List<ChatMessage>

    fun deleteByConversationId(conversationId: UUID): Int
}

// ConversationRepository.kt
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ConversationRepository : CrudRepository<Conversation, UUID> {

    fun findByUserId(userId: String): List<Conversation>

    @Query("SELECT * FROM conversations ORDER BY updated_at DESC LIMIT :limit")
    fun findRecentConversations(limit: Int): List<Conversation>
}
```

### Phase 5: Embedding Service

#### 5.1 Create Embedding Service
```kotlin
@Singleton
class EmbeddingService(
    @Named("embeddingModel")
    private val embeddingModel: EmbeddingModel
) {
    private val logger = LoggerFactory.getLogger(EmbeddingService::class.java)

    /**
     * Generate embedding for a text using OpenAI text-embedding-3-small
     */
    fun generateEmbedding(text: String): FloatArray {
        logger.debug("Generating embedding for text: ${text.take(50)}...")

        val response = embeddingModel.embed(text)
        val embedding = response.content().vector()

        logger.debug("Generated embedding with ${embedding.size} dimensions")
        return embedding
    }

    /**
     * Batch generate embeddings for multiple texts
     */
    fun generateEmbeddings(texts: List<String>): List<FloatArray> {
        logger.debug("Generating embeddings for ${texts.size} texts")

        val response = embeddingModel.embedAll(texts)
        return response.content().map { it.vector() }
    }
}
```

#### 5.2 Configure Embedding Model Bean
```kotlin
@Factory
class EmbeddingModelConfig {

    @Singleton
    @Named("embeddingModel")
    fun embeddingModel(
        @Value("\${langchain4j.open-ai.api-key}") apiKey: String,
        @Value("\${langchain4j.open-ai.embedding-model.model-name}") modelName: String
    ): EmbeddingModel {
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .build()
    }
}
```

### Phase 6: Chat Memory Service

#### 6.1 Create Enhanced Chat Memory Service
```kotlin
@Singleton
class ChatMemoryService(
    private val chatMessageRepository: ChatMessageRepository,
    private val conversationRepository: ConversationRepository,
    private val embeddingService: EmbeddingService,
    private val chatAgentService: ChatAgentService,
    @Value("\${chat-memory.max-messages:20}")
    private val maxMessages: Int,
    @Value("\${chat-memory.max-similar-messages:5}")
    private val maxSimilarMessages: Int,
    @Value("\${chat-memory.similarity-threshold:0.7}")
    private val similarityThreshold: Double
) {
    private val logger = LoggerFactory.getLogger(ChatMemoryService::class.java)

    /**
     * Process chat with conversation memory and semantic search
     */
    @Transactional
    fun chat(conversationId: String, userMessage: String): String {
        val convId = UUID.fromString(conversationId)

        // 1. Generate embedding for user message
        val userEmbedding = embeddingService.generateEmbedding(userMessage)

        // 2. Store user message with embedding
        val userMsg = ChatMessage(
            conversationId = convId,
            role = MessageRole.USER,
            content = userMessage,
            embedding = userEmbedding
        )
        chatMessageRepository.save(userMsg)

        // 3. Get conversation context
        val context = buildConversationContext(convId, userEmbedding)

        // 4. Build prompt with context
        val promptWithContext = buildPromptWithContext(userMessage, context)

        // 5. Get response from LLM
        val response = chatAgentService.chat(promptWithContext)

        // 6. Generate embedding for assistant response
        val assistantEmbedding = embeddingService.generateEmbedding(response)

        // 7. Store assistant message with embedding
        val assistantMsg = ChatMessage(
            conversationId = convId,
            role = MessageRole.ASSISTANT,
            content = response,
            embedding = assistantEmbedding
        )
        chatMessageRepository.save(assistantMsg)

        // 8. Update conversation timestamp
        updateConversationTimestamp(convId)

        return response
    }

    /**
     * Build conversation context using recent messages + similar messages
     */
    private fun buildConversationContext(
        conversationId: UUID,
        currentEmbedding: FloatArray
    ): ConversationContext {
        // Get recent messages from this conversation
        val recentMessages = chatMessageRepository
            .findRecentMessages(conversationId, maxMessages)
            .reversed() // Oldest first

        // Get semantically similar messages
        val similarMessages = chatMessageRepository
            .findSimilarMessages(conversationId, currentEmbedding, maxSimilarMessages)
            .filter { it !in recentMessages } // Avoid duplicates

        return ConversationContext(
            recentMessages = recentMessages,
            similarMessages = similarMessages
        )
    }

    /**
     * Build prompt with conversation context
     */
    private fun buildPromptWithContext(
        userMessage: String,
        context: ConversationContext
    ): String {
        val sb = StringBuilder()

        // Add similar messages context if available
        if (context.similarMessages.isNotEmpty()) {
            sb.append("## Relevant Context from Past Conversations:\n")
            context.similarMessages.forEach { msg ->
                sb.append("${msg.role}: ${msg.content}\n")
            }
            sb.append("\n")
        }

        // Add recent conversation history
        if (context.recentMessages.isNotEmpty()) {
            sb.append("## Recent Conversation:\n")
            context.recentMessages.forEach { msg ->
                sb.append("${msg.role}: ${msg.content}\n")
            }
            sb.append("\n")
        }

        // Add current message
        sb.append("## Current Question:\n")
        sb.append(userMessage)

        return sb.toString()
    }

    /**
     * Update conversation's updated_at timestamp
     */
    private fun updateConversationTimestamp(conversationId: UUID) {
        conversationRepository.findById(conversationId).ifPresentOrElse(
            { conv ->
                conversationRepository.update(
                    conv.copy(updatedAt = Instant.now())
                )
            },
            {
                // Create conversation if it doesn't exist
                conversationRepository.save(
                    Conversation(
                        id = conversationId,
                        userId = null,
                        title = null
                    )
                )
            }
        )
    }

    /**
     * Clear conversation history
     */
    fun clearConversation(conversationId: String) {
        val convId = UUID.fromString(conversationId)
        chatMessageRepository.deleteByConversationId(convId)
        conversationRepository.deleteById(convId)
        logger.info("Cleared conversation: $conversationId")
    }
}

data class ConversationContext(
    val recentMessages: List<ChatMessage>,
    val similarMessages: List<ChatMessage>
)
```

### Phase 7: Update Controller

#### 7.1 Update ChatController
```kotlin
@Controller("/api/chat")
class ChatController(
    private val chatMemoryService: ChatMemoryService
) {
    // Update chat endpoint to use ChatMemoryService
    @Post
    fun chat(@Body request: ChatRequest): HttpResponse<*> {
        // ... validation ...

        val response = chatMemoryService.chat(conversationId, request.message)

        // ... return response ...
    }

    // Add endpoint to clear conversation
    @Delete("/{conversationId}")
    fun clearConversation(@PathVariable conversationId: String): HttpResponse<*> {
        chatMemoryService.clearConversation(conversationId)
        return HttpResponse.ok(mapOf("message" to "Conversation cleared"))
    }
}
```

## Testing Strategy

### 1. Unit Tests
- Test embedding generation
- Test vector similarity calculations
- Test message retrieval logic

### 2. Integration Tests
- Test database operations
- Test full conversation flow
- Test semantic search accuracy

### 3. Manual Testing Scenarios
```bash
# Test 1: Start conversation
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "My favorite color is blue"}'

# Test 2: Continue conversation (should remember)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is my favorite color?", "conversationId": "<id>"}'

# Test 3: Test semantic search (ask related question)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What colors do I like?", "conversationId": "<id>"}'
```

## Performance Considerations

### 1. Embedding Generation
- **Cache Strategy**: Cache embeddings for common queries
- **Batch Processing**: Generate embeddings in batches when possible
- **Async Processing**: Consider async embedding generation for non-critical paths

### 2. Vector Search
- **Index Selection**: HNSW for fast search, IVFFlat for fast insertion
- **Dimension Reduction**: Consider using smaller embedding dimensions if needed
- **Result Limit**: Limit similarity search results (default: 5 messages)

### 3. Database
- **Connection Pooling**: Configure HikariCP properly
- **Query Optimization**: Use proper indexes
- **Cleanup Strategy**: Implement old message cleanup (optional)

## Security Considerations

1. **SQL Injection**: Use parameterized queries (handled by Micronaut Data)
2. **API Key Security**: Store OpenAI API key in environment variables
3. **Database Credentials**: Use secrets management in production
4. **Rate Limiting**: Add rate limiting to prevent abuse
5. **Conversation Access**: Add user authentication/authorization

## Future Enhancements

1. **Multi-user Support**: Add user authentication and conversation ownership
2. **Conversation Summarization**: Summarize long conversations
3. **Export/Import**: Export conversation history
4. **Analytics**: Track conversation metrics
5. **Hybrid Search**: Combine vector search with keyword search
6. **Fine-tuning**: Fine-tune embedding model for domain-specific use cases

## Rollback Plan

If issues arise:
1. Keep the simple stateless implementation as fallback
2. Feature flag to toggle between simple and advanced memory
3. Database migrations can be rolled back via Liquibase
4. Monitor error rates and performance metrics

## Timeline Estimate

- **Phase 1-2**: Dependencies & Database Setup - 2 hours
- **Phase 3-4**: Domain Models & Repositories - 2 hours
- **Phase 5**: Embedding Service - 1 hour
- **Phase 6**: Chat Memory Service - 3 hours
- **Phase 7**: Controller Updates - 1 hour
- **Testing & Debugging**: 2-3 hours

**Total**: ~11-12 hours

## Success Criteria

- ✅ Messages stored with embeddings in PostgreSQL
- ✅ Semantic similarity search returns relevant messages
- ✅ Conversations maintain context across messages
- ✅ Response time < 2 seconds for chat requests
- ✅ Embedding generation successful rate > 99%
- ✅ No memory leaks or connection pool exhaustion
