package com.job_platfrom.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResultEvent {

    private Long applicationId;
    private String requestId;
    private Double similarity;
    private Double matchPercentage;
    private String verdict;
}