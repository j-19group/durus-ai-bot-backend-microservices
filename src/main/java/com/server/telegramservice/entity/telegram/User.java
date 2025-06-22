package com.server.telegramservice.entity.telegram;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    private String username;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ChatSession> sessions = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
