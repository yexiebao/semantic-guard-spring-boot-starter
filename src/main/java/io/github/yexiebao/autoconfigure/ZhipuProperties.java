package io.github.yexiebao.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智谱 AI 向量模型配置类
 */
@Data
@ConfigurationProperties(prefix = "semantic-guard.zhipu")
public class ZhipuProperties {

    /** 智谱 AI API Key  */
    private String apiKey = "Your_key";

    /** 智谱 AI API 基础地址 (V4 接口) */
    private String baseUrl = "https://open.bigmodel.cn/api/paas";

    /** 嵌入模型名称，embedding-2 固定维度为 1024 */
    private String modelName = "embedding-2";

    /** 向量维度：embedding-2 返回 1024 维 */
    private Integer dimensions = 1024;
}