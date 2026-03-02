package io.github.yexiebao;

import io.github.yexiebao.embedding.EmbeddingProvider;
import io.github.yexiebao.engine.DFAEngine;
import io.github.yexiebao.engine.DefaultSemanticGuardService;
import io.github.yexiebao.engine.SemanticGuardService;
import io.github.yexiebao.model.GuardResult;
import io.github.yexiebao.model.SensitiveWordNode;
import io.github.yexiebao.store.InMemoryVectorStore;
import io.github.yexiebao.store.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内存向量库模式下的插件功能测试（无需数据库）
 *
 * <p>使用模拟 EmbeddingProvider，验证可插拔架构与内存存储流程。</p>
 */
public class InMemoryVectorStoreTest {

    private SemanticGuardService semanticGuardService;
    private DFAEngine dfaEngine;
    private VectorStore vectorStore;

    @BeforeEach
    void setup() throws IOException {
        dfaEngine = new DFAEngine();
        Set<String> words = new HashSet<>(Arrays.asList("赌博", "色情", "暴力"));
        dfaEngine.init(words);

        // 模拟 EmbeddingProvider：相同文本返回相同向量，不同文本返回不同向量
        EmbeddingProvider mockEmbedding = new EmbeddingProvider() {
            @Override
            public float[] embed(String text) {
                float[] v = new float[4];
                for (int i = 0; i < Math.min(4, text.length()); i++) {
                    v[i] = text.charAt(i) * 0.01f;
                }
                return v;
            }

            @Override
            public int dimensions() {
                return 4;
            }
        };

        vectorStore = new InMemoryVectorStore();
        vectorStore.save(Collections.singletonList(
                new SensitiveWordNode("赌博", "illegal", mockEmbedding.embed("赌博"))
        ));

        io.github.yexiebao.autoconfigure.GuardProperties props = new io.github.yexiebao.autoconfigure.GuardProperties();
        props.setThreshold(0.8);
        semanticGuardService = new DefaultSemanticGuardService(dfaEngine, mockEmbedding, vectorStore, props);
    }

    @Test
    void testDfaMatch() throws Exception {
        GuardResult r = semanticGuardService.check("这个网站有赌博内容");
        assertFalse(r.isSafe());
        assertEquals("DFA_MATCH", r.getMatchType());
        assertEquals("赌博", r.getHitContent());
    }

    @Test
    void testSafe() throws Exception {
        GuardResult r = semanticGuardService.check("今天天气晴朗");
        assertTrue(r.isSafe());
    }
}
