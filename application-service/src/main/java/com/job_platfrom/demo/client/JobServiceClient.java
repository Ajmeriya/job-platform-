package com.job_platfrom.demo.client;

import com.job_platfrom.demo.dto.JobResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
        return getJobList(jobServiceBaseUrl + "/api/jobs", authorizationHeader);
    }

    public JobResponse getJobById(Long jobId, String authorizationHeader) {
        return exchange(
            jobServiceBaseUrl + "/api/jobs/" + jobId,
            HttpMethod.GET,
            authorizationHeader,
            null,
            JobResponse.class
        );
    }

    public JobResponse createJob(Object request, Long createdBy, String authorizationHeader) {
        return exchange(
            jobServiceBaseUrl + "/api/jobs?createdBy=" + createdBy,
            HttpMethod.POST,
            authorizationHeader,
            request,
            JobResponse.class
        );
    }

    public JobResponse updateJob(Long jobId, Object request, Long createdBy, String authorizationHeader) {
        return exchange(
            jobServiceBaseUrl + "/api/jobs/" + jobId + "?createdBy=" + createdBy,
            HttpMethod.PUT,
            authorizationHeader,
            request,
            JobResponse.class
        );
    }

    public JobResponse updateJobStatus(Long jobId, Object request, Long createdBy, String authorizationHeader) {
        return exchange(
            jobServiceBaseUrl + "/api/jobs/" + jobId + "/status?createdBy=" + createdBy,
            HttpMethod.PUT,
            authorizationHeader,
            request,
            JobResponse.class
        );
    }

    public void deleteJob(Long jobId, Long createdBy, String authorizationHeader) {
        exchange(
            jobServiceBaseUrl + "/api/jobs/" + jobId + "?createdBy=" + createdBy,
            HttpMethod.DELETE,
            authorizationHeader,
            null,
            Void.class
        );
    }

    public Object addRounds(Long jobId, Object rounds, String authorizationHeader) {
        return exchange(
            jobServiceBaseUrl + "/api/jobs/" + jobId + "/rounds",
            HttpMethod.POST,
            authorizationHeader,
            rounds,
            Object.class
        );
    }

    public Object getRoundsByJobId(Long jobId, String authorizationHeader) {
        return exchange(
            jobServiceBaseUrl + "/api/jobs/" + jobId + "/rounds",
            HttpMethod.GET,
            authorizationHeader,
            null,
            Object.class
        );
    }

    private List<JobResponse> getJobList(String url, String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<List<JobResponse>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<JobResponse>>() {
            }
        );

        return response.getBody();
    }

    private <T> T exchange(String url, HttpMethod method, String authorizationHeader, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, method, requestEntity, responseType);
        return response.getBody();
    }
}
