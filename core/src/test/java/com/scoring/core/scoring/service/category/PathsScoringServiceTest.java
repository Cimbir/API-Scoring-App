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
import java.util.List;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
public class PathsScoringServiceTest {

    @Autowired
    private ScoringConfig scoringConfig;

    private PathsScoringService pathsScoringService;
    private OpenAPI openAPI;

    @BeforeEach
    public void setUp() {
        scoringConfig.getValidation().getPath().setEnforceNamingConventions(true);
        scoringConfig.getValidation().getPath().setEnforceCrudOperationConventions(true);
        scoringConfig.getValidation().getPath().setCheckForRedundantPaths(true);
        scoringConfig.getValidation().getPath().setAllowedNamingConventions(List.of(
                "kebab-case",
                "snake_case",
                "camelCase"
        ));
        scoringConfig.getValidation().getPath().setPathSimilarityThreshold(0.5);
        scoringConfig.getValidation().getPath().setPenaltyForNamingConventionMismatch(5);
        scoringConfig.getValidation().getPath().setPenaltyForMissingCrudOperations(5);
        scoringConfig.getValidation().getPath().setPenaltyForRedundantPaths(5);

        scoringConfig.getWeights().setSchemaAndTypes(15);

        pathsScoringService = new PathsScoringService(scoringConfig);
        SpecLoaderService specLoaderService = new SpecLoaderService();

        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        openAPI = specLoaderService.load(specLocation);
    }

    @Test
    public void testPathsScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        CategoryScore score = pathsScoringService.scoreCategory(emptySpec);
        assert score.score() == 0 : "Score should be 0 for empty spec";
    }

    @Test
    public void testPathsScoringWithNoRules() {
        scoringConfig.getValidation().getPath().setEnforceNamingConventions(false);
        scoringConfig.getValidation().getPath().setEnforceCrudOperationConventions(false);
        scoringConfig.getValidation().getPath().setCheckForRedundantPaths(false);

        CategoryScore score = pathsScoringService.scoreCategory(openAPI);
        System.out.println(score.toString());
        assert score.score() == scoringConfig.getWeights().getPathsAndOperations() :
                "Score should be greater than 0 when no rules are applied";
    }

    @Test
    public void testPathsScoringWithDefaultRules() {
        CategoryScore score = pathsScoringService.scoreCategory(openAPI);

        assert score.score() >
                scoringConfig.getWeights().getCategoryMinimumPercentage() * scoringConfig.getWeights().getPathsAndOperations()
                : "Score should be greater than 0 with default rules";
    }
}
