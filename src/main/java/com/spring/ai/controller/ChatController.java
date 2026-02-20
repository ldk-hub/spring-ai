package com.spring.ai.controller;

import com.spring.ai.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        String chatId = payload.getOrDefault("chatId", "default"); // Default for now, frontend should send it
        String response = ragService.chat(chatId, message);
        return Map.of("response", response);
    }
}
