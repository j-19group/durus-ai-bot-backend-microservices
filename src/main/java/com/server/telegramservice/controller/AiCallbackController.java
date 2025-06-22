package com.server.telegramservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AiCallbackController {
    @PostMapping("/api/kie/callback")
    public ResponseEntity<Void> callback(@RequestBody Map<String, Object> payload) {
        // Обработай payload: получи id, status, output
        return ResponseEntity.ok().build();
    }
}

