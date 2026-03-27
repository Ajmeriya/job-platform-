package com.job_platfrom.demo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResumeUploadResponse {

    private String fileName;
    private String storedFileName;
    private String fileUrl;
}
