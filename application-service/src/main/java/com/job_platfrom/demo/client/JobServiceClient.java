package com.job_platfrom.demo.client;

import com.job_platfrom.demo.dto.JobResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class JobServiceClient {

    private final RestTemplate restTemplate;
    private final String jobServiceBaseUrl;

    public JobServiceClient(RestTemplate restTemplate,
                            @Value("${services.job-service.base-url:http://localhost:8081}") String jobServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.jobServiceBaseUrl = jobServiceBaseUrl;
    }

    public List<JobResponse> getAllJobs(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<List<JobResponse>> response = restTemplate.exchange(
            jobServiceBaseUrl + "/api/jobs",
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<JobResponse>>() {
            }
        );

        return response.getBody();
    }
}
