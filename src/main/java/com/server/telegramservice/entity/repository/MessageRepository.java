package com.server.telegramservice.entity.repository;

import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.entity.telegram.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE m.session = :session ORDER BY m.createdAt DESC")
    List<Message> findRecentMessagesBySession(@org.springframework.data.repository.query.Param("session") ChatSession session, Pageable pageable);

}
