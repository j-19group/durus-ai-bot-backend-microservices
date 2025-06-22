package com.server.telegramservice.dto.requests.KieAi;

import lombok.Data;

@Data
public class KieAiFluxImageRequest {
    private String prompt;
    private String model = "flux-kontext-pro"; // flux-kontext-pro, flux-kontext-max
    private String size = "1:1"; // 1:1, 3:2, 2:3, 16:9, 9:16
    private String callBackUrl; // Optional webhook URL
    private Boolean async = false; // Async generation

    // Constructors
    public KieAiFluxImageRequest() {}

    public KieAiFluxImageRequest(String prompt) {
        this.prompt = prompt;
    }

    public KieAiFluxImageRequest(String prompt, String model, String size) {
        this.prompt = prompt;
        this.model = model;
        this.size = size;
    }
}
