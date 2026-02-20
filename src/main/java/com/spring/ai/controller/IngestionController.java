package com.spring.ai.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 문서를 벡터 스토어에 등록(인제스트)하는 REST 컨트롤러입니다.
 * POST /api/ingest 로 텍스트를 전달하면 임베딩 후 pgvector에 저장됩니다.
 */
@RestController
@RequestMapping("/api/ingest")
@CrossOrigin(origins = "*")
public class IngestionController {

    private final VectorStore vectorStore;

    public IngestionController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 요청 본문의 content 필드를 벡터 스토어에 저장합니다.
     *
     * @param payload content 키를 포함한 JSON 본문
     * @return 성공 메시지 및 저장된 문자 수 반환
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> ingest(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");

        // 입력값 유효성 검증
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "content 필드가 비어 있습니다."));
        }

        // 문서 생성 후 벡터 스토어에 저장
        List<Document> documents = List.of(new Document(content));
        vectorStore.add(documents);

        return ResponseEntity.ok(Map.of(
                "message", "문서 인제스트 완료",
                "characters", content.length()));
    }
}
