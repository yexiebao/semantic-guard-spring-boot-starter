package io.github.yexiebao.engine;

import io.github.yexiebao.model.GuardResult;

import java.io.IOException;

/**
 * 语义敏感词检测统一门面接口
 *
 * <p>对外暴露的核心能力只有两类：</p>
 * <ul>
 *     <li>基于 DFA 的「硬匹配」敏感词检测</li>
 *     <li>基于向量检索（pgvector + 智谱 Embedding）的「语义匹配」敏感词检测</li>
 * </ul>
 *
 * <p>业务系统只需要注入本接口即可完成敏感词检测，无需关心内部实现细节。</p>
 */
public interface SemanticGuardService {

    /**
     * 使用默认的配置阈值进行敏感词检测
     *
     * @param text 待检测文本（可以是标题、评论、聊天内容等）
     * @return 检测结果，包含是否安全、命中类型、命中内容、相似度（如有）
     * @throws IOException 当调用外部向量化服务（如智谱 API）失败时抛出
     */
    GuardResult check(String text) throws IOException;

    /**
     * 使用调用方自定义的相似度阈值进行敏感词检测
     *
     * <p>典型场景：同一个系统中，不同业务线对「语义相似度」的容忍度不同，</p>
     * <p>例如评论区可以设置为 0.8，而私信场景可以设置为 0.9，更为严格。</p>
     *
     * @param text      待检测文本
     * @param threshold 自定义语义相似度阈值（0.0 ~ 1.0，建议 0.8 以上）
     * @return 检测结果
     * @throws IOException 当调用外部向量化服务（如智谱 API）失败时抛出
     */
    GuardResult check(String text, double threshold) throws IOException;
}


