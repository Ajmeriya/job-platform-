package com.job_platfrom.demo.dto;

import com.job_platfrom.demo.Enum.DifficultyLevel;
import com.job_platfrom.demo.Enum.RoundType;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobRoundRequest {
    private RoundType roundType;

    private Integer aptitudeQuestions;
    private List<String> aptitudeTopics;

    private Integer dsaQuestions;
    private Integer sqlQuestions;
    private List<String> technicalTopics;

    private DifficultyLevel difficulty;
    private Integer timeLimit;

    private List<String> interviewSkills;
}
