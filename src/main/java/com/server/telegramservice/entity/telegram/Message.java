package com.server.telegramservice.entity.telegram;

import com.server.telegramservice.entity.enums.Bot;
import com.server.telegramservice.entity.enums.MessageType;
import com.server.telegramservice.entity.enums.Sender;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    private Sender sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<File> files = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private MessageType messageType;
    private Bot botType;
    private LocalDateTime updatedAt = LocalDateTime.now();
}