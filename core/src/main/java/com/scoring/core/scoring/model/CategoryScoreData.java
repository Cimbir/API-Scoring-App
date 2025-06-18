package com.scoring.core.scoring.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryScoreData {
    int points = 0;
    List<CategoryScore.Issue> issues = new ArrayList<>();
    List<String> strengths = new ArrayList<>();

    public CategoryScore buildScore(int maxPoints, String name) {
        return CategoryScore.builder()
                .maxScore(maxPoints)
                .score(Math.max(0, this.getPoints()))
                .categoryName(name)
                .issues(this.getIssues())
                .strengths(this.getStrengths())
                .build();
    }
}
