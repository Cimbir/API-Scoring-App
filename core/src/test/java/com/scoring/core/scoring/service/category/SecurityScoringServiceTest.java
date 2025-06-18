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
public class SecurityScoringServiceTest {

    @Autowired
    private ScoringConfig scoringConfig;

    private SecurityScoringService securityScoringService;
    private OpenAPI openAPI;

    @BeforeEach
    public void setUp() {
        scoringConfig.getValidation().getSecurity().setRequireSecuritySchemes(true);
        scoringConfig.getValidation().getSecurity().setRequireOperationLevelSecurity(true);
        scoringConfig.getValidation().getSecurity().setRequireGlobalSecurity(false);
        scoringConfig.getValidation().getSecurity().setRecommendedSecurityTypes(List.of("oauth2","apikey","http"));
        scoringConfig.getValidation().getSecurity().setPenaltyForWeakSecuritySchemes(5);
        scoringConfig.getValidation().getSecurity().setPenaltyForWeakOperationSecurity(5);
        scoringConfig.getValidation().getSecurity().setPenaltyForWeakGlobalSecurity(0);

        scoringConfig.getWeights().setSchemaAndTypes(10);

        securityScoringService = new SecurityScoringService(scoringConfig);
        SpecLoaderService specLoaderService = new SpecLoaderService();

        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        openAPI = specLoaderService.load(specLocation);
    }

    @Test
    public void testSecurityScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        CategoryScore score = securityScoringService.scoreCategory(emptySpec);
        assert score.score() == 0 : "Score should be 0 for empty spec";
    }

    @Test
    public void testSecurityScoringWithNoRules() {
        scoringConfig.getValidation().getSecurity().setRequireSecuritySchemes(false);
        scoringConfig.getValidation().getSecurity().setRequireOperationLevelSecurity(false);
        scoringConfig.getValidation().getSecurity().setRequireGlobalSecurity(false);

        CategoryScore score = securityScoringService.scoreCategory(openAPI);

        assert score.score() == scoringConfig.getWeights().getSecurity() :
                "Score should match the weight for schema and types when no rules are applied";
    }

    @Test
    public void testSecurityScoringWithDefaultRules() {
        CategoryScore score = securityScoringService.scoreCategory(openAPI);

        assert
                score.score() >
                        scoringConfig.getWeights().getCategoryMinimumPercentage() * scoringConfig.getWeights().getSecurity() :
                "Score should be greater than the minimum percentage of the schema and types weight";
    }
}
