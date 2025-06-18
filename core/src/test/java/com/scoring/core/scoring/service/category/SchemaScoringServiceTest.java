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
public class SchemaScoringServiceTest {

    @Autowired
    private ScoringConfig scoringConfig;

    private SchemaScoringService schemaScoringService;
    private OpenAPI openAPI;

    @BeforeEach
    public void setUp() {
        scoringConfig.getValidation().getSchema().setRequireSchemaComponents(true);
        scoringConfig.getValidation().getSchema().setRequireRequestBodySchema(true);
        scoringConfig.getValidation().getSchema().setRequireResponseBodySchema(true);
        scoringConfig.getValidation().getSchema().setAllowedGenericSchema(false);
        scoringConfig.getValidation().getSchema().setRequiredDataTypes(
                List.of("string", "integer", "boolean", "number", "array", "object"));
        scoringConfig.getValidation().getSchema().setPenaltyForMissingSchema(5);

        scoringConfig.getWeights().setSchemaAndTypes(20);

        schemaScoringService = new SchemaScoringService(scoringConfig);
        SpecLoaderService specLoaderService = new SpecLoaderService();

        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        openAPI = specLoaderService.load(specLocation);
    }

    @Test
    public void testSchemaScoringEmptySpec() {
        OpenAPI emptySpec = new OpenAPI();

        CategoryScore score = schemaScoringService.scoreCategory(emptySpec);
        assert score.score() == 0 : "Score should be 0 for empty spec";
    }

    @Test
    public void testSchemaScoringWithNoRules() {
        scoringConfig.getValidation().getSchema().setRequireSchemaComponents(false);
        scoringConfig.getValidation().getSchema().setRequireRequestBodySchema(false);
        scoringConfig.getValidation().getSchema().setRequireResponseBodySchema(false);
        scoringConfig.getValidation().getSchema().setAllowedGenericSchema(true);

        CategoryScore score = schemaScoringService.scoreCategory(openAPI);

        assert score.score() == scoringConfig.getWeights().getSchemaAndTypes() :
            "Score should match the weight for schema and types when no rules are applied";
    }

    @Test
    public void testSchemaScoringWithDefaultRules() {
        CategoryScore score = schemaScoringService.scoreCategory(openAPI);

        assert
                score.score() >
                scoringConfig.getWeights().getCategoryMinimumPercentage() * scoringConfig.getWeights().getSchemaAndTypes() :
                "Score should be greater than the minimum percentage of the schema and types weight";
    }
}
