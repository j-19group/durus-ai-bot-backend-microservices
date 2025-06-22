package com.server.telegramservice.dto.requests.Runway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunwayRequestFromImageToVideo {
    private String promptImage;
    private Long seed;
    private String model;
    private String promptText;
    private Integer duration;
    private String ratio;
}
