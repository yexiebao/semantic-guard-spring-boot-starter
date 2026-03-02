package io.github.yexiebao.embedding;

import java.io.IOException;

/**
 * 文本向量化提供者抽象接口（可插拔、跨模型）
 *
 * <p>将任意文本转换为固定维度的向量表示，用于语义相似度检索。</p>
 * <p>不绑定任何厂商模型，支持智谱、OpenAI、本地模型等任意实现切换。</p>
 *
 * <p>扩展方式：实现本接口并注册为 Spring Bean，即可替换默认的智谱实现。</p>
 */
public interface EmbeddingProvider {

    /**
     * 将文本转换为向量
     *
     * @param text 待向量化的文本
     * @return 向量数组（维度由具体模型决定，如智谱 embedding-2 为 1024 维）
     * @throws IOException 当调用外部 API 或模型推理失败时抛出
     */
    float[] embed(String text) throws IOException;

    /**
     * 返回本提供者产出的向量维度（用于校验与向量库兼容性）
     *
     * @return 向量维度，如 1024
     */
    int dimensions();
}
