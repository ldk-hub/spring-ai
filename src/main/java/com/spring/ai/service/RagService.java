package com.spring.ai.service;

import com.spring.ai.repository.CustomVectorRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gemini 기반의 RAG 채팅 서비스입니다.
 */
@Service
public class RagService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final CustomVectorRepository vectorRepository;
    private final EmbeddingModel embeddingModel;

    // AI 페르소나 및 RAG 역할을 정의하는 System Prompt
    private static final String AI_PERSONA_PROMPT = """
            당신은 데이터를 정확하고 논리적으로 분석하여 사용자에게 제공하는 
            수석 전문 기술 어시스턴트(Senior Technical Assistant)입니다.
            
            반드시 아래에 제공된 [참고 문서(Context)]의 내용만을 기반으로 답변을 작성하세요.
            문서에서 찾을 수 없는 내용은 유추하지 말고 "해당 내용은 준비된 문서에서 찾을 수 없습니다."라고만 답변하며 절대 지어내지 마세요.
            답변은 명확하고 전문적인 한국어로 작성하며, 필요 시 요약이나 불릿 포인트 포맷을 활용하세요.
            
            [참고 문서(Context)]
            {context_information}
            """;

    public RagService(ChatClient.Builder builder, 
                      CustomVectorRepository vectorRepository, 
                      EmbeddingModel embeddingModel) {
        
        this.vectorRepository = vectorRepository;
        this.embeddingModel = embeddingModel;

        this.chatMemory = new ChatMemory() {
            private final int MAX_ENTRIES = 100;
            private final java.util.Map<String, List<Message>> lruCache = java.util.Collections
                    .synchronizedMap(new java.util.LinkedHashMap<String, List<Message>>(16, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<String, List<Message>> eldest) {
                            return size() > MAX_ENTRIES;
                        }
                    });

            @Override
            public void add(String conversationId, List<Message> messages) {
                lruCache.compute(conversationId, (k, existing) -> {
                    if (existing == null) {
                        return new java.util.ArrayList<>(messages);
                    } else {
                        existing.addAll(messages);
                        return existing;
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
                .defaultAdvisors(new MessageChatMemoryAdvisor(this.chatMemory))
                .build();
    }

    /**
     * 사용자 메시지를 임베딩하여 연관 컨텍스트를 검색하고, Gemini에 전달하여 RAG 응답을 생성합니다.
     */
    public String chat(String chatId, String message) {
        // 1. 사용자 질문을 임베딩 모델로 벡터화 (텍스트 -> float 배열)
        float[] queryEmbedding = embeddingModel.embed(message);
        
        // 2. pgvector(JdbcClient)를 통해 코사인 유사도 기반 문서 검색 (가장 연관된 N개 추출)
        List<CustomVectorRepository.DocumentResult> similarDocs = vectorRepository.similaritySearch(queryEmbedding, 3);
        
        StringBuilder contextBuilder = new StringBuilder();
        for (CustomVectorRepository.DocumentResult doc : similarDocs) {
            String source = "Unknown";
            if (doc.metadataJson() != null && doc.metadataJson().contains("\"source\"")) {
                try {
                    source = doc.metadataJson().split("\"source\":\"")[1].split("\"")[0];
                } catch (Exception ignore) {}
            }
            contextBuilder.append("[출처: ").append(source).append("]\n")
                          .append(doc.content()).append("\n\n---\n\n");
        }
        String contextInfo = contextBuilder.toString();
        
        // 3. AI 페르소나 시스템 프롬프트에 검색된 컨텍스트 매핑
        String systemPrompt = AI_PERSONA_PROMPT.replace("{context_information}", contextInfo);

        // 4. Gemini 호환 생성 API에 질문과 시스템 프롬프트를 함께 주입하여 RAG 응답을 호출
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(a -> a.param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }

    public List<Message> getHistory(String chatId) {
        return this.chatMemory.get(chatId, 100);
    }
}
