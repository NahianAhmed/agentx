# AgentX - Agentic AI Chat API

A powerful agentic AI application built with Micronaut and LangChain4j that provides a chat API with function calling capabilities.

## Features

- **Chat API**: RESTful endpoint for AI conversations
- **Chat Memory**: Maintains conversation context across messages
- **OpenAI Integration**: Uses GPT-4 for intelligent responses
- **Function Calling**: AI can call tools to perform calculations, date/time operations, and string manipulations
- **Kotlin**: Written entirely in Kotlin

## Available Tools

The AI assistant has access to the following tools:

### Calculator Tools
- `add(a, b)` - Calculate sum of two numbers
- `subtract(a, b)` - Calculate difference
- `multiply(a, b)` - Calculate product
- `divide(a, b)` - Calculate division
- `sqrt(a)` - Calculate square root
- `power(base, exponent)` - Calculate power

### DateTime Tools
- `getCurrentDateTime()` - Get current date and time
- `getCurrentDateTimeInZone(zoneId)` - Get time for specific timezone
- `getCurrentDate()` - Get current date
- `getCurrentTime()` - Get current time

### String Tools
- `toUpperCase(text)` - Convert to uppercase
- `toLowerCase(text)` - Convert to lowercase
- `reverse(text)` - Reverse a string
- `getLength(text)` - Get string length
- `countOccurrences(text, substring)` - Count substring occurrences

## Prerequisites

- Java 21 or higher
- OpenAI API key

## Configuration

Set your OpenAI API key as an environment variable:

```bash
export OPENAI_API_KEY=your-api-key-here
```

Or create a `local.yml` file in `src/main/resources/`:

```yaml
langchain4j:
  openai:
    api-key: your-api-key-here
    chat-model:
      model-name: gpt-4
      temperature: 0.7
      max-tokens: 1000
```

## Running the Application

```bash
./gradlew run
```

The application will start on `http://localhost:8080`

## API Usage

### Chat Endpoint

**POST** `/api/chat`

Request:
```json
{
  "message": "What is 25 + 17?",
  "conversationId": "optional-conversation-id"
}
```

Response:
```json
{
  "response": "The sum of 25 and 17 is 42.",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Example cURL Commands

```bash
# Simple chat
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello! Can you help me?"}'

# Using calculator tools
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the square root of 144?"}'

# Using datetime tools
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What time is it?"}'

# With conversation ID for context
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What was my previous question?", "conversationId": "some-id"}'
```

## Project Structure

```
src/main/kotlin/com/agentx/
├── controller/
│   └── ChatController.kt          # REST API controller
├── dto/
│   ├── ChatRequest.kt             # Request DTO
│   └── ChatResponse.kt            # Response DTO
├── service/
│   └── ChatAgentService.kt        # AI service with LangChain4j
├── tools/
│   ├── CalculatorTools.kt         # Math function tools
│   ├── DateTimeTools.kt           # Date/time function tools
│   └── StringTools.kt             # String manipulation tools
└── Application.kt                 # Main application class
```

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## Resources

- [Micronaut Documentation](https://docs.micronaut.io/4.10.6/guide/index.html)
- [Micronaut LangChain4j Guide](https://micronaut-projects.github.io/micronaut-langchain4j/latest/guide/)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)

