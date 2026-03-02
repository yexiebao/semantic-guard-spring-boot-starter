package io.github.yexiebao.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语义相似度检索的单条命中结果
 *
 * <p>封装从 pgvector 向量表中查出的最相似敏感词信息：</p>
 * <ul>
 *     <li>content：命中的敏感词文本</li>
 *     <li>category：敏感词所属分类</li>
 *     <li>score：与待检测文本的相似度（0.0 ~ 1.0，越大越相似）</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SemanticMatchResult {

    /**
     * 命中的敏感词内容
     */
    private String content;

    /**
     * 敏感词分类（如：political、pornography 等）
     */
    private String category;

    /**
     * 语义相似度分值（0.0 ~ 1.0，值越大越相似）
     */
    private Double score;
}


