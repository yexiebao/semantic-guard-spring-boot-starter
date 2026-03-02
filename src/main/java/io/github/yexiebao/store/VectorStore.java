package io.github.yexiebao.store;

import io.github.yexiebao.model.SemanticMatchResult;
import io.github.yexiebao.model.SensitiveWordNode;

import java.util.List;

/**
 * 向量存储与检索抽象接口（可插拔）
 *
 * <p>支持内存、pgvector 等任意实现，不绑定具体存储介质。</p>
 * <p>扩展方式：实现本接口并注册为 Spring Bean，即可替换默认实现。</p>
 */
public interface VectorStore {

    /**
     * 批量保存敏感词节点（含内容、分类、向量）
     *
     * @param nodes 敏感词节点列表
     */
    void save(List<SensitiveWordNode> nodes);

    /**
     * 语义相似度 TOP1 检索
     *
     * @param queryVector 待检测文本的向量
     * @return 最相似的一条记录；无数据时返回 null
     */
    SemanticMatchResult queryTopMatch(float[] queryVector);

    /**
     * 判断某条敏感词（content + category）是否已存在
     *
     * @param content  敏感词内容
     * @param category 分类
     * @return true 表示已存在
     */
    boolean existsByContentAndCategory(String content, String category);
}
