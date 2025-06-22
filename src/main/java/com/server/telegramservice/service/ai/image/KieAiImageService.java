package com.server.telegramservice.service.ai.image;

import com.server.telegramservice.dto.requests.KieAi.*;
import com.server.telegramservice.dto.responses.KieAi.*;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KieAiImageService {
    @Value("${kieai.api-key}")
    private String apiKey;

    private final RestTemplate rt;
    @Value("${kieai.api-url}")
    private  String baseUrl;

    public KieAiImageService(RestTemplate rt) {
        this.rt = rt;
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiKey);
        return h;
    }


    public KieAiFluxImageResponse generateFluxImage(KieAiFluxImageRequest req) {
        return post("/api/v1/flux/generate", req, KieAiFluxImageResponse.class);
    }

    /**
     * Image-to-Image: Transform existing image with text prompt
     */
    public KieAiImageToImageResponse imageToImage(KieAiImageToImageRequest req) {
        return post("/api/v1/image-to-image/generate", req, KieAiImageToImageResponse.class);
    }

    /**
     * Get image generation task status
     */
    public KieAiImageTaskResponse getImageTask(String taskId) {
        return get("/api/v1/image/record-info?task_id=" + taskId, KieAiImageTaskResponse.class);
    }


    /**
     * Stable Diffusion image generation
     */
    public KieAiStableDiffusionResponse generateStableDiffusion(KieAiStableDiffusionRequest req) {
        return post("/api/v1/stable-diffusion/generate", req, KieAiStableDiffusionResponse.class);
    }
    
    private <T> T post(String uri, Object req, Class<T> resType) {
        HttpEntity<?> e = new HttpEntity<>(req, headers());
        return rt.postForObject(baseUrl + uri, e, resType);
    }

    private <T> T get(String uri, Class<T> resType) {
        HttpEntity<Void> e = new HttpEntity<>(headers());
        return rt.exchange(baseUrl + uri, HttpMethod.GET, e, resType).getBody();
    }
}

