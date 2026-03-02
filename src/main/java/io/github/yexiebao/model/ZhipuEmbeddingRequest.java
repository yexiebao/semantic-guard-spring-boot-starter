package io.github.yexiebao.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智谱 Embedding 请求体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZhipuEmbeddingRequest {
    private String model; // 模型名称
    private String input; // 待向量化的文本
}