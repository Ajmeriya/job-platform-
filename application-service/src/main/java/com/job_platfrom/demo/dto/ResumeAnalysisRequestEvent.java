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
public class ResumeAnalysisRequestEvent {

    private Long applicationId;
    private Long jobId;
    private String candidateEmail;
    private String resumeUrl;
    private String requestId;
}