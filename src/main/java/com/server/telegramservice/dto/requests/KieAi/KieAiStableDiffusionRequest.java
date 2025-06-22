package com.server.telegramservice.dto.requests.KieAi;

import lombok.Data;

@Data
public class KieAiStableDiffusionRequest {
    private String prompt;
    private String negativePrompt;
    private String model = "stable-diffusion-xl";
    private Integer width = 1024;
    private Integer height = 1024;
    private Integer steps = 20;
    private Double guidanceScale = 7.5;
    private Integer seed;
    private String callBackUrl;

    public KieAiStableDiffusionRequest() {}

    public KieAiStableDiffusionRequest(String prompt) {
        this.prompt = prompt;
    }
}
