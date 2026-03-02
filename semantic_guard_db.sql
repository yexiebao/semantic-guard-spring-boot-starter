CREATE DATABASE semantic_guard_db;

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS semantic_sensitive_words (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,                -- 敏感词文本内容
    category TEXT,                         -- 词库分类（如：政治、色情）
    word_vector vector(1024),              -- 明确维度为 1024
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 针对 1024 维度的 HNSW 索引
CREATE INDEX idx_sensitive_vector ON semantic_sensitive_words 
USING hnsw (word_vector vector_cosine_ops);