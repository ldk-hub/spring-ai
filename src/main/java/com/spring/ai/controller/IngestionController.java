package com.spring.ai.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
@CrossOrigin(origins = "*")
public class IngestionController {

    private final VectorStore vectorStore;

    public IngestionController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping
    public void ingest(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        List<Document> documents = List.of(new Document(content));
        vectorStore.add(documents);
    }
}
