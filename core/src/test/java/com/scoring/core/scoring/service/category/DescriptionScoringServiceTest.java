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
public class DescriptionScoringServiceTest {

    @Autowired
    private ScoringConfig scoringConfig;

    private DescriptionScoringService descriptionScoringService;
    private OpenAPI openAPI;

    @BeforeEach
    public void setUp() {
        scoringConfig.getValidation().getDescription().setRequireOperationDescriptions(true);
        scoringConfig.getValidation().getDescription().setRequireParameterDescriptions(true);
        scoringConfig.getValidation().getDescription().setRequireOperationDescriptions(true);
        scoringConfig.getValidation().getDescription().setRequireResponseDescriptions(true);
        scoringConfig.getValidation().getDescription().setRequireRequestDescriptions(true);
        scoringConfig.getValidation().getDescription().setRequireSchemaDescriptions(false);
        scoringConfig.getValidation().getDescription().setMinimumDescriptionLength(10);

        scoringConfig.getWeights().setSchemaAndTypes(20);

        descriptionScoringService = new DescriptionScoringService(scoringConfig);
        SpecLoaderService specLoaderService = new SpecLoaderService();

        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        openAPI = specLoaderService.load(specLocation);
    }

    @Test
    public void testDescriptionScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        CategoryScore score = descriptionScoringService.scoreCategory(emptySpec);
        assert score.score() == 0 : "Score should be 0 for empty spec";
    }

    @Test
    public void testDescriptionScoringWithNoRules() {
        scoringConfig.getValidation().getDescription().setRequireGeneralDescription(false);
        scoringConfig.getValidation().getDescription().setRequireParameterDescriptions(false);
        scoringConfig.getValidation().getDescription().setRequireOperationDescriptions(false);
        scoringConfig.getValidation().getDescription().setRequireResponseDescriptions(false);
        scoringConfig.getValidation().getDescription().setRequireRequestDescriptions(false);
        scoringConfig.getValidation().getDescription().setMinimumDescriptionLength(0);

        CategoryScore score = descriptionScoringService.scoreCategory(openAPI);
        assert score.score() == scoringConfig.getWeights().getDescriptionsAndDocumentation() :
                "Score should be greater than 0 when no rules are applied";
    }

    @Test
    public void testDescriptionScoringWithDefaultRules() {
        CategoryScore score = descriptionScoringService.scoreCategory(openAPI);

        assert score.score() >
                scoringConfig.getWeights().getCategoryMinimumPercentage() * scoringConfig.getWeights().getDescriptionsAndDocumentation()
                : "Score should be greater than 0 with default rules";
    }
}
