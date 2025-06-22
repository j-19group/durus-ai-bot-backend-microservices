package com.server.telegramservice.dto.requests.KieAi;

import lombok.Data;

@Data
public class KieAiDalleImageRequest {
    private String prompt;
    private String size = "1024x1024"; // 1024x1024, 1792x1024, 1024x1792
    private String quality = "standard"; // standard, hd
    private Integer n = 1; // Number of images (1-10)
    private String callBackUrl;

    public KieAiDalleImageRequest() {}

    public KieAiDalleImageRequest(String prompt) {
        this.prompt = prompt;
    }
}
