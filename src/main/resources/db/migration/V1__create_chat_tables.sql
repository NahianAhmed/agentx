-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS agentx;

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS agentx.conversation_titles (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    title TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB
);

CREATE TABLE IF NOT EXISTS agentx.conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_title_id TEXT NOT NULL REFERENCES agentx.conversation_titles(id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    tool_call_id TEXT,
    tool_name TEXT,
    metadata JSONB,
    embedding public.vector(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversation_titles_user_id
    ON agentx.conversation_titles(user_id);

CREATE INDEX IF NOT EXISTS idx_conversation_titles_updated_at
    ON agentx.conversation_titles(updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_conversations_conversation_created_at
    ON agentx.conversations(conversation_title_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_conversations_embedding_ivfflat
    ON agentx.conversations
    USING ivfflat (embedding public.vector_cosine_ops)
    WITH (lists = 100);
