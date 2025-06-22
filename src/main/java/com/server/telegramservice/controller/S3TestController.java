
package com.server.telegramservice.controller;

import com.server.telegramservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/s3/test")
@RequiredArgsConstructor
@Slf4j
public class S3TestController {

    private final S3Service s3Service;

    /**
     * Проверка подключения к S3
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("🔍 S3 Health check requested");

        Map<String, Object> response = new HashMap<>();

        try {
            // Попробуем загрузить тестовый файл
            String testContent = "Health check test - " + LocalDateTime.now();
            byte[] testData = testContent.getBytes(StandardCharsets.UTF_8);

            String testUrl = s3Service.uploadBytes(testData, "test", "health-check.txt", "text/plain");

            if (testUrl != null) {
                response.put("status", "healthy");
                response.put("message", "S3 service is working correctly");
                response.put("testFileUrl", testUrl);
                response.put("timestamp", LocalDateTime.now());

                // Удаляем тестовый файл
                boolean deleted = s3Service.deleteFileByUrl(testUrl);
                response.put("testFileDeleted", deleted);

                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to upload test file");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            log.error("❌ S3 health check failed: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "S3 service error: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Загрузка одного файла
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "folder", required = false) String folder) {

        log.info("📤 Single file upload request: {} to folder: {}",
                file.getOriginalFilename(), folder);

        Map<String, Object> response = new HashMap<>();

        try {
            String fileUrl = s3Service.uploadFile(file, folder);

            if (fileUrl != null) {
                response.put("success", true);
                response.put("message", "File uploaded successfully");
                response.put("fileUrl", fileUrl);
                response.put("originalFilename", file.getOriginalFilename());
                response.put("fileSize", file.getSize());
                response.put("contentType", file.getContentType());
                response.put("folder", folder);
                response.put("uploadedAt", LocalDateTime.now());

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Upload failed - no URL returned");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (IOException e) {
            log.error("❌ Single file upload failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Загрузка нескольких файлов
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "test") String folder) {

        log.info("📦 Multiple files upload request: {} files to folder: {}",
                files.size(), folder);

        Map<String, Object> response = new HashMap<>();

        try {
            List<String> fileUrls = s3Service.uploadFiles(files, folder);

            response.put("success", true);
            response.put("message", "Batch upload completed");
            response.put("totalFiles", files.size());
            response.put("successfulUploads", fileUrls.size());
            response.put("failedUploads", files.size() - fileUrls.size());
            response.put("fileUrls", fileUrls);
            response.put("folder", folder);
            response.put("uploadedAt", LocalDateTime.now());

            // Детали по каждому файлу
            List<Map<String, Object>> fileDetails = files.stream()
                    .map(file -> {
                        Map<String, Object> details = new HashMap<>();
                        details.put("filename", file.getOriginalFilename());
                        details.put("size", file.getSize());
                        details.put("contentType", file.getContentType());
                        return details;
                    })
                    .toList();
            response.put("fileDetails", fileDetails);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("❌ Multiple files upload failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Batch upload failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Асинхронная загрузка нескольких файлов
     */
    @PostMapping("/upload/async")
    public ResponseEntity<Map<String, Object>> uploadFilesAsync(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "test") String folder) {

        log.info("🚀 Async multiple files upload request: {} files to folder: {}",
                files.size(), folder);

        Map<String, Object> response = new HashMap<>();

        try {
            CompletableFuture<List<String>> future = s3Service.uploadFilesAsync(files, folder);

            // Ждем завершения (в реальном приложении лучше вернуть ID задачи)
            List<String> fileUrls = future.get();

            response.put("success", true);
            response.put("message", "Async batch upload completed");
            response.put("totalFiles", files.size());
            response.put("successfulUploads", fileUrls.size());
            response.put("failedUploads", files.size() - fileUrls.size());
            response.put("fileUrls", fileUrls);
            response.put("folder", folder);
            response.put("uploadedAt", LocalDateTime.now());
            response.put("asyncMode", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Async files upload failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Async upload failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Загрузка байтов (для тестирования загрузки сгенерированного контента)
     */
    @PostMapping("/upload/bytes")
    public ResponseEntity<Map<String, Object>> uploadBytes(
            @RequestParam("content") String content,
            @RequestParam(value = "filename", defaultValue = "test-content.txt") String filename,
            @RequestParam(value = "folder", defaultValue = "test") String folder,
            @RequestParam(value = "contentType", defaultValue = "text/plain") String contentType) {

        log.info("📝 Bytes upload request: {} chars to {}/{}",
                content.length(), folder, filename);

        Map<String, Object> response = new HashMap<>();

        try {
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            String fileUrl = s3Service.uploadBytes(data, folder, filename, contentType);

            if (fileUrl != null) {
                response.put("success", true);
                response.put("message", "Bytes uploaded successfully");
                response.put("fileUrl", fileUrl);
                response.put("filename", filename);
                response.put("dataSize", data.length);
                response.put("contentType", contentType);
                response.put("folder", folder);
                response.put("uploadedAt", LocalDateTime.now());

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Bytes upload failed - no URL returned");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (IOException e) {
            log.error("❌ Bytes upload failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Bytes upload failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Создание тестового изображения и его загрузка
     */
    @PostMapping("/upload/test-image")
    public ResponseEntity<Map<String, Object>> uploadTestImage(
            @RequestParam(value = "folder", defaultValue = "images") String folder) {

        log.info("🖼️ Test image generation and upload request to folder: {}", folder);

        Map<String, Object> response = new HashMap<>();

        try {
            // Создаем простое тестовое изображение (1x1 PNG)
            byte[] pngData = createTestPngImage();
            String filename = "test-image-" + UUID.randomUUID().toString().substring(0, 8) + ".png";

            String fileUrl = s3Service.uploadBytes(pngData, folder, filename, "image/png");

            if (fileUrl != null) {
                response.put("success", true);
                response.put("message", "Test image uploaded successfully");
                response.put("fileUrl", fileUrl);
                response.put("filename", filename);
                response.put("imageSize", pngData.length);
                response.put("contentType", "image/png");
                response.put("folder", folder);
                response.put("uploadedAt", LocalDateTime.now());

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Test image upload failed");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            log.error("❌ Test image upload failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Test image upload failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Удаление файла по URL
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("url") String fileUrl) {
        log.info("🗑️ Delete file request: {}", fileUrl);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = s3Service.deleteFileByUrl(fileUrl);

            response.put("success", deleted);
            response.put("message", deleted ? "File deleted successfully" : "File deletion failed");
            response.put("fileUrl", fileUrl);
            response.put("deletedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ File deletion failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "File deletion failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Stress test - загрузка множества файлов для проверки производительности
     */
    @PostMapping("/stress-test")
    public ResponseEntity<Map<String, Object>> stressTest(
            @RequestParam(value = "fileCount", defaultValue = "10") int fileCount,
            @RequestParam(value = "fileSize", defaultValue = "1024") int fileSizeBytes,
            @RequestParam(value = "folder", defaultValue = "stress-test") String folder) {

        log.info("⚡ Stress test request: {} files of {} bytes each", fileCount, fileSizeBytes);

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            List<String> uploadedUrls = new java.util.ArrayList<>();
            List<String> errors = new java.util.ArrayList<>();

            for (int i = 0; i < fileCount; i++) {
                try {
                    // Создаем тестовые данные
                    String content = "Stress test file " + i + " - " +
                            "x".repeat(Math.max(0, fileSizeBytes - 50));
                    byte[] data = content.getBytes(StandardCharsets.UTF_8);
                    String filename = String.format("stress-test-%d-%s.txt", i,
                            UUID.randomUUID().toString().substring(0, 8));

                    String fileUrl = s3Service.uploadBytes(data, folder, filename, "text/plain");

                    if (fileUrl != null) {
                        uploadedUrls.add(fileUrl);
                    } else {
                        errors.add("File " + i + ": Upload returned null");
                    }

                } catch (Exception e) {
                    errors.add("File " + i + ": " + e.getMessage());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            response.put("success", true);
            response.put("message", "Stress test completed");
            response.put("totalFiles", fileCount);
            response.put("successfulUploads", uploadedUrls.size());
            response.put("failedUploads", errors.size());
            response.put("totalTimeMs", totalTime);
            response.put("avgTimePerFileMs", totalTime / (double) fileCount);
            response.put("uploadedUrls", uploadedUrls);
            response.put("errors", errors);
            response.put("folder", folder);
            response.put("fileSizeBytes", fileSizeBytes);
            response.put("completedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Stress test failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Stress test failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("totalTimeMs", System.currentTimeMillis() - startTime);
            return ResponseEntity.internalServerError().body(response);
        }
    }



    // ==================== УТИЛИТАРНЫЕ МЕТОДЫ ====================

    /**
     * Создает минимальное тестовое PNG изображение 1x1 пиксель
     */
    private byte[] createTestPngImage() {
        // Минимальный PNG файл (1x1 черный пиксель)
        return new byte[] {
                (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG signature
                0x00, 0x00, 0x00, 0x0D,  // IHDR chunk size
                0x49, 0x48, 0x44, 0x52,  // IHDR
                0x00, 0x00, 0x00, 0x01,  // width = 1
                0x00, 0x00, 0x00, 0x01,  // height = 1
                0x08, 0x02, 0x00, 0x00, 0x00,  // bit depth, color type, compression, filter, interlace
                (byte)0x90, 0x77, 0x53, (byte)0xDE,  // CRC
                0x00, 0x00, 0x00, 0x0C,  // IDAT chunk size
                0x49, 0x44, 0x41, 0x54,  // IDAT
                0x08, (byte)0x99, 0x01, 0x01, 0x00, 0x00, (byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01,  // compressed data
                (byte)0xE2, 0x21, (byte)0xBC, 0x33,  // CRC
                0x00, 0x00, 0x00, 0x00,  // IEND chunk size
                0x49, 0x45, 0x4E, 0x44,  // IEND
                (byte)0xAE, 0x42, 0x60, (byte)0x82   // CRC
        };
    }
}
