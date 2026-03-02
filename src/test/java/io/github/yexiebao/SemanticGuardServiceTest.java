package io.github.yexiebao;

import io.github.yexiebao.autoconfigure.SemanticGuardAutoConfiguration;
import io.github.yexiebao.engine.DFAEngine;
import io.github.yexiebao.engine.SemanticGuardService;
import io.github.yexiebao.model.GuardResult;
import io.github.yexiebao.model.SensitiveWordNode;
import io.github.yexiebao.store.VectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootTest(classes = TestApplication.class)
@Import(SemanticGuardAutoConfiguration.class)
public class SemanticGuardServiceTest {

    @Autowired
    private SemanticGuardService semanticGuardService;

    @Autowired
    private DFAEngine dfaEngine;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorStore vectorStore;

    @Test
    public void testSemanticGuard() throws Exception {
        // ===================== 核心：直接读取数据库中已有的 word_vector =====================
        System.out.println("===== 开始从数据库加载敏感词及已有向量 =====");

        // 1. 直接查询 content、category 和 word_vector 三个字段
        List<SensitiveWordNode> sensitiveNodes = jdbcTemplate.query(
                "SELECT id, content, category, word_vector FROM semantic_sensitive_words",
                (rs, rowNum) -> {
                    SensitiveWordNode node = new SensitiveWordNode();
                    node.setContent(rs.getString("content"));
                    node.setCategory(rs.getString("category"));

                    // 2. 读取数据库中的 vector 类型，转成 float[]
                    String vectorStr = rs.getString("word_vector");
                    float[] vector = parseVectorString(vectorStr); // 下面有这个方法
                    node.setVector(vector);

                    return node;
                }
        );
        System.out.println("数据库查询到敏感词数量：" + sensitiveNodes.size());

        // 3. 直接调用 save，把带向量的敏感词存入内存
        vectorStore.save(sensitiveNodes);
        System.out.println("===== 敏感词向量已存入内存，共 " + sensitiveNodes.size() + " 条 =====");

        // ===================== 初始化 DFA 词典 =====================
        List<String> allWords = new ArrayList<>();
        for (SensitiveWordNode node : sensitiveNodes) {
            allWords.add(node.getContent());
        }
        Set<String> wordSet = new HashSet<>(allWords);
        dfaEngine.init(wordSet);
        System.out.println("DFA 词典初始化完成，共 " + wordSet.size() + " 个词");

        // ===================== 执行文本检测 =====================
        String[] samples = new String[]{
                "力工梭哈",
                "力工梭哈定律",
                "中共利用"
        };

        for (String text : samples) {
            System.out.println("\n==================================================");
            System.out.println("待检测文本: " + text);

            GuardResult result = semanticGuardService.check(text);

            System.out.println("检测结果：");
            System.out.println(" - 是否安全(safe): " + result.isSafe());
            System.out.println(" - 匹配类型(matchType): " + result.getMatchType());
            System.out.println(" - 命中内容(hitContent): " + result.getHitContent());
            System.out.println(" - 相似度(score): " + result.getScore());
        }
    }

    // 辅助方法：把 pgvector 的字符串 "[0.1,0.2,...]" 转成 float[]
    private float[] parseVectorString(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) {
            return new float[0];
        }
        // 去掉首尾的 []，按逗号分割
        String[] strs = vectorStr.replace("[", "").replace("]", "").split(",");
        float[] vector = new float[strs.length];
        for (int i = 0; i < strs.length; i++) {
            vector[i] = Float.parseFloat(strs[i].trim());
        }
        return vector;
    }
}