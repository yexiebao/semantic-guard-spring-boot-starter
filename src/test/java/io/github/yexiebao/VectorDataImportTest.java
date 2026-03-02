package io.github.yexiebao;

import io.github.yexiebao.embedding.EmbeddingProvider;
import io.github.yexiebao.model.SensitiveWordNode;
import io.github.yexiebao.store.VectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = TestApplication.class)
public class VectorDataImportTest {

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Autowired
    private VectorStore vectorStore;

    @Test
    public void runImport() throws Exception {
        /*
         * 这个测试的目标（冒烟测试）
         * 1）遍历你本地词库目录下的所有 txt 文件
         * 2）逐行调用智谱 Embedding 生成向量
         * 3）批量写入本地 PostgreSQL（pgvector）向量表
         * 4）已导入过（内容+分类相同）的敏感词会直接跳过，避免重复导入
         *
         * 说明：你已在本地 PostgreSQL 建好表（sql 已执行），因此这里不再负责建库建表。
         */

        // 1. 不修改你的文件夹路径：保持你原来写死的目录
        String folderPath = "D:\\project\\javaProject\\Sensitive-lexicon\\Vocabulary";
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("文件夹为空或路径错误！folderPath=" + folderPath);
            return;
        }

        // 2. 依次处理目录下的每一个 txt 文件
        for (File file : files) {
            // 文件名（去后缀）作为分类名，如 "色情词库"
            String category = file.getName().replace(".txt", "");
            System.out.println(">>> 开始导入分类: " + category + "，文件: " + file.getAbsolutePath());

            // 读取 txt 所有行并进行导入
            List<String> words = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<SensitiveWordNode> batchList = new ArrayList<>();

            int imported = 0;
            for (String word : words) {
                String w = word == null ? "" : word.trim();
                if (w.isEmpty()) {
                    continue;
                }

                // 2.1 已导入过的词条（按 content + category 判断）直接跳过
                if (vectorStore.existsByContentAndCategory(w, category)) {
                    continue;
                }

                try {
                    // 调用 EmbeddingProvider 转向量（默认智谱，依赖 semantic-guard.zhipu.api-key）
                    float[] vector = embeddingProvider.embed(w);

                    // 构建节点并加入批次
                    batchList.add(new SensitiveWordNode(w, category, vector));
                    imported++;

                    // 每 20 条执行一次批量写入，平衡吞吐与事务压力
                    if (batchList.size() >= 20) {
                        vectorStore.save(batchList);
                        batchList.clear();
                        System.out.println("已存入 20 条，当前分类累计: " + imported);
                        Thread.sleep(200);
                    }
                } catch (Exception e) {
                    System.err.println("处理词条 [" + w + "] 失败: " + e.getMessage());
                }
            }

            if (!batchList.isEmpty()) {
                vectorStore.save(batchList);
            }

            System.out.println("<<< 分类 [" + category + "] 导入完毕！本次导入条数: " + imported);
        }
    }
}