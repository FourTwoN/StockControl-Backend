package com.fortytwo.demeter.chatbot.repository;

import com.fortytwo.demeter.chatbot.model.ChatMessage;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ChatMessageRepository implements PanacheRepositoryBase<ChatMessage, UUID> {

    public List<ChatMessage> findBySessionId(UUID sessionId) {
        return find("session.id = ?1 order by createdAt asc", sessionId).list();
    }

    public long countBySessionId(UUID sessionId) {
        return count("session.id", sessionId);
    }
}
