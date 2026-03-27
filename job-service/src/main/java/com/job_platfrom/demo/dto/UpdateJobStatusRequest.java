package com.job_platfrom.demo.dto;

import com.job_platfrom.demo.Enum.JobStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateJobStatusRequest {
    private JobStatus status;
}
