# semantic-guard-spring-boot-starter

**License: MIT** (`LICENSE` file in repo root)

一个**通用、可插拔、跨向量模型**的「语义敏感词过滤」Spring Boot Starter，解耦“向量模型（Embedding）”与“向量存储（pgvector/内存）”，支持 DFA 硬匹配 + 向量语义双检测，可快速接入任意 Java 业务系统。目前处于开发中，欢迎提交 PR/Issue 共同完善。

A **generic, pluggable, model-agnostic** semantic sensitive-word filtering Spring Boot Starter.It decouples "vector models (Embedding)" from "vector storage (pgvector/in-memory)", supports dual detection with DFA hard matching + vector semantics, and can be quickly integrated into any Java business system. Currently under active development, PRs and Issues are welcome for joint improvement.

---

## 核心功能 / Core Features

- **双重检测**：DFA 硬匹配优先，语义向量相似度兜底  
  **Dual-stage detection**: DFA exact match first, vector semantic similarity as fallback
- **可插拔向量模型**：通过 `EmbeddingProvider` 接口接入你的模型（默认智谱）  
  **Pluggable embedding** via `EmbeddingProvider` (Zhipu provided by default)
- **可插拔向量库**：通过 `VectorStore` 接口选择 pgvector（生产）或内存（Demo）  
  **Pluggable vector store** via `VectorStore` (pgvector for production / memory for demo)
- **适配谐音、近义词、变体词**：依赖向量模型的语义空间能力  
  **Handles homophones/synonyms/variants** depending on embedding quality

---

## 落地效果（端到端流程）/ End-to-End Workflow

### 离线/启动阶段（构建词库）/ Offline or Startup (build the lexicon)

1. 配置 PostgreSQL（开启 pgvector）  
   Configure PostgreSQL with pgvector enabled
2. 读取“敏感词 TXT 文件夹”  
   Read sensitive-word TXT folder
3. 对每个敏感词调用你的 Embedding 模型转向量，并写入向量表（`semantic_sensitive_words`）  
   Vectorize each word using your embedding model, store into `semantic_sensitive_words`
4. 从数据库把“硬拦截词集合”加载到内存 DFA 前缀树（仅用于硬匹配）  
   Load the hard-block word set into DFA trie in memory (for exact match only)

### 在线检测阶段（拦截文本）/ Online Detection (filter input text)

- **Step 1：DFA**  
  文本命中 DFA → **直接拦截**（不再进行语义检索）  
  If DFA hits → **block immediately** (skip semantic stage)

- **Step 2：语义相似度**  
  DFA 放行 → 文本向量化 → pgvector TOP1 相似度检索 → 与阈值比较  
  If DFA passes → embed text → pgvector TOP1 similarity search → compare with threshold

---

## 快速开始 / Quick Start

### 1) 依赖 / Dependencies

```xml
<dependency>
  <groupId>io.github.yexeibao</groupId>
  <artifactId>semantic-guard-spring-boot-starter</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>

<!-- If you use pgvector store -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
</dependency>
```

### 2) 建表（pgvector）/ Schema (pgvector)

执行仓库内的 `semantic_guard_db.sql`（或手动执行如下 SQL）。  
Run `semantic_guard_db.sql` (or execute the SQL below).

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS semantic_sensitive_words (
  id BIGSERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  category TEXT,
  word_vector vector(1024),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sensitive_vector
ON semantic_sensitive_words
USING hnsw (word_vector vector_cosine_ops);
```

### 3) 配置 / Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/semantic_guard_db
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver

semantic-guard:
  enabled: true
  threshold: 0.85                # semantic similarity threshold
  vector-store-type: pgvector    # pgvector | memory
  zhipu:
    api-key: ${ZHIPU_API_KEY}    # DO NOT hardcode secrets
    base-url: https://open.bigmodel.cn/api/paas
    model-name: embedding-2
```

---

## 使用示例 / Usage Example

### 1) 对外极简 API / Minimal API

```java
@Autowired
private SemanticGuardService semanticGuardService;

public void check(String text) throws IOException {
    GuardResult r = semanticGuardService.check(text);
    if (!r.isSafe()) {
        // DFA_MATCH or SEMANTIC_MATCH
        // r.getHitContent(), r.getScore()
    }
}
```

### 2) 自定义向量模型（可替换智谱）/ Bring Your Own Embedding Model

实现 `EmbeddingProvider` 并注册为 Bean，即可替换默认智谱实现。  
Implement `EmbeddingProvider` and register it as a Spring bean to replace Zhipu.

```java
@Component
public class MyEmbeddingProvider implements EmbeddingProvider {
    @Override
    public float[] embed(String text) throws IOException {
        // call your own embedding service
        return ...;
    }

    @Override
    public int dimensions() {
        return 1024;
    }
}
```

---

## 原理说明 / How It Works

### 1) DFA 硬匹配 / DFA Exact Match

- 适合“出现就必须拦截”的**强敏感词**（误伤成本低、规则确定）  
  Best for **hard-block** words where any occurrence should be blocked
- 不理解上下文、不分词也能做子串命中，但词库必须谨慎  
  Context-free; can match substrings without tokenization, so keep the DFA list strict

### 2) 向量语义匹配 / Vector Semantic Match

- 将文本映射到向量空间，做近邻检索（pgvector HNSW）  
  Map text into vectors, run nearest-neighbor search (pgvector HNSW)
- 适合检测谐音/近义/变体：例如「赌博≈堵博≈网赌≈博彩」  
  Great for homophones/synonyms/variants depending on embedding quality
- 阈值（`semantic-guard.threshold`）决定“拦截敏感度”：越低越严格、误伤越多  
  Threshold controls strictness: lower → stricter but more false positives

---

## 组件扩展点 / Extension Points

- **EmbeddingProvider**：替换向量模型（智谱/OpenAI/本地模型）  
  Replace embedding model (Zhipu/OpenAI/local)
- **VectorStore**：替换向量存储（pgvector/内存/其他向量数据库）  
  Replace vector store (pgvector/memory/other DB)

---

## License

MIT
