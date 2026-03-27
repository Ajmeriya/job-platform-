package com.job_platfrom.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private List<String> skillsRequired;
    private Long createdBy;
    private String status;
    private String location;
    private Integer experienceRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
