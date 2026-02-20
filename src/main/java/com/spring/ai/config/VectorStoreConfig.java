package com.spring.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 벡터 스토어 설정을 위한 클래스입니다.
 * 외부 DB(pgvector) 대신 인메모리 저장소(SimpleVectorStore)를 사용하여
 * 환경 의존성 없이 RAG 기능을 구동할 수 있게 합니다.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
