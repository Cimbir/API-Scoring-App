package com.scoring.core.scoring.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CategoryScore(
        Integer score,
        Integer maxScore,
        String categoryName,
        List<Issue> issues,
        List<String> strengths
) {
    @Builder
    public record Issue(
            String location,
            String description,
            Severity severity,
            String suggestion
    ) {
    }
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }
}
