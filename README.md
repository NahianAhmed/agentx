# AgentX - AI Chat API with Persistent Memory

A production-ready AI chat service built with Micronaut and LangChain4j, featuring persistent conversation memory powered by PostgreSQL + pgvector. Implements clean architecture patterns with semantic search, function calling, and scalable conversation management.

## ✨ Key Features

- 🤖 **Conversational AI** - Context-aware responses using LangChain4j and OpenAI GPT-4
- 🧠 **Persistent Memory** - PostgreSQL + pgvector for conversation history and semantic search
- 🔍 **Semantic Search** - Vector similarity search for intelligent context retrieval
- 🛠️ **Function Calling** - Built-in tools for calculations, date/time, and string operations
- 🏗️ **Clean Architecture** - Separated use cases, services, and repositories
- ⚡ **Production Ready** - Proper error handling, logging, and configuration
- 🔧 **Custom Memory Provider** - Manual ChatMemoryProvider implementation for full control

## 🏛️ Architecture

Built using **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────┐
│  Controller Layer (HTTP/API)                     │
├─────────────────────────────────────────────────┤
│  Use Case Layer (Business Logic Orchestration)  │
├─────────────────────────────────────────────────┤
│  Service Layer (Domain Services)                │
├─────────────────────────────────────────────────┤
│  Repository Layer (Data Access)                 │
└─────────────────────────────────────────────────┘
```

### Memory Architecture

**Hybrid Memory Approach:**
- **Recent Context**: LangChain4j's `MessageWindowChatMemory` with custom PostgreSQL storage
- **Semantic Context**: Vector similarity search using pgvector for relevant past conversations
- **Embeddings**: OpenAI Embeddings for semantic understanding

## 📦 Prerequisites

- **Java 21** or higher
- **PostgreSQL 14+** with pgvector extension
- **OpenAI API Key**

## 🚀 Setup

### 1. Database Setup

Install PostgreSQL and the pgvector extension:

```sql
-- Create database
CREATE DATABASE agentx;

-- Connect to database
\c agentx

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
```

Run the schema migrations (see `src/main/resources/db/migration/`)

### 2. Configuration

Create `src/main/resources/application-local.yml`:

```yaml
datasources:
  default:
    url: jdbc:postgresql://localhost:5432/agentx
    username: your_username
    password: your_password
    driver-class-name: org.postgresql.Driver

langchain4j:
  openai:
    api-key: your-openai-api-key
    chat-model:
      model-name: gpt-4
      temperature: 0.7
      max-tokens: 1000
    embedding-model:
      model-name: text-embedding-3-small

chat-memory:
  max-messages: 20
  max-similar-messages: 5
  similarity-threshold: 0.7
```

Or use environment variables:

```bash
export OPENAI_API_KEY=your-api-key-here
export DATASOURCES_DEFAULT_URL=jdbc:postgresql://localhost:5432/agentx
export DATASOURCES_DEFAULT_USERNAME=your_username
export DATASOURCES_DEFAULT_PASSWORD=your_password
```

### 3. Run the Application

```bash
./gradlew run
```

The application will start on `http://localhost:8080`

## 📖 API Documentation

### Chat Endpoint

**POST** `/api/chat`

Creates or continues a conversation with the AI assistant.

#### Request Body

```json
{
  "message": "What is 25 + 17?",
  "conversationId": "optional-uuid"
}
```

- `message` (required): The user's message
- `conversationId` (optional): UUID for conversation continuity. If omitted, a new conversation is created.

#### Response

```json
{
  "response": "I'll calculate that for you. 25 + 17 = 42.",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Clear Conversation

**DELETE** `/api/chat/{conversationId}`

Deletes all messages and history for a conversation.

### Example cURL Commands

```bash
# Start a new conversation
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello! What can you help me with?"}'

# Continue a conversation
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What was my first question?",
    "conversationId": "550e8400-e29b-41d4-a716-446655440000"
  }'

# Using calculator tools
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Calculate the square root of 144"}'

# Using datetime tools
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What time is it in Tokyo?"}'

# Clear conversation
curl -X DELETE http://localhost:8080/api/chat/550e8400-e29b-41d4-a716-446655440000
```

## 🛠️ Available Tools

The AI assistant has access to the following function calling tools:

### Calculator Tools
- `add(a, b)` - Sum of two numbers
- `subtract(a, b)` - Difference between numbers
- `multiply(a, b)` - Product of numbers
- `divide(a, b)` - Division result
- `sqrt(a)` - Square root
- `power(base, exponent)` - Exponentiation

### DateTime Tools
- `getCurrentDateTime()` - Current date and time
- `getCurrentDateTimeInZone(zoneId)` - Time in specific timezone (e.g., "Asia/Dhaka", "America/New_York")
- `getCurrentDate()` - Current date
- `getCurrentTime()` - Current time
- `getCurrentTimeInZone(zoneId)` - Time in specific timezone

### String Tools
- `toUpperCase(text)` - Convert to uppercase
- `toLowerCase(text)` - Convert to lowercase
- `reverse(text)` - Reverse string
- `getLength(text)` - String length
- `countOccurrences(text, substring)` - Count substring occurrences

## 📁 Project Structure

```
src/main/kotlin/com/agentx/
├── config/
│   ├── ChatMemoryConfiguration.kt     # Memory provider configuration
│   └── AiServiceConfiguration.kt      # AI service bean factory
├── controller/
│   └── ChatController.kt              # REST API endpoints
├── dto/
│   ├── ChatRequest.kt                 # Request DTO
│   └── ChatResponse.kt                # Response DTO
├── memory/
│   └── PersistChatMemoryStore.kt      # PostgreSQL memory storage
├── model/
│   ├── Conversation.kt                # Conversation entity
│   └── ConversationTitle.kt           # Conversation metadata
├── repository/
│   ├── ConversationRepository.kt      # JPA repository
│   ├── ConversationRepositoryCustom.kt # Custom queries
│   └── ConversationTitleRepository.kt # Metadata repository
├── service/
│   ├── ChatAgentService.kt            # LangChain4j AI service interface
│   ├── ConversationService.kt         # Conversation persistence
│   └── EmbeddingService.kt            # OpenAI embeddings
├── tools/
│   ├── CalculatorTools.kt             # Math operations
│   ├── DateTimeTools.kt               # Date/time operations
│   └── StringTools.kt                 # String operations
├── usecase/
│   └── ChatConversationUseCase.kt     # Business logic orchestration
└── Application.kt                     # Main application
```

## 🏗️ Technical Architecture

### Memory Management

The application uses a **custom ChatMemoryProvider** implementation:

1. **PostgresChatMemoryStore**: Implements LangChain4j's `ChatMemoryStore` interface
2. **ChatMemoryProvider**: Wraps the store and creates `MessageWindowChatMemory` instances
3. **Manual AI Service Building**: Uses `AiServices.builder()` instead of `@AiService` annotation for full control

**Why Manual Building?**
Micronaut's `@AiService` annotation doesn't support the `chatMemoryProvider` parameter, so we manually construct the service bean to properly wire the custom memory provider.

### Conversation Flow

1. **User sends message** → Controller validates and generates/uses conversation ID
2. **Use Case orchestrates**:
   - Generates embedding for user message
   - Stores message with embedding in PostgreSQL
   - Performs semantic search for similar past messages
   - Enriches context if relevant history is found
3. **ChatMemoryProvider loads** recent conversation history from PostgreSQL
4. **LangChain4j combines**:
   - Recent messages (from ChatMemory)
   - Semantic context (from vector search)
   - System prompt and user message
5. **AI generates response** with access to function calling tools
6. **Response stored** with embedding for future semantic search

## 🧪 Building & Testing

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run with hot reload
./gradlew run --continuous
```

## 🐳 Docker Support (Coming Soon)

```bash
# Run with Docker Compose
docker-compose up
```

## 📚 Resources

- [Micronaut Documentation](https://docs.micronaut.io/latest/guide/)
- [Micronaut LangChain4j](https://micronaut-projects.github.io/micronaut-langchain4j/latest/guide/)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [pgvector](https://github.com/pgvector/pgvector)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License.

---

**Built with ❤️ using Kotlin, Micronaut, and LangChain4j**
