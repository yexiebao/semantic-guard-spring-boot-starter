package io.github.yexiebao.store;

import io.github.yexiebao.model.SemanticMatchResult;
import io.github.yexiebao.model.SensitiveWordNode;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 基于 pgvector 的向量存储实现（生产级、可持久化）
 *
 * <p>实现 {@link VectorStore} 接口，将敏感词向量存入 PostgreSQL pgvector 扩展表。</p>
 * <p>适用场景：大规模词库、需要持久化、已有 PostgreSQL 环境。</p>
 */
public class PgVectorStoreAdapter implements VectorStore {

    private final JdbcTemplate jdbcTemplate;

    public PgVectorStoreAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(List<SensitiveWordNode> nodes) {
        String sql = "INSERT INTO semantic_sensitive_words (content, category, word_vector) VALUES (?, ?, ?::vector)";
        jdbcTemplate.batchUpdate(sql, nodes, 100, (ps, n) -> {
            ps.setString(1, n.getContent());
            ps.setString(2, n.getCategory());
            ps.setString(3, vectorToSql(n.getVector()));
        });
    }

    @Override
    public SemanticMatchResult queryTopMatch(float[] queryVector) {
        String vec = vectorToSql(queryVector);
        String sql = "SELECT content, category, 1 - (word_vector <=> ?::vector) AS score " +
                "FROM semantic_sensitive_words ORDER BY word_vector <=> ?::vector LIMIT 1";
        List<SemanticMatchResult> list = jdbcTemplate.query(sql, new Object[]{vec, vec}, (rs, rn) -> {
            SemanticMatchResult r = new SemanticMatchResult();
            r.setContent(rs.getString("content"));
            r.setCategory(rs.getString("category"));
            r.setScore(rs.getDouble("score"));
            return r;
        });
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public boolean existsByContentAndCategory(String content, String category) {
        String sql = "SELECT 1 FROM semantic_sensitive_words WHERE content = ? AND category = ? LIMIT 1";
        List<Integer> list = jdbcTemplate.query(sql, new Object[]{content, category}, (rs, rn) -> rs.getInt(1));
        return !list.isEmpty();
    }

    private static String vectorToSql(float[] v) {
        if (v == null || v.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(v[i]);
            if (i < v.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}
