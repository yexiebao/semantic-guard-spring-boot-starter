package io.github.yexiebao.embedding;

import com.google.gson.Gson;
import io.github.yexiebao.autoconfigure.ZhipuProperties;
import io.github.yexiebao.model.ZhipuEmbeddingRequest;
import io.github.yexiebao.model.ZhipuEmbeddingResponse;
import okhttp3.*;

import java.io.IOException;

/**
 * 智谱 AI Embedding 实现（默认向量模型）
 *
 * <p>实现 {@link EmbeddingProvider} 接口，调用智谱 embedding-2 接口将文本转为 1024 维向量。</p>
 * <p>未来扩展其他模型时，只需新增实现类并配置切换，无需重构。</p>
 */
public class ZhipuEmbeddingProvider implements EmbeddingProvider {

    private final ZhipuProperties properties;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public ZhipuEmbeddingProvider(ZhipuProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    @Override
    public float[] embed(String text) throws IOException {
        ZhipuEmbeddingRequest req = new ZhipuEmbeddingRequest(properties.getModelName(), text);
        String json = gson.toJson(req);

        Request httpRequest = new Request.Builder()
                .url(properties.getBaseUrl() + "/v4/embeddings")
                .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("智谱向量接口调用失败，状态码: " + response.code());
            }
            ResponseBody body = response.body();
            String bodyStr = (body != null) ? body.string() : "";
            ZhipuEmbeddingResponse resp = gson.fromJson(bodyStr, ZhipuEmbeddingResponse.class);
            if (resp == null || resp.getData() == null || resp.getData().length == 0) {
                throw new IOException("智谱返回数据为空");
            }
            double[] d = resp.getData()[0].getEmbedding();
            float[] f = new float[d.length];
            for (int i = 0; i < d.length; i++) {
                f[i] = (float) d[i];
            }
            return f;
        }
    }

    @Override
    public int dimensions() {
        return properties.getDimensions() != null ? properties.getDimensions() : 1024;
    }
}
