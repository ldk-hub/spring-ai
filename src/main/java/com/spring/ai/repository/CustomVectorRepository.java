package com.spring.ai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * pgvector 확장을 사용하여 L2 / Cosine 거리 기반 (hnsw 인덱스) 벡터 유사도 검색을 처리합니다.
 * 복잡한 JPA 대신 JdbcClient와 Native Query를 사용하여 성능을 극대화합니다.
 */
@Repository
public class CustomVectorRepository {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public CustomVectorRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 애플리케이션 시작 시 pgvector Extension 및 테이블, HNSW 인덱스 스키마 자동 구성
     */
    @PostConstruct
    public void initSchema() {
        // vector 확장 활성화
        jdbcClient.sql("CREATE EXTENSION IF NOT EXISTS vector").update();

        // 문서 및 임베딩 저장을 위한 테이블 (Gemini text-embedding-004의 768차원 기준, 메타데이터 추가)
        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS documents (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                content TEXT,
                metadata JSONB,
                embedding vector(768)
            )
        """).update();

        // Fail-safe: 테이블이 이미 존재했던 경우 metadata 컬럼 추가
        try {
            jdbcClient.sql("ALTER TABLE documents ADD COLUMN IF NOT EXISTS metadata JSONB").update();
        } catch (Exception ignore) { }

        // Cosine distance (vector_cosine_ops) 기반의 hnsw 최적화 인덱스 생성
        jdbcClient.sql("""
            CREATE INDEX IF NOT EXISTS document_embedding_hnsw_idx 
            ON documents USING hnsw (embedding vector_cosine_ops)
            WITH (m = 16, ef_construction = 64)
        """).update();
    }

    /**
     * 문서를 임베딩 데이터 및 출처 메타데이터와 함께 pgvector 테이블에 저장합니다.
     */
    public void saveDocument(String content, Map<String, Object> metadata, float[] embedding) {
        String metadataJson = "{}";
        try {
            if (metadata != null && !metadata.isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(metadata);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        jdbcClient.sql("INSERT INTO documents (content, metadata, embedding) VALUES (:content, :metadata::jsonb, :embedding)")
                .param("content", content)
                .param("metadata", metadataJson)
                .param("embedding", new PGvector(embedding))
                .update();
    }

    /**
     * 질의 벡터와 비교하여 연관성 높은 N개의 문서를 코사인 유사도 기반으로 검색합니다.
     * 검색된 컨텍스트와 함께 출처(metadata)도 리턴합니다.
     */
    public List<DocumentResult> similaritySearch(float[] queryEmbedding, int limit) {
        // <=> 연산자는 pgvector의 코사인 거리 연산자입니다.
        return jdbcClient.sql("""
                SELECT content, metadata 
                FROM documents 
                ORDER BY embedding <=> :query_embedding 
                LIMIT :limit
                """)
                .param("query_embedding", new PGvector(queryEmbedding))
                .param("limit", limit)
                .query((rs, rowNum) -> new DocumentResult(
                        rs.getString("content"),
                        rs.getString("metadata")
                ))
                .list();
    }

    // 결과 매핑용 Record
    public record DocumentResult(String content, String metadataJson) {}
}
