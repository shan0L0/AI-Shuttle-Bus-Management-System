-- init.sql
-- 创建向量扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建示例表
CREATE TABLE IF NOT EXISTS knowledge_vectors (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) UNIQUE,
    embedding vector(1024) NOT NULL,
    metadata JSONB DEFAULT '{}',
    source_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );