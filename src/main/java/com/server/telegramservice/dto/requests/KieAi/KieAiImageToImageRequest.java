package com.server.telegramservice.dto.requests.KieAi;

import lombok.Data;

@Data
public class KieAiImageToImageRequest {
    private String prompt;
    private String fileUrl; // URL of source image
    private String model = "stable-diffusion-xl";
    private Double strength = 0.75; // 0.1-1.0
    private Integer steps = 20;
    private String callBackUrl;

    public KieAiImageToImageRequest() {}

    public KieAiImageToImageRequest(String prompt, String fileUrl) {
        this.prompt = prompt;
        this.fileUrl = fileUrl;
    }
}
