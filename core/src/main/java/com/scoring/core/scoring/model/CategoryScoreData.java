package com.scoring.core.scoring.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoryScoreData {
    int points;
    List<CategoryScore.Issue> issues;
    List<String> strengths;
}
