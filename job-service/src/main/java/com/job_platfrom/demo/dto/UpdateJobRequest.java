package com.job_platfrom.demo.dto;

import com.job_platfrom.demo.Enum.JobStatus;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateJobRequest {
    private String role;
    private String companyName;
    private String description;
    private List<String> skillsRequired;
    private String location;
    private Integer experienceRequired;
    private JobStatus status;
}
