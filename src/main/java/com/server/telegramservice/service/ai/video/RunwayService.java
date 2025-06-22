package com.server.telegramservice.service.ai.video;

import com.server.telegramservice.dto.requests.Runway.RunwayRequestFromImageToVideo;
import com.server.telegramservice.dto.requests.Runway.RunwayTextToImageRequest;
import com.server.telegramservice.dto.responses.RunWay.RunWayTaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class RunwayService {
    private final RestTemplate restTemplate;
    @Value("${runway.api-key}") private String apiKey;

    private HttpHeaders baseHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiKey);
        h.set("X-Runway-Version", "2024-11-06");
        return h;
    }

    public RunWayTaskResponse imageToVideo(RunwayRequestFromImageToVideo req) {
        HttpEntity<RunwayRequestFromImageToVideo> e = new HttpEntity<>(req, baseHeaders());
        return restTemplate
                .postForObject("https://api.dev.runwayml.com/v1/image_to_video", e, RunWayTaskResponse.class);
    }

    public RunWayTaskResponse textToImage(RunwayTextToImageRequest req) {
        HttpEntity<RunwayTextToImageRequest> e = new HttpEntity<>(req, baseHeaders());
        return restTemplate
                .postForObject("https://api.dev.runwayml.com/v1/text_to_image", e, RunWayTaskResponse.class);
    }

    public RunWayTaskResponse getTask(String id) {
        HttpEntity<Void> e = new HttpEntity<>(baseHeaders());
        return restTemplate
                .exchange("https://api.dev.runwayml.com/v1/tasks/"+id,
                HttpMethod.GET, e, RunWayTaskResponse.class).getBody();
    }

    public void cancelTask(String id) {
        HttpEntity<Void> e = new HttpEntity<>(baseHeaders());
        restTemplate
                .exchange("https://api.dev.runwayml.com/v1/tasks/"+id,
                HttpMethod.DELETE, e, Void.class);
    }
}

