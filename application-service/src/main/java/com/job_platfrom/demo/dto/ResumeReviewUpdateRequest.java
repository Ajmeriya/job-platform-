package com.job_platfrom.demo.dto;

import com.job_platfrom.demo.Enum.ResumeStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeReviewUpdateRequest {

    private ResumeStatus resumeStatus;
    private Double resumeScore;
}
