package com.spring.ai.controller;

import com.spring.ai.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // For development simplicity
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        String chatId = payload.getOrDefault("chatId", "default"); // Default for now, frontend should send it
        String response = ragService.chat(chatId, message);
        return Map.of("response", response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    /**
     * API 동작 유무를 확인하는 헬스 체크 엔드포인트입니다.
     */
    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of("status", "UP", "message", "Gemini API Chat Service is running");
    }

    /**
     * 특정 대화 ID의 채팅 이력을 조회합니다.
     */
    @GetMapping("/history")
    public List<Map<String, String>> getHistory(@RequestParam(defaultValue = "default") String chatId) {
        return ragService.getHistory(chatId).stream()
                .map(msg -> Map.of(
                        "type", msg.getMessageType() != null ? msg.getMessageType().getValue() : "unknown",
                        "content", msg.getText() != null ? msg.getText() : ""))
                .collect(Collectors.toList());
    }
}
