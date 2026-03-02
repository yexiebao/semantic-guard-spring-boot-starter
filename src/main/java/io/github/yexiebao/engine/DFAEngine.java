package io.github.yexiebao.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DFA（Deterministic Finite Automaton，确定性有限自动机）敏感词检测引擎
 * 核心思想：利用 Map 嵌套的方式构建敏感词前缀树（字典树），将敏感词的每个字符作为树的节点
 * 扫描文本时只需遍历一次文本（O(n) 时间复杂度），即可完成敏感词检测，效率远高于暴力匹配
 * 适用场景：文本内容审核、敏感词过滤等场景
 *
 * <p>注意：本类不再标记为 Spring 组件，由自动装配模块通过 {@code @Bean} 的方式创建，</p>
 * <p>这样可以避免与业务方应用中的同名 Bean 产生冲突，更符合 Spring Boot Starter 的设计规范。</p>
 */
public class DFAEngine {

    /**
     * 敏感词前缀树的根节点
     *
     * <p>结构说明：</p>
     * <ul>
     *     <li>key：字符</li>
     *     <li>value：子节点 Map 或结束标识（通过特殊 key {@code (char)0} 标记）</li>
     * </ul>
     */
    private Map<Character, Object> wordMap = new HashMap<>();

    // 原代码未使用该常量，保留仅作注释参考：用于标记敏感词结尾的标识key
    private static final String IS_END = "isEnd";

    /**
     * 初始化敏感词前缀树（核心方法）
     * 将敏感词集合构建成 DFA 算法所需的前缀树结构，为后续检测做准备
     *
     * @param sensitiveWords 敏感词集合（通常从数据库/配置文件加载，如 {"赌博", "色情", "暴力"}）
     */
    public void init(Set<String> sensitiveWords) {
        // 新建一个空的前缀树根节点，避免直接修改原有 wordMap 导致线程安全问题
        Map<Character, Object> newMap = new HashMap<>();

        // 遍历每一个敏感词，逐个构建前缀树节点
        for (String word : sensitiveWords) {
            // 临时变量，指向当前遍历到的节点（初始为根节点）
            Map<Character, Object> nowMap = newMap;

            // 遍历敏感词的每一个字符
            for (int i = 0; i < word.length(); i++) {
                // 获取当前位置的字符（作为树的节点key）
                char key = word.charAt(i);

                // 尝试从当前节点获取该字符对应的子节点
                Map<Character, Object> subMap = (Map<Character, Object>) nowMap.get(key);

                if (subMap != null) {
                    // 该字符节点已存在，将当前节点指针移动到该子节点
                    nowMap = subMap;
                } else {
                    // 该字符节点不存在，新建子节点 Map
                    Map<Character, Object> nextMap = new HashMap<>();
                    // 用特殊字符(char)0作为key，值"0"标记该节点不是敏感词的结尾
                    nextMap.put((char) 0, "0");
                    // 将新节点添加到当前节点的子节点中
                    nowMap.put(key, nextMap);
                    // 将当前节点指针移动到新建的子节点
                    nowMap = nextMap;
                }

                // 如果遍历到敏感词的最后一个字符
                if (i == word.length() - 1) {
                    // 将特殊字符(char)0的值改为"1"，标记该节点是敏感词的结尾
                    nowMap.put((char) 0, "1");
                }
            }
        }
        // 将构建好的前缀树赋值给全局变量，完成初始化
        this.wordMap = newMap;
    }

    /**
     * 检测文本中是否包含敏感词（对外提供的核心检测方法）
     *
     * @param text 待检测的文本内容（如："该网站包含赌博相关内容"）
     * @return true=文本包含敏感词，false=文本不包含敏感词
     */
    public boolean contains(String text) {
        // 空文本直接返回不包含
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 遍历文本的每一个字符，作为敏感词检测的起始位置
        for (int i = 0; i < text.length(); i++) {
            // 从第i个字符开始检测是否匹配敏感词
            int matchType = checkSensitiveWord(text, i);
            // matchType>0 表示匹配到了敏感词（返回值为匹配到的敏感词长度）
            if (matchType > 0) {
                return true;
            }
        }
        // 遍历完所有字符都未匹配到敏感词
        return false;
    }

    /**
     * 返回文本中命中的第一个敏感词（用于上层构建详细检测结果）
     *
     * @param text 待检测文本
     * @return 首个命中的敏感词内容；若未命中则返回 {@code null}
     */
    public String findFirst(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 与 contains 相同，从每一个字符作为起点尝试匹配
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                // 根据返回的匹配长度截取出敏感词内容
                return text.substring(i, i + length);
            }
        }
        return null;
    }

    /**
     * 核心检测方法：从指定位置开始，检测文本是否匹配敏感词
     *
     * @param text       待检测文本
     * @param beginIndex 检测起始位置（文本的字符下标）
     * @return 匹配到的敏感词长度（0=未匹配，>0=匹配到的敏感词长度）
     */
    private int checkSensitiveWord(String text, int beginIndex) {
        // 指向前缀树的当前节点（初始为根节点）
        Map<Character, Object> nowMap = wordMap;
        // 匹配到的敏感词长度（初始为0）
        int matchFlag = 0;

        // 从起始位置开始，逐个字符遍历文本
        for (int i = beginIndex; i < text.length(); i++) {
            char key = text.charAt(i);
            // 从当前节点获取该字符对应的子节点
            nowMap = (Map<Character, Object>) nowMap.get(key);

            if (nowMap != null) {
                // 字符匹配成功，匹配长度+1
                matchFlag++;
                // 检查该节点是否是敏感词的结尾（特殊字符(char)0的值为"1"）
                if ("1".equals(nowMap.get((char) 0))) {
                    // 匹配到完整敏感词，返回匹配长度
                    return matchFlag;
                }
            } else {
                // 字符不匹配，终止当前检测（DFA 算法核心：前缀不匹配则无需继续）
                break;
            }
        }
        // 未匹配到完整敏感词，返回0
        return 0;
    }
}