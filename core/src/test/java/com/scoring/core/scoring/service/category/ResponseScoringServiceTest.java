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
public class ResponseScoringServiceTest {

    @Autowired
    private ScoringConfig scoringConfig;

    private ResponseScoringService responseScoringService;
    private OpenAPI openAPI;

    @BeforeEach
    public void setUp() {
        scoringConfig.getValidation().getResponse().setRequireSuccessResponses(true);
        scoringConfig.getValidation().getResponse().setRequireErrorResponses(true);
        scoringConfig.getValidation().getResponse().setRequireDefaultResponse(false);
        scoringConfig.getValidation().getResponse().setRequiredErrorCodes(List.of("400"));

        scoringConfig.getWeights().setSchemaAndTypes(15);

        responseScoringService = new ResponseScoringService(scoringConfig);
        SpecLoaderService specLoaderService = new SpecLoaderService();

        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        openAPI = specLoaderService.load(specLocation);
    }

    @Test
    public void testResponseScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        CategoryScore score = responseScoringService.scoreCategory(emptySpec);
        assert score.score() == 0 : "Score should be 0 for empty spec";
    }

    @Test
    public void testResponseScoringWithNoRules() {
        scoringConfig.getValidation().getResponse().setRequireSuccessResponses(false);
        scoringConfig.getValidation().getResponse().setRequireErrorResponses(false);
        scoringConfig.getValidation().getResponse().setRequireDefaultResponse(false);

        CategoryScore score = responseScoringService.scoreCategory(openAPI);

        assert score.score() == scoringConfig.getWeights().getResponseCodes() :
                "Score should be greater than 0 when no rules are applied";
    }

    @Test
    public void testResponseScoringWithDefaultRules() {
        CategoryScore score = responseScoringService.scoreCategory(openAPI);

        assert score.score() >
                scoringConfig.getWeights().getCategoryMinimumPercentage() * scoringConfig.getWeights().getResponseCodes()
                : "Score should be greater than 0 with default rules";
    }
}
