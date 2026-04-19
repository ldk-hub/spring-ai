package com.spring.ai.controller;

import com.spring.ai.repository.CustomVectorRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 문서를 벡터 스토어에 등록(인제스트)하는 REST 컨트롤러입니다.
 * POST /api/ingest : 텍스트
 * POST /api/ingest/file : 파일 (PDF/TXT)
 */
@RestController
@RequestMapping("/api/ingest")
@CrossOrigin(origins = "*")
public class IngestionController {

    private final CustomVectorRepository vectorRepository;
    private final EmbeddingModel embeddingModel;

    public IngestionController(CustomVectorRepository vectorRepository, EmbeddingModel embeddingModel) {
        this.vectorRepository = vectorRepository;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 단순 텍스트 입력의 인제스트
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> ingestText(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "content 필드가 비어 있습니다."));
        }

        processAndSaveDocument(content, "Raw Text Input");

        return ResponseEntity.ok(Map.of(
                "message", "텍스트 인제스트 완료 (청키분할 적용)",
                "characters", content.length()));
    }

    /**
     * 파일(PDF, TXT) 업로드를 통한 인제스트
     */
    @PostMapping("/file")
    public ResponseEntity<Map<String, Object>> ingestFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일이 첨부되지 않았습니다."));
        }

        try {
            String filename = file.getOriginalFilename();
            String content;
            
            // PDF 파일일 경우 Spring AI PDF Reader 활용
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    new org.springframework.core.io.InputStreamResource(file.getInputStream()),
                    PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopTextLinesToDelete(0)
                            .build())
                        .build());
                
                List<Document> documents = pdfReader.get();
                StringBuilder sb = new StringBuilder();
                for (Document doc : documents) {
                    sb.append(doc.getText()).append("\n"); // Document.getText() returns content in string
                }
                content = sb.toString();
            } else {
                // 그 외 범용 파일은 바이트 데이터를 문자열로 처리
                content = new String(file.getBytes());
            }

            processAndSaveDocument(content, filename);

            return ResponseEntity.ok(Map.of(
                    "message", "파일 인제스트 완료",
                    "filename", filename,
                    "characters", content.length()));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 처리 중 오류가 발생했습니다. : " + e.getMessage()));
        }
    }

    /**
     * 긴 문서를 청킹(Chunking)하여 메타데이터와 함께 개별 벡터를 DB에 저장합니다.
     */
    private void processAndSaveDocument(String content, String sourceName) {
        // 긴 문서를 문맥이 유지되는 적절한 길이(토큰 단위)로 쪼개기 위한 TokenTextSplitter
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(List.of(new Document(content)));

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String chunkContent = chunk.getText(); // Document.getText() returns the extracted string.
            
            // 징크별 텍스트를 임베딩 모델로 벡터(float[]) 변환
            float[] embedding = embeddingModel.embed(chunkContent);

            // 메타데이터(출처 및 파티션 정보) 구성
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", sourceName);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());

            // Custom DB에 저장
            vectorRepository.saveDocument(chunkContent, metadata, embedding);
        }
    }
}
