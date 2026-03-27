package com.job_platfrom.demo.dto;

import com.job_platfrom.demo.Enum.ApplicationStatus;
import com.job_platfrom.demo.Enum.ResumeStatus;
import com.job_platfrom.demo.Enum.RoundType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationResponse {

    private Long id;
    private Long jobId;
    private String candidateEmail;
    private String resumeUrl;
    private ApplicationStatus status;
    private RoundType currentRound;
    private Double resumeScore;
    private ResumeStatus resumeStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ApplicationRoundResponse> rounds;
}
