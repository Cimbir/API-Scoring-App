package com.scoring.core.scoring.model.category.path;

public record NamingAnalysis(
        boolean isConsistent,
        String detectedPattern
) {
}
