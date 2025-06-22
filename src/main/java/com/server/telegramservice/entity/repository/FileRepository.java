package com.server.telegramservice.entity.repository;

import com.server.telegramservice.entity.telegram.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
}