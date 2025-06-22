package com.server.telegramservice.entity.telegram;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "files")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String filename;
    private String fileType;
    private String s3Url;

    @ManyToOne
    @JoinColumn(name = "message_id")
    private Message message;
}