package com.scoring.core.scoring.model;

import lombok.Builder;

@Builder
public record SpecScore(
        Integer totalScore,
        String grade,
        CategoryScore schemaScore,
        CategoryScore descriptionScore,
        CategoryScore pathsScore,
        CategoryScore responseScore,
        CategoryScore exampleScore,
        CategoryScore securityScore,
        CategoryScore bestPracticesScore
) {
}
