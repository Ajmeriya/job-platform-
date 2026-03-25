package com.job_platfrom.demo.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "interview_rounds")
@Getter
@Setter
public class InterviewRound extends JobRound {

    @ElementCollection
    @CollectionTable(name = "interview_round_skills", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "skill")
    private List<String> interviewSkills = new ArrayList<>();
}
