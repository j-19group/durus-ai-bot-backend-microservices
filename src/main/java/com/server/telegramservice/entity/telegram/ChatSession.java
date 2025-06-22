package com.server.telegramservice.entity.telegram;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "chat_sessions")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private List<Message> messages = new ArrayList<>();

    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime endedAt;
}
