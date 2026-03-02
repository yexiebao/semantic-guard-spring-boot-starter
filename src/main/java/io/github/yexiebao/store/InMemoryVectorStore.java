package io.github.yexiebao.store;

import io.github.yexiebao.model.SemanticMatchResult;
import io.github.yexiebao.model.SensitiveWordNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存向量库实现（适合 Demo、小规模词库）
 *
 * <p>不持久化向量，应用重启后需重新加载。使用暴力余弦相似度检索。</p>
 * <p>适用场景：快速验证、无 DB 环境、词量较小（如 &lt; 10 万）。</p>
 */
public class InMemoryVectorStore implements VectorStore {

    private final List<SensitiveWordNode> entries = new CopyOnWriteArrayList<>();

    @Override
    public void save(List<SensitiveWordNode> nodes) {
        if (nodes != null && !nodes.isEmpty()) {
            entries.addAll(nodes);
        }
    }

    @Override
    public SemanticMatchResult queryTopMatch(float[] queryVector) {
        // ===================== 第一步：打印入参和前置校验 =====================
        System.out.println("===== 开始执行向量语义匹配 =====");
        // 打印待检测向量基本信息（只打印维度，避免1024维数值刷屏）
        System.out.println("待检测向量：是否为空=" + (queryVector == null) +
                "，维度=" + (queryVector == null ? 0 : queryVector.length));
        System.out.println("内存中敏感词数量：" + (entries == null ? 0 : entries.size()));

        // 前置校验
        if (queryVector == null || queryVector.length == 0 || entries.isEmpty()) {
            System.out.println("【校验失败】：待检测向量为空 或 无敏感词，返回null");
            return null;
        }

        // ===================== 第二步：初始化最优匹配变量 =====================
        double bestScore = -1;
        SensitiveWordNode best = null;
        System.out.println("初始化：最高相似度=" + bestScore + "，命中节点=null");

        // ===================== 第三步：遍历所有敏感词向量 =====================
        int count = 0;
        for (SensitiveWordNode node : entries) {
            count++;
            float[] v = node.getVector();
            String word = node.getContent();

            System.out.println("\n----- 处理第" + count + "个敏感词 -----");
            System.out.println("敏感词内容：" + word);
            System.out.println("敏感词向量：是否为空=" + (v == null) +
                    "，维度=" + (v == null ? 0 : v.length));

            // 跳过无效向量
            if (v == null || v.length != queryVector.length) {
                System.out.println("【跳过】：向量为空 或 维度不一致（待检测维度=" + queryVector.length + "）");
                continue;
            }

            // 计算余弦相似度
            System.out.println("开始计算余弦相似度...");
            double score = cosineSimilarity(queryVector, v);
            System.out.println("相似度计算结果：" + score);

            // 更新最优匹配
            if (score > bestScore) {
                System.out.println("【更新最优】：原最高=" + bestScore + " → 新最高=" + score + "，命中词：" + word);
                bestScore = score;
                best = node;
            } else {
                System.out.println("【不更新】：当前相似度" + score + " ≤ 最高相似度" + bestScore);
            }
        }

        // ===================== 第四步：封装返回结果 =====================
        System.out.println("\n===== 遍历完成 =====");
        System.out.println("最终最高相似度：" + bestScore);
        System.out.println("最终命中敏感词：" + (best == null ? "无" : best.getContent()));

        if (best == null) {
            System.out.println("【结果】：无匹配敏感词，返回null");
            return null;
        }

        SemanticMatchResult r = new SemanticMatchResult();
        r.setContent(best.getContent());
        r.setCategory(best.getCategory());
        r.setScore(bestScore);

        System.out.println("【结果】：封装匹配结果 → 内容=" + r.getContent() +
                "，分类=" + r.getCategory() +
                "，相似度=" + r.getScore());
        System.out.println("===== 向量语义匹配结束 =====");

        return r;
    }
    @Override
    public boolean existsByContentAndCategory(String content, String category) {
        for (SensitiveWordNode n : entries) {
            if (content.equals(n.getContent()) && (category == null ? n.getCategory() == null : category.equals(n.getCategory()))) {
                return true;
            }
        }
        return false;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom <= 0 ? 0 : dot / denom;
    }
}
