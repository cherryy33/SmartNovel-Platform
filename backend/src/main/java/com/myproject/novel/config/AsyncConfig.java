package com.myproject.novel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 开启异步能力（RAG 索引任务运行在虚拟线程中）
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
