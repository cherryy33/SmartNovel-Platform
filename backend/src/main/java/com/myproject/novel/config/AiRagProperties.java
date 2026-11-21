package com.myproject.novel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring AI RAG 知识库配置
 */
@Data
@ConfigurationProperties(prefix = "smart-novel.ai.rag")
public class AiRagProperties {

    /** 向量库（SimpleVectorStore）持久化文件路径 */
    private String vectorStorePath = "./ai-data/vector-store.json";

    /** 向量模型名称，仅用于记录，实际调用以 spring.ai.openai.embedding.options.model 为准 */
    private String embeddingModel = "embedding-3";

    /** 单个分块的最大字符数 */
    private int chunkSize = 500;

    /** 相邻分块之间的重叠字符数 */
    private int chunkOverlap = 100;

    /** 每次问答默认召回的章节片段数量 */
    private int topK = 5;

    /** 相似度阈值，低于该值的片段会被过滤掉 */
    private double similarityThreshold = 0.3;

    /** 拼接进提示词的参考内容最大字符数 */
    private int contextMaxChars = 3000;

    /** 单次索引任务最多处理的章节数 */
    private int maxChaptersPerIndex = 200;
}
