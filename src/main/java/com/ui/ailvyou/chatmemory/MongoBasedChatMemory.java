package com.ui.ailvyou.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MongoBasedChatMemory implements ChatMemory {

    private final MongoTemplate mongoTemplate;

    public MongoBasedChatMemory(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void add(String conversationId, Message message) {
        ChatMessageDocument document = new ChatMessageDocument();
        document.setConversationId(conversationId);
        document.setRole(message.getMessageType().getValue());
        document.setContent(message.getText());
        document.setMetadata(message.getMetadata());
        document.setCreatedAt(LocalDateTime.now());

        mongoTemplate.save(document, "chat_memory");
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<ChatMessageDocument> documents = messages.stream()
                .map(message -> {
                    ChatMessageDocument document = new ChatMessageDocument();
                    document.setConversationId(conversationId);
                    document.setRole(message.getMessageType().getValue());
                    document.setContent(message.getText());
                    document.setMetadata(message.getMetadata());
                    document.setCreatedAt(LocalDateTime.now());
                    return document;
                })
                .collect(Collectors.toList());

        mongoTemplate.insert(documents, "chat_memory");
    }

    @Override
    public List<Message> get(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        query.with(org.springframework.data.domain.Sort.by("createdAt").ascending());

        List<ChatMessageDocument> documents = mongoTemplate.find(
                query,
                ChatMessageDocument.class,
                "chat_memory"
        );

        return documents.stream()
                .map(doc -> new Message() {
                    @Override
                    public String getText() {
                        return doc.getContent();
                    }

                    @Override
                    public MessageType getMessageType() {
                        return MessageType.fromValue(doc.getRole());
                    }

                    @Override
                    public Map<String, Object> getMetadata() {
                        return doc.getMetadata();
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, "chat_memory");
    }
}
