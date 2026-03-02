package io.github.yexiebao.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智谱 Embedding 响应体
 */
@Data
public class ZhipuEmbeddingResponse {
    private EmbeddingData[] data;

    @Data
    public static class EmbeddingData {
        private double[] embedding; // 接口返回的是 double 数组
        private int index;
    }
}