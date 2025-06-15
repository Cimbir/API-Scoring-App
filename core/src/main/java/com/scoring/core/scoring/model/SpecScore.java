package com.scoring.core.scoring.model;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record SpecScore(
        Integer totalScore,
        Map<String, CategoryScore> categoryScores,
        List<String> recommendations
) {
}
