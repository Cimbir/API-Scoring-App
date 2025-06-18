package com.scoring.core.scoring.service;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.SpecScore;
import com.scoring.core.scoring.service.category.*;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class APIScoringService {
    private final ScoringConfig scoringConfig;

    private final SchemaScoringService schemaScoringService;
    private final DescriptionScoringService descriptionScoringService;
    private final PathsScoringService pathsScoringService;
    private final ResponseScoringService responseScoringService;
    private final ExampleScoringService exampleScoringService;
    private final SecurityScoringService securityScoringService;
    private final BestPracticesScoringService bestPracticesScoringService;

    public SpecScore score(OpenAPI spec) {
        CategoryScore schemaScore = schemaScoringService.scoreCategory(spec);
        CategoryScore descriptionScore = descriptionScoringService.scoreCategory(spec);
        CategoryScore pathsScore = pathsScoringService.scoreCategory(spec);
        CategoryScore responseScore = responseScoringService.scoreCategory(spec);
        CategoryScore exampleScore = exampleScoringService.scoreCategory(spec);
        CategoryScore securityScore = securityScoringService.scoreCategory(spec);
        CategoryScore bestPracticesScore = bestPracticesScoringService.scoreCategory(spec);

        int totalScore = schemaScore.score() +
                descriptionScore.score() +
                pathsScore.score() +
                responseScore.score() +
                exampleScore.score() +
                securityScore.score() +
                bestPracticesScore.score();

        String grade = getGrade(totalScore);

        return SpecScore.builder()
                .totalScore(totalScore)
                .grade(grade)
                .schemaScore(schemaScore)
                .descriptionScore(descriptionScore)
                .pathsScore(pathsScore)
                .responseScore(responseScore)
                .exampleScore(exampleScore)
                .securityScore(securityScore)
                .bestPracticesScore(bestPracticesScore)
                .build();
    }

    public String getGrade(int score) {
        if(score > scoringConfig.getThresholds().getExcellent()){
            return "A";
        } else if(score > scoringConfig.getThresholds().getVeryGood()) {
            return "B";
        } else if(score > scoringConfig.getThresholds().getGood()) {
            return "C";
        } else if(score > scoringConfig.getThresholds().getFair()) {
            return "D";
        } else if(score > scoringConfig.getThresholds().getPoor()) {
            return "E";
        } else {
            return "F";
        }
    }
}
