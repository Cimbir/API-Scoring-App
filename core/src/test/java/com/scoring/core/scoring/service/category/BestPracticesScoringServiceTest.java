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
public class BestPracticesScoringServiceTest {

    @Autowired
    private ScoringConfig scoringConfig;

    private BestPracticesScoringService bestPracticesScoringService;
    private OpenAPI openAPI;

    @BeforeEach
    public void setUp() {
        scoringConfig.getValidation().getBestPractice().setRequireTags(true);
        scoringConfig.getValidation().getBestPractice().setRequireVersioning(true);
        scoringConfig.getValidation().getBestPractice().setRequireServersArray(true);
        scoringConfig.getValidation().getBestPractice().setRequireComponentReuse(true);
        scoringConfig.getValidation().getBestPractice().setRequireOperationIds(true);
        scoringConfig.getValidation().getBestPractice().setMinimumReusableComponents(2);

        scoringConfig.getWeights().setSchemaAndTypes(10);

        bestPracticesScoringService = new BestPracticesScoringService(scoringConfig);
        SpecLoaderService specLoaderService = new SpecLoaderService();

        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        openAPI = specLoaderService.load(specLocation);
    }

    @Test
    public void testBestPracticeScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        CategoryScore score = bestPracticesScoringService.scoreCategory(emptySpec);
        assert score.score() == 0 : "Score should be 0 for empty spec";
    }

    @Test
    public void testBestPracticeScoringWithNoRules() {
        scoringConfig.getValidation().getBestPractice().setRequireTags(false);
        scoringConfig.getValidation().getBestPractice().setRequireVersioning(false);
        scoringConfig.getValidation().getBestPractice().setRequireServersArray(false);
        scoringConfig.getValidation().getBestPractice().setRequireComponentReuse(false);
        scoringConfig.getValidation().getBestPractice().setRequireOperationIds(false);

        CategoryScore score = bestPracticesScoringService.scoreCategory(openAPI);

        assert score.score() == scoringConfig.getWeights().getBestPractices() : "Should be full score with no rules";
    }

    @Test
    public void testBestPracticeScoringWithDefaultRules() {
        CategoryScore score = bestPracticesScoringService.scoreCategory(openAPI);

        assert
                score.score() >
                        scoringConfig.getWeights().getCategoryMinimumPercentage() * scoringConfig.getWeights().getBestPractices() :
                "Score should be above minimum percentage of max score with default rules";
    }
}
