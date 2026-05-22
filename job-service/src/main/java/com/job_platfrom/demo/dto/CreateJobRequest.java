package com.job_platfrom.demo.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateJobRequest {
    private String role;
    private String companyName;
    private String description;
    private List<String> skillsRequired;
    private String location;
    private Integer experienceRequired;
}
