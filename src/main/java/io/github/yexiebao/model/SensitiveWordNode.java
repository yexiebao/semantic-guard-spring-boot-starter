package io.github.yexiebao.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感词节点模型（实体类）
 * 用于封装敏感词的核心信息，包含敏感词内容、分类、以及可选的向量特征（用于AI/语义检测场景）
 * 通常配合敏感词库使用，可存储在数据库/缓存中，或用于敏感词的语义相似度分析
 */
@Data // Lombok注解：自动生成getter/setter、toString、equals、hashCode、canEqual方法
@AllArgsConstructor // Lombok注解：自动生成包含所有字段的全参构造方法
@NoArgsConstructor // Lombok注解：自动生成无参构造方法
public class SensitiveWordNode {
    /**
     * 敏感词内容（核心字段）
     * 示例："赌博"、"色情"、"暴力"、"高仿"
     * 对应敏感词库中的具体敏感词文本
     */
    private String content;

    /**
     * 敏感词分类（用于精细化管理）
     * 示例：
     * - "political"：政治敏感词
     * - "pornography"：色情敏感词
     * - "violence"：暴力敏感词
     * - "advertising"：广告违规词
     * - "business"：业务专属敏感词（如电商"假货"、游戏"外挂"）
     * 便于按分类过滤、统计、管理敏感词
     */
    private String category;

    /**
     * 敏感词的向量特征（可选字段，用于AI语义检测）
     * 1. 来源：通过Word2Vec、BERT等NLP模型将敏感词转换为数值向量
     * 2. 用途：
     *    - 检测语义相似的敏感词（如"赌钱"与"赌博"向量相似度高）
     *    - 识别变形/谐音敏感词（如"堵博"→"赌博"）
     * 3. 维度：常见为128/256/768维，float数组存储每个维度的数值
     */
    private float[] vector;
}