package com.job_platfrom.demo.dto;

import com.job_platfrom.demo.Enum.RoundStatus;
import com.job_platfrom.demo.Enum.RoundType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationRoundResponse {

    private Long id;
    private RoundType roundType;
    private RoundStatus status;
    private Double score;
    private String feedback;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
