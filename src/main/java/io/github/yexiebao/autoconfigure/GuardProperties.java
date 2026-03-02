package io.github.yexiebao.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 语义检测配置类
 */
@Data
@ConfigurationProperties(prefix = "semantic-guard")
public class GuardProperties {
    /** 是否开启语义检测 */
    private boolean enabled = true;
    /** 相似度阈值 (0.0 - 1.0) */
    private double threshold = 0.85;
    /** 向量存储类型：memory（内存）| pgvector（PostgreSQL） */
    private String vectorStoreType = "pgvector";
}