package com.spring.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gemini 채팅 서비스입니다.
 * <p>
 * QuestionAnswerAdvisor(RAG)는 Gemini OpenAI 호환 임베딩 API가
 * v1main 버전에서 text-embedding-004를 지원하지 않아 제거하였습니다.
 * 대신 InMemoryChatMemory를 통한 대화 컨텍스트 유지 기능만 활성화됩니다.
 * </p>
 */
@Service
public class RagService {

    // 채팅 클라이언트 (대화 메모리 어드바이저 포함)
    private final ChatClient chatClient;
    // 대화 이력 저장을 위한 메모리
    private final ChatMemory chatMemory;

    public RagService(ChatClient.Builder builder) {
        // 커스텀 LRU 축출 규칙을 적용한 ChatMemory 구현체를 익명 클래스로 만들어서 메모리 누수를 방지합니다.
        // 최대 100개의 과거 대화(ChatId) 세션만 유지합니다. (LRU Cache 방식)
        this.chatMemory = new ChatMemory() {
            private final int MAX_ENTRIES = 100;
            // LRU 동작을 위해 accessOrder = true인 LinkedHashMap 사용
            private final java.util.Map<String, List<Message>> lruCache = java.util.Collections
                    .synchronizedMap(new java.util.LinkedHashMap<String, List<Message>>(16, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<String, List<Message>> eldest) {
                            return size() > MAX_ENTRIES;
                        }
                    });

            @Override
            public void add(String conversationId, List<Message> messages) {
                lruCache.compute(conversationId, (k, existingMessages) -> {
                    if (existingMessages == null) {
                        return new java.util.ArrayList<>(messages);
                    } else {
                        existingMessages.addAll(messages);
                        return existingMessages;
                    }
                });
            }

            @Override
            public void add(String conversationId, Message message) {
                add(conversationId, List.of(message));
            }

            @Override
            public List<Message> get(String conversationId, int lastN) {
                List<Message> messages = lruCache.get(conversationId);
                if (messages == null || messages.isEmpty()) {
                    return List.of();
                }
                int fromIndex = Math.max(0, messages.size() - lastN);
                return new java.util.ArrayList<>(messages.subList(fromIndex, messages.size()));
            }

            @Override
            public void clear(String conversationId) {
                lruCache.remove(conversationId);
            }
        };

        this.chatClient = builder
                // 대화 메모리 어드바이저: 이전 대화 맥락을 유지합니다
                .defaultAdvisors(new MessageChatMemoryAdvisor(this.chatMemory))
                .build();
    }

    /**
     * 사용자 메시지를 Gemini에 전달하고 응답을 반환합니다.
     *
     * @param chatId  대화 세션 식별자
     * @param message 사용자 입력 메시지
     * @return Gemini의 응답 텍스트
     */
    public String chat(String chatId, String message) {
        return chatClient.prompt()
                .user(message)
                // 대화 ID를 어드바이저에 전달하여 세션별 메모리를 분리합니다
                .advisors(a -> a.param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }

    /**
     * 특정 대화 ID의 전체 채팅 이력을 조회합니다.
     *
     * @param chatId 대화 식별자
     * @return 대화 이력 메시지 리스트
     */
    public List<Message> getHistory(String chatId) {
        // 최근 100개의 대화 이력을 가져옵니다
        return this.chatMemory.get(chatId, 100);
    }
}
