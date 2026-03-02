package io.github.yexiebao.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GuardResult {
    /** 是否通过检测 */
    private boolean safe;
    /** 拦截类型：DFA_MATCH (硬匹配) 或 SEMANTIC_MATCH (语义匹配) */
    private String matchType;
    /** 命中的敏感词或语义片段 */
    private String hitContent;
    /** 相似度分值（仅语义匹配有值） */
    private Double score;
}