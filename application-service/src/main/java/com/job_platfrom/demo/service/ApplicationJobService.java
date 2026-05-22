package com.job_platfrom.demo.service;

import com.job_platfrom.demo.client.JobServiceClient;
import com.job_platfrom.demo.dto.JobResponse;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationJobService {

    private final JobServiceClient jobServiceClient;

    public ApplicationJobService(JobServiceClient jobServiceClient) {
        this.jobServiceClient = jobServiceClient;
    }

    public List<JobResponse> fetchAllJobs(String authorizationHeader) {
        List<JobResponse> jobs = jobServiceClient.getAllJobs(authorizationHeader);
        return jobs == null ? Collections.emptyList() : jobs;
    }
}
