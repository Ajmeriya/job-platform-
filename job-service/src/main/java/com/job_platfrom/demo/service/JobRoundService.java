package com.job_platfrom.demo.service;

import com.job_platfrom.demo.dto.JobRoundRequest;
import com.job_platfrom.demo.entity.JobRound;
import java.util.List;

public interface JobRoundService {

	List<JobRound> addRounds(Long jobId, List<JobRoundRequest> rounds);

	List<JobRound> getRoundsByJobId(Long jobId);
}
