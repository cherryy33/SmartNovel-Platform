package com.myproject.novel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.File;

/**
 * AI 相关配置
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(AiRagProperties.class)
public class AiConfig {

    /**
     * 配置自定义 RestClientBuilder：避免 write-numbers-as-strings 把 AI 请求参数
     * 的 Number 类型误转为字符串（如 "temperature": 0.7 => "0.7"）。
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        // 流式响应总时长可达数分钟，读超时需放宽；单块到达间隔远小于该值
        factory.setReadTimeout(180000);
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    /**
     * 内存向量库：基于 EmbeddingModel 自动把文本转为向量，启动时从本地 JSON 恢复。
     */
    @Bean
    public SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel,
        AiRagProperties aiRagProperties) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        File file = new File(aiRagProperties.getVectorStorePath());
        if (file.exists() && file.length() > 0) {
            try {
                vectorStore.load(file);
                log.info("已从 {} 加载向量库", file.getAbsolutePath());
            } catch (Exception e) {
                log.warn("向量库文件 {} 加载失败，将使用空向量库", file.getAbsolutePath(), e);
            }
        }
        return vectorStore;
    }
}
