package com.server.telegramservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
@Slf4j
public class S3Service {

    private S3Client s3Client;
    private ExecutorService executorService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String regionName;

    @Value("${aws.s3.max-file-size:5368709120}") // 5GB по умолчанию
    private long maxFileSize;

    @Value("${aws.s3.multipart-threshold:104857600}") // 100MB
    private long multipartThreshold;

    // Константы для валидации
    private static final Pattern UNSAFE_FILENAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1f\\x7f]");
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_KEY_LENGTH = 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            "video/mp4", "video/avi", "video/mov", "video/webm",
            "audio/mp3", "audio/wav", "audio/ogg",
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv", "application/json", "application/xml",
            "application/zip", "application/x-rar-compressed", "application/x-7z-compressed"
    );

    @PostConstruct
    public void initializeS3Client() {
        log.info("🚀 Initializing S3 service...");
        log.debug("S3 Configuration - Bucket: {}, BaseURL: {}, Region: {}", bucket, baseUrl, regionName);

        try {
            validateConfiguration();

            this.s3Client = S3Client.builder()
                    .endpointOverride(java.net.URI.create(baseUrl))
                    .region(Region.of(regionName))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ))
                    .build();

            this.executorService = Executors.newFixedThreadPool(10);

            // Проверяем подключение
            testConnection();

            log.info("✅ S3 client initialized successfully");
            log.info("📊 Configuration: MaxFileSize={}MB, MultipartThreshold={}MB",
                    maxFileSize / 1024 / 1024, multipartThreshold / 1024 / 1024);

        } catch (Exception e) {
            log.error("❌ Failed to initialize S3 client", e);
            throw new RuntimeException("S3 service initialization failed", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("🧹 Cleaning up S3 service resources...");
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (s3Client != null) {
            s3Client.close();
        }
        log.info("✅ S3 service cleanup completed");
    }

    /**
     * Загрузка файла с детальной валидацией и логированием
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        log.info("📤 [{}] Starting file upload - Original: '{}', Size: {}MB, Folder: '{}'",
                operationId, file.getOriginalFilename(),
                String.format("%.2f", (double) file.getSize() / 1024 / 1024), folder);

        try {
            // 1. Валидация входных данных
            validateUploadRequest(file, folder, operationId);

            // 2. Подготовка имени файла
            String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
            String contentType = determineContentType(file);
            String key = generateFileKey(folder, sanitizedFilename);

            log.debug("📝 [{}] File details - Sanitized: '{}', ContentType: '{}', Key: '{}'",
                    operationId, sanitizedFilename, contentType, key);

            // 3. Выбор стратегии загрузки
            String fileUrl;
            if (file.getSize() > multipartThreshold) {
                log.info("🔄 [{}] Using multipart upload (size > {}MB)",
                        operationId, multipartThreshold / 1024 / 1024);
                fileUrl = uploadFileMultipart(file.getBytes(), key, contentType, operationId);
            } else {
                log.info("⬆️ [{}] Using standard upload", operationId);
                fileUrl = uploadFileStandard(file.getBytes(), key, contentType, operationId);
            }

            if (fileUrl != null) {
                log.info("✅ [{}] File uploaded successfully: {}", operationId, fileUrl);
            }

            return fileUrl;

        } catch (Exception e) {
            log.error("❌ [{}] File upload failed for '{}': {}",
                    operationId, file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Загрузка байтов с детальным логированием
     */
    public String uploadBytes(byte[] data, String folder, String originalFilename, String contentType) throws IOException {
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        log.info("📤 [{}] Starting bytes upload - Filename: '{}', Size: {}MB, ContentType: '{}', Folder: '{}'",
                operationId, originalFilename,
                String.format("%.2f", (double) data.length / 1024 / 1024), contentType, folder);

        try {
            // Валидация
            validateBytesUploadRequest(data, folder, originalFilename, contentType, operationId);

            String sanitizedFilename = sanitizeFilename(originalFilename);
            String validatedContentType = validateContentType(contentType);
            String key = generateFileKey(folder, sanitizedFilename);

            log.debug("📝 [{}] Prepared - Key: '{}', ContentType: '{}'",
                    operationId, key, validatedContentType);

            String fileUrl;
            if (data.length > multipartThreshold) {
                log.info("🔄 [{}] Using multipart upload for bytes", operationId);
                fileUrl = uploadFileMultipart(data, key, validatedContentType, operationId);
            } else {
                log.info("⬆️ [{}] Using standard upload for bytes", operationId);
                fileUrl = uploadFileStandard(data, key, validatedContentType, operationId);
            }

            if (fileUrl != null) {
                log.info("✅ [{}] Bytes uploaded successfully: {}", operationId, fileUrl);
            }

            return fileUrl;

        } catch (Exception e) {
            log.error("❌ [{}] Bytes upload failed for '{}': {}",
                    operationId, originalFilename, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Множественная загрузка файлов
     */
    public List<String> uploadFiles(List<MultipartFile> files, String folder) throws IOException {
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        log.info("📦 [BATCH-{}] Starting batch upload - Count: {}, Folder: '{}'",
                batchId, files.size(), folder);

        List<String> urls = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                log.debug("📄 [BATCH-{}] Processing file {}/{}: '{}'",
                        batchId, i + 1, files.size(), file.getOriginalFilename());

                String url = uploadFile(file, folder);
                if (url != null) {
                    urls.add(url);
                } else {
                    failures.add(file.getOriginalFilename());
                }
            } catch (Exception e) {
                log.error("❌ [BATCH-{}] Failed to upload file {}/{}: '{}' - {}",
                        batchId, i + 1, files.size(), file.getOriginalFilename(), e.getMessage());
                failures.add(file.getOriginalFilename() + " (" + e.getMessage() + ")");
            }
        }

        log.info("📊 [BATCH-{}] Batch upload completed - Success: {}, Failures: {}",
                batchId, urls.size(), failures.size());

        if (!failures.isEmpty()) {
            log.warn("⚠️ [BATCH-{}] Failed files: {}", batchId, failures);
        }

        return urls;
    }

    /**
     * Асинхронная множественная загрузка
     */
    public CompletableFuture<List<String>> uploadFilesAsync(List<MultipartFile> files, String folder) {
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🚀 [ASYNC-BATCH-{}] Starting async batch upload - Count: {}", batchId, files.size());

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            final int fileIndex = i;
            final MultipartFile file = files.get(i);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("🔄 [ASYNC-BATCH-{}] Processing async file {}/{}: '{}'",
                            batchId, fileIndex + 1, files.size(), file.getOriginalFilename());
                    return uploadFile(file, folder);
                } catch (Exception e) {
                    log.error("❌ [ASYNC-BATCH-{}] Async upload failed for file {}/{}: '{}' - {}",
                            batchId, fileIndex + 1, files.size(), file.getOriginalFilename(), e.getMessage());
                    return null;
                }
            }, executorService);

            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<String> results = futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .toList();

                    log.info("✅ [ASYNC-BATCH-{}] Async batch completed - Success: {}/{}",
                            batchId, results.size(), files.size());
                    return results;
                });
    }

    /**
     * Удаление файла с детальным логированием
     */
    public boolean deleteFile(String key) {
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🗑️ [{}] Starting file deletion - Key: '{}'", operationId, key);

        try {
            validateDeleteRequest(key, operationId);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            DeleteObjectResponse response = s3Client.deleteObject(deleteRequest);

            if (response.sdkHttpResponse().isSuccessful()) {
                log.info("✅ [{}] File deleted successfully: '{}'", operationId, key);
                return true;
            } else {
                log.error("❌ [{}] Delete failed - HTTP {}: {}",
                        operationId, response.sdkHttpResponse().statusCode(),
                        response.sdkHttpResponse().statusText().orElse("Unknown error"));
                return false;
            }

        } catch (NoSuchKeyException e) {
            log.warn("⚠️ [{}] File not found for deletion: '{}' - {}", operationId, key, e.getMessage());
            return true; // Считаем успешным, если файл уже отсутствует
        } catch (S3Exception e) {
            log.error("❌ [{}] S3 error during deletion of '{}' - Code: {}, Message: {}",
                    operationId, key, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
            return false;
        } catch (SdkServiceException e) {
            log.error("❌ [{}] AWS service error during deletion of '{}' - HTTP {}: {}",
                    operationId, key, e.statusCode(), e.getMessage());
            return false;
        } catch (SdkClientException e) {
            log.error("❌ [{}] AWS client error during deletion of '{}': {}",
                    operationId, key, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ [{}] Unexpected error during deletion of '{}': {}",
                    operationId, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Удаление файла по URL
     */
    public boolean deleteFileByUrl(String fileUrl) {
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        log.info("🗑️ [{}] Starting file deletion by URL: '{}'", operationId, fileUrl);

        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                log.error("❌ [{}] Invalid URL format: '{}'", operationId, fileUrl);
                return false;
            }

            log.debug("🔍 [{}] Extracted key from URL: '{}'", operationId, key);
            return deleteFile(key);

        } catch (Exception e) {
            log.error("❌ [{}] Error extracting key from URL '{}': {}",
                    operationId, fileUrl, e.getMessage(), e);
            return false;
        }
    }

    // ==================== ПРИВАТНЫЕ МЕТОДЫ ====================

    private void validateConfiguration() {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(bucket)) errors.add("Bucket name is required");
        if (!StringUtils.hasText(baseUrl)) errors.add("Base URL is required");
        if (!StringUtils.hasText(accessKey)) errors.add("Access key is required");
        if (!StringUtils.hasText(secretKey)) errors.add("Secret key is required");
        if (!StringUtils.hasText(regionName)) errors.add("Region is required");

        if (!errors.isEmpty()) {
            throw new IllegalStateException("S3 configuration errors: " + String.join(", ", errors));
        }

        log.debug("✅ S3 configuration validation passed");
    }

    private void testConnection() {
        try {
            log.debug("🔍 Testing S3 connection...");
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.headBucket(headBucketRequest);
            log.info("✅ S3 connection test successful");
        } catch (Exception e) {
            log.error("❌ S3 connection test failed: {}", e.getMessage());
            throw new RuntimeException("S3 connection test failed", e);
        }
    }

    private void validateUploadRequest(MultipartFile file, String folder, String operationId) {
        List<String> errors = new ArrayList<>();

        if (file == null) {
            errors.add("File is null");
        } else {
            if (file.isEmpty()) {
                errors.add("File is empty");
            }
            if (file.getSize() > maxFileSize) {
                errors.add(String.format("File size (%d bytes) exceeds maximum allowed (%d bytes)",
                        file.getSize(), maxFileSize));
            }
            if (!StringUtils.hasText(file.getOriginalFilename())) {
                errors.add("Original filename is required");
            }
        }

        if (!StringUtils.hasText(folder)) {
            errors.add("Folder is required");
        }

        if (!errors.isEmpty()) {
            String errorMsg = "Validation failed: " + String.join(", ", errors);
            log.error("❌ [{}] {}", operationId, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.debug("✅ [{}] Upload request validation passed", operationId);
    }

    private void validateBytesUploadRequest(byte[] data, String folder, String filename,
                                            String contentType, String operationId) {
        List<String> errors = new ArrayList<>();

        if (data == null || data.length == 0) {
            errors.add("Data is null or empty");
        } else if (data.length > maxFileSize) {
            errors.add(String.format("Data size (%d bytes) exceeds maximum allowed (%d bytes)",
                    data.length, maxFileSize));
        }

        if (!StringUtils.hasText(folder)) {
            errors.add("Folder is required");
        }

        if (!StringUtils.hasText(filename)) {
            errors.add("Filename is required");
        }

        if (!StringUtils.hasText(contentType)) {
            errors.add("Content type is required");
        }

        if (!errors.isEmpty()) {
            String errorMsg = "Validation failed: " + String.join(", ", errors);
            log.error("❌ [{}] {}", operationId, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.debug("✅ [{}] Bytes upload request validation passed", operationId);
    }

    private void validateDeleteRequest(String key, String operationId) {
        if (!StringUtils.hasText(key)) {
            String errorMsg = "Key is required for deletion";
            log.error("❌ [{}] {}", operationId, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (key.length() > MAX_KEY_LENGTH) {
            String errorMsg = String.format("Key length (%d) exceeds maximum allowed (%d)",
                    key.length(), MAX_KEY_LENGTH);
            log.error("❌ [{}] {}", operationId, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        log.debug("✅ [{}] Delete request validation passed", operationId);
    }

    private String sanitizeFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "unnamed_file_" + Instant.now().toEpochMilli();
        }

        String sanitized = originalFilename;

        // Удаляем опасные символы
        sanitized = UNSAFE_FILENAME_PATTERN.matcher(sanitized).replaceAll("_");

        // Ограничиваем длину
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0 && lastDot < sanitized.length() - 1) {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, Math.min(MAX_FILENAME_LENGTH - extension.length(), lastDot));
                sanitized += extension;
            } else {
                sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH);
            }
        }

        // URL-кодирование для безопасности
        try {
            sanitized = URLEncoder.encode(sanitized, StandardCharsets.UTF_8)
                    .replace("+", "%20"); // Пробелы как %20 вместо +
        } catch (Exception e) {
            log.warn("Failed to URL encode filename '{}', using original: {}", sanitized, e.getMessage());
        }

        log.debug("🧹 Filename sanitized: '{}' -> '{}'", originalFilename, sanitized);
        return sanitized;
    }

    private String determineContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (!StringUtils.hasText(contentType) || "application/octet-stream".equals(contentType)) {
            // Пытаемся определить по расширению
            String filename = file.getOriginalFilename();
            if (StringUtils.hasText(filename)) {
                String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
                contentType = getContentTypeByExtension(extension);
            }
        }

        return validateContentType(contentType);
    }

    private String getContentTypeByExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/avi";
            case "mov" -> "video/mov";
            case "webm" -> "video/webm";
            case "mp3" -> "audio/mp3";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "7z" -> "application/x-7z-compressed";
            default -> "application/octet-stream";
        };
    }

    private String validateContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            log.warn("⚠️ Content type is empty, using default");
            return "application/octet-stream";
        }

        if (!ALLOWED_CONTENT_TYPES.contains(contentType) &&
                !"application/octet-stream".equals(contentType)) {
            log.warn("⚠️ Content type '{}' not in allowed list, proceeding anyway", contentType);
        }

        return contentType;
    }

    private String generateFileKey(String folder, String filename) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String key = folder + "/" + timestamp + "_" + filename;

        if (key.length() > MAX_KEY_LENGTH) {
            // Обрезаем имя файла, сохраняя расширение
            String extension = "";
            int lastDot = filename.lastIndexOf('.');
            if (lastDot > 0) {
                extension = filename.substring(lastDot);
            }

            int maxFilenameLength = MAX_KEY_LENGTH - folder.length() - timestamp.length() - 2 - extension.length();
            String truncatedFilename = filename.substring(0, Math.min(filename.length() - extension.length(), maxFilenameLength)) + extension;
            key = folder + "/" + timestamp + "_" + truncatedFilename;

            log.warn("⚠️ Key was too long, truncated to: '{}'", key);
        }

        return key;
    }

    private String uploadFileStandard(byte[] data, String key, String contentType, String operationId) {
        try {
            log.debug("⬆️ [{}] Executing standard upload for key: '{}'", operationId, key);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));

            if (response.sdkHttpResponse().isSuccessful()) {
                String fileUrl = baseUrl + "/" + bucket + "/" + key;
                log.debug("✅ [{}] Standard upload successful, ETag: {}",
                        operationId, response.eTag());
                return fileUrl;
            } else {
                log.error("❌ [{}] Standard upload failed - HTTP {}: {}",
                        operationId, response.sdkHttpResponse().statusCode(),
                        response.sdkHttpResponse().statusText().orElse("Unknown error"));
                return null;
            }

        } catch (S3Exception e) {
            log.error("❌ [{}] S3 error during standard upload - Code: {}, Message: {}",
                    operationId, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (SdkServiceException e) {
            log.error("❌ [{}] AWS service error during standard upload - HTTP {}: {}",
                    operationId, e.statusCode(), e.getMessage());
            throw new RuntimeException("AWS service error: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            log.error("❌ [{}] AWS client error during standard upload: {}",
                    operationId, e.getMessage(), e);
            throw new RuntimeException("AWS client error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ [{}] Unexpected error during standard upload: {}",
                    operationId, e.getMessage(), e);
            throw new RuntimeException("Unexpected upload error: " + e.getMessage(), e);
        }
    }

    private String uploadFileMultipart(byte[] data, String key, String contentType, String operationId) {
        String uploadId = null;
        List<CompletedPart> completedParts = new ArrayList<>();

        try {
            log.debug("🔄 [{}] Starting multipart upload for key: '{}'", operationId, key);

            // 1. Инициируем multipart upload
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
            uploadId = createResponse.uploadId();

            log.debug("📋 [{}] Multipart upload initiated, uploadId: {}", operationId, uploadId);

            // 2. Разбиваем файл на части и загружаем
            int partSize = 5 * 1024 * 1024; // 5MB минимум для multipart
            int partNumber = 1;
            int offset = 0;

            while (offset < data.length) {
                int currentPartSize = Math.min(partSize, data.length - offset);
                byte[] partData = Arrays.copyOfRange(data, offset, offset + currentPartSize);

                log.debug("📦 [{}] Uploading part {}, size: {}MB",
                        operationId, partNumber, String.format("%.2f", (double) currentPartSize / 1024 / 1024));

                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long) currentPartSize)
                        .build();

                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest,
                        RequestBody.fromBytes(partData));

                CompletedPart completedPart = CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build();

                completedParts.add(completedPart);

                offset += currentPartSize;
                partNumber++;
            }

            // 3. Завершаем multipart upload
            log.debug("🏁 [{}] Completing multipart upload with {} parts", operationId, completedParts.size());

            CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(completedUpload)
                    .build();

            CompleteMultipartUploadResponse completeResponse = s3Client.completeMultipartUpload(completeRequest);

            String fileUrl = baseUrl + "/" + bucket + "/" + key;
            log.info("✅ [{}] Multipart upload completed successfully, ETag: {}",
                    operationId, completeResponse.eTag());
            return fileUrl;

        } catch (Exception e) {
            // В случае ошибки - отменяем multipart upload
            if (uploadId != null) {
                try {
                    log.warn("🚫 [{}] Aborting multipart upload due to error: {}", operationId, e.getMessage());
                    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .uploadId(uploadId)
                            .build();
                    s3Client.abortMultipartUpload(abortRequest);
                    log.debug("✅ [{}] Multipart upload aborted", operationId);
                } catch (Exception abortException) {
                    log.error("❌ [{}] Failed to abort multipart upload: {}",
                            operationId, abortException.getMessage());
                }
            }

            log.error("❌ [{}] Multipart upload failed: {}", operationId, e.getMessage(), e);
            throw new RuntimeException("Multipart upload failed: " + e.getMessage(), e);
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }

        String prefix = baseUrl + "/" + bucket + "/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }

        log.warn("⚠️ URL doesn't match expected format: '{}'", fileUrl);
        return null;
    }
}