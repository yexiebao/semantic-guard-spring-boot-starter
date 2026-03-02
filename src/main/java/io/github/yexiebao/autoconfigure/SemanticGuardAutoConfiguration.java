package io.github.yexiebao.autoconfigure;

import io.github.yexiebao.embedding.EmbeddingProvider;
import io.github.yexiebao.embedding.ZhipuEmbeddingProvider;
import io.github.yexiebao.engine.DFAEngine;
import io.github.yexiebao.engine.DefaultSemanticGuardService;
import io.github.yexiebao.engine.SemanticGuardService;
import io.github.yexiebao.store.InMemoryVectorStore;
import io.github.yexiebao.store.PgVectorStoreAdapter;
import io.github.yexiebao.store.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 语义敏感词检测自动装配
 *
 * <p>默认接入智谱 Embedding，支持内存 / pgvector 两种向量存储。</p>
 * <p>扩展其他向量模型：实现 {@link EmbeddingProvider} 并注册为 Bean 即可。</p>
 */
@Configuration
@EnableConfigurationProperties({GuardProperties.class, ZhipuProperties.class})
@ConditionalOnProperty(prefix = "semantic-guard", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SemanticGuardAutoConfiguration {

    /** 默认 Embedding：智谱（无自定义 Bean 时生效） */
    @Bean
    @ConditionalOnMissingBean(EmbeddingProvider.class)
    public EmbeddingProvider embeddingProvider(ZhipuProperties properties) {
        return new ZhipuEmbeddingProvider(properties);
    }

    /** pgvector 存储（当配置为 pgvector 且存在 JdbcTemplate 时） */
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnProperty(prefix = "semantic-guard", name = "vector-store-type", havingValue = "pgvector", matchIfMissing = true)
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate) {
        return new PgVectorStoreAdapter(jdbcTemplate);
    }

    /** 内存向量存储（当显式配置 vector-store-type=memory 时） */
    @Bean
    @ConditionalOnProperty(prefix = "semantic-guard", name = "vector-store-type", havingValue = "memory")
    public VectorStore inMemoryVectorStore() {
        return new InMemoryVectorStore();
    }

    /** 兜底：无任何 VectorStore 时（如测试未连 DB）使用内存存储，保证 SemanticGuardService 能创建 */
    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore fallbackInMemoryVectorStore() {
        return new InMemoryVectorStore();
    }

    @Bean
    public DFAEngine dfaEngine() {
        return new DFAEngine();
    }

    @Bean
    @ConditionalOnBean(VectorStore.class)
    public SemanticGuardService semanticGuardService(DFAEngine dfaEngine,
                                                      EmbeddingProvider embeddingProvider,
                                                      VectorStore vectorStore,
                                                      GuardProperties guardProperties) {
        return new DefaultSemanticGuardService(dfaEngine, embeddingProvider, vectorStore, guardProperties);
    }
}
