package io.github.yexiebao.engine;

import io.github.yexiebao.autoconfigure.GuardProperties;
import io.github.yexiebao.embedding.EmbeddingProvider;
import io.github.yexiebao.model.GuardResult;
import io.github.yexiebao.model.SemanticMatchResult;
import io.github.yexiebao.store.VectorStore;

import java.io.IOException;
import java.util.Arrays;

/**
 * 语义敏感词检测默认实现
 *
 * <p>内部整合三大能力：</p>
 * <ul>
 *     <li>{@link DFAEngine}：基于 DFA 的硬匹配敏感词检测</li>
 *     <li>{@link EmbeddingProvider}：可插拔向量化（默认智谱）</li>
 *     <li>{@link VectorStore}：可插拔向量存储（内存 / pgvector）</li>
 * </ul>
 *
 * <p>检测流程：</p>
 * <ol>
 *     <li>优先进行 DFA 硬匹配，命中则直接拦截，标记为 "DFA_MATCH"</li>
 *     <li>若 DFA 未命中，再进行语义匹配；当相似度 >= 阈值 时，标记为 "SEMANTIC_MATCH"</li>
 *     <li>两者均未命中，则认为文本安全</li>
 * </ol>
 */
public class DefaultSemanticGuardService implements SemanticGuardService {

    private final DFAEngine dfaEngine;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final GuardProperties guardProperties;

    public DefaultSemanticGuardService(DFAEngine dfaEngine,
                                       EmbeddingProvider embeddingProvider,
                                       VectorStore vectorStore,
                                       GuardProperties guardProperties) {
        this.dfaEngine = dfaEngine;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.guardProperties = guardProperties;
    }

    @Override
    public GuardResult check(String text) throws IOException {
        // 使用配置文件中的默认阈值
        return check(text, guardProperties.getThreshold());
    }

    @Override
    public GuardResult check(String text, double threshold) throws IOException {
        // 1. 空文本直接判定为安全，避免不必要的外部调用
        if (text == null || text.trim().isEmpty()) {
            return GuardResult.builder()
                    .safe(true)
                    .matchType(null)
                    .hitContent(null)
                    .score(null)
                    .build();
        }

        // 2. 先进行 DFA 硬匹配（O(n) 扫描），命中即直接拦截
//        String dfaHit = dfaEngine.findFirst(text);
//        if (dfaHit != null) {
//            return GuardResult.builder()
//                    .safe(false)
//                    .matchType("DFA_MATCH")
//                    .hitContent(dfaHit)
//                    .score(null)
//                    .build();
//        }

        // 3. 再进行语义匹配：文本 -> 向量 -> 向量库相似度检索
        // 向量化
        float[] queryVector = embeddingProvider.embed(text);
        System.out.println("向量具体值：" + Arrays.toString(queryVector));
        SemanticMatchResult matchResult = vectorStore.queryTopMatch(queryVector);
        System.out.println("检测结果："+matchResult);
        // 数据库中没有任何向量数据，直接视为安全（调用方可通过独立监控告警）
        if (matchResult == null || matchResult.getScore() == null) {
            return GuardResult.builder()
                    .safe(true)
                    .matchType(null)
                    .hitContent(null)
                    .score(null)
                    .build();
        }

        // 4. 根据相似度与阈值判断是否命中语义敏感词
        if (matchResult.getScore() >= threshold) {
            return GuardResult.builder()
                    .safe(false)
                    .matchType("SEMANTIC_MATCH")
                    .hitContent(matchResult.getContent())
                    .score(matchResult.getScore())
                    .build();
        }

        // 5. 相似度低于阈值，认为当前文本语义安全
        return GuardResult.builder()
                .safe(true)
                .matchType(null)
                .hitContent(null)
                .score(matchResult.getScore())
                .build();
    }
}


