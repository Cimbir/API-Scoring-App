package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.service.SpecLoaderService;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
public class ExampleScoringServiceTest {

    @Autowired
    private ScoringConfig scoringConfig;

    private ExampleScoringService exampleScoringService;
    private OpenAPI openAPI;

    @BeforeEach
    public void setUp() {
        scoringConfig.getValidation().getExample().setRequireRequestExamples(true);
        scoringConfig.getValidation().getExample().setRequireResponseExamples(true);
        scoringConfig.getValidation().getExample().setMinimumExampleCoverage(0.8);

        exampleScoringService = new ExampleScoringService(scoringConfig);
        SpecLoaderService specLoaderService = new SpecLoaderService();

        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        openAPI = specLoaderService.load(specLocation);
    }

    @Test
    public void testExampleScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        CategoryScore score = exampleScoringService.scoreCategory(emptySpec);
        assert score.score() == 0 : "Score should be 0 for empty spec";
    }

    @Test
    public void testExampleScoringWithNoRules() {
        scoringConfig.getValidation().getExample().setRequireRequestExamples(false);
        scoringConfig.getValidation().getExample().setRequireResponseExamples(false);

        CategoryScore score = exampleScoringService.scoreCategory(openAPI);

        assert score.score() == scoringConfig.getWeights().getExamplesAndSamples() :
                "Score should be greater than 0 when no rules are applied";
    }

    @Test
    public void testExampleScoringWithDefaultRules() {
        CategoryScore score = exampleScoringService.scoreCategory(openAPI);

        assert score.score() >
                scoringConfig.getWeights().getCategoryMinimumPercentage() * scoringConfig.getWeights().getExamplesAndSamples()
                : "Score should be greater than 0 with default rules";
    }
}
