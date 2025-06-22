package com.server.telegramservice.dto.requests.Runway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunwayTextToImageRequest {
    private String model;
    private String prompt;
    private Integer numImages;
    private String ratio;
    private List<String> referenceImages;
}
