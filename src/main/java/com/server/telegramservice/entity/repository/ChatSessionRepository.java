package com.server.telegramservice.entity.repository;

import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.entity.telegram.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findTopByUserAndEndedAtIsNullOrderByStartedAtDesc(User user);
   }
