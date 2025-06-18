package com.scoring.core.scoring.service;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
public class APIScoringServiceTest {

    @Autowired
    private APIScoringService apiScoringService;

    @Autowired
    private SpecLoaderService specLoaderService;

    @Test
    public void testAPIScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        var score = apiScoringService.score(emptySpec);
        assert score.totalScore() == 0 : "Total score should be 0 for empty spec";
        assert score.grade().equals("F") : "Grade should be F for empty spec";
    }

    @Test
    public void testAPIScoringScoreDefaultRules() {
        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        OpenAPI openAPI = specLoaderService.load(specLocation);

        var score = apiScoringService.score(openAPI);

        assert score.totalScore() > 0 : "Total score should be greater than 0 for valid spec";
        assert score.grade().equals("A") || score.grade().equals("B") || score.grade().equals("C") :
                "Grade should be A, B, or C for valid spec";

        // Validate individual category scores
        assert score.schemaScore().score() >= 0 : "Schema score should be non-negative";
        assert score.descriptionScore().score() >= 0 : "Description score should be non-negative";
        assert score.pathsScore().score() >= 0 : "Paths score should be non-negative";
        assert score.responseScore().score() >= 0 : "Response score should be non-negative";
        assert score.exampleScore().score() >= 0 : "Example score should be non-negative";
        assert score.securityScore().score() >= 0 : "Security score should be non-negative";
        assert score.bestPracticesScore().score() >= 0 : "Best practices score should be non-negative";
    }

    @Test
    public void testAPIScoringScoreTwitterDefaultRules() {
        String specLocation = "https://snowcait.github.io/twitter-swagger-ui/openapi.v2.json";
        OpenAPI openAPI = specLoaderService.load(specLocation);

        var score = apiScoringService.score(openAPI);

        assert score.totalScore() > 0 : "Total score should be greater than 0 for valid spec";
        assert score.grade().equals("A") || score.grade().equals("B") || score.grade().equals("C") :
                "Grade should be A, B, or C for valid spec";

        // Validate individual category scores
        assert score.schemaScore().score() >= 0 : "Schema score should be non-negative";
        assert score.descriptionScore().score() >= 0 : "Description score should be non-negative";
        assert score.pathsScore().score() >= 0 : "Paths score should be non-negative";
        assert score.responseScore().score() >= 0 : "Response score should be non-negative";
        assert score.exampleScore().score() >= 0 : "Example score should be non-negative";
        assert score.securityScore().score() >= 0 : "Security score should be non-negative";
        assert score.bestPracticesScore().score() >= 0 : "Best practices score should be non-negative";
    }
}
