package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.CategoryScoreData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

// TODO: validate schema types

@Service
public class SchemaScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public SchemaScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        CategoryScoreData data = new CategoryScoreData();
        data.setPoints(scoringConfig.getWeights().getSchemaAndTypes());

        // Check if components/schemas section is defined
        if (scoringConfig.getValidation().getSchema().isRequireSchemaComponents()){
            checkComponentsSchemas(spec, data);
        }

        // Check for proper data types in paths
        if (spec.getPaths() != null) {
            int schemaIssues = 0;
            int totalSchemas = 0;

            for (Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : pathItem.readOperationsMap().entrySet()) {
                    PathItem.HttpMethod method = operationEntry.getKey();
                    Operation operation = operationEntry.getValue();
                    String operationId = operation.getOperationId() != null ? operation.getOperationId() : method.toString();

                    // Check request body schemas
                    totalSchemas += operation.getRequestBody().getContent().size();
                    schemaIssues += checkRequestBodySchema(path, operation, operationId, data);

                    // Check response schemas
                    totalSchemas += (int) operation.getResponses().values().stream()
                            .filter(response -> response.getContent() != null)
                            .mapToInt(response -> response.getContent().size())
                            .sum();
                    schemaIssues += checkResponseBodySchema(path, operation, operationId, data);
                }
            }

            if (totalSchemas > 0) {
                // Calculate score reduction based on schema issues
                double schemaQualityRatio = 1.0 - ((double) schemaIssues / totalSchemas);
                data.setPoints((int) (data.getPoints() * schemaQualityRatio));

                if (schemaIssues == 0) {
                    data.getStrengths().add("All schemas have proper data types");
                } else {
                    // Additional summary for overall schema quality
                    CategoryScore.Severity overallSeverity = schemaIssues > totalSchemas * 0.5 ?
                            CategoryScore.Severity.HIGH : CategoryScore.Severity.MEDIUM;

                    data.getIssues().add(CategoryScore.Issue.builder()
                            .location(new CategoryScore.Issue.Location("overall", "all", "schemas"))
                            .description(String.format("Schema quality issues detected in %d out of %d schemas (%.1f%%)",
                                    schemaIssues, totalSchemas, (double) schemaIssues / totalSchemas * 100))
                            .severity(overallSeverity)
                            .suggestion("Review and improve schema definitions to ensure proper typing")
                            .build());
                }
            }
        }

        return CategoryScore.builder()
                .maxScore(scoringConfig.getWeights().getSchemaAndTypes())
                .score(Math.max(0, data.getPoints()))
                .categoryName("Schema & Types")
                .issues(data.getIssues())
                .strengths(data.getStrengths())
                .build();
    }

    private void checkComponentsSchemas(OpenAPI spec, CategoryScoreData data) {
        if (
                spec.getComponents() == null ||
                spec.getComponents().getSchemas() == null ||
                spec.getComponents().getSchemas().isEmpty()
        ) {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getSchema().getPenaltyForMissingSchema());
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("components", "schemas", "root"))
                    .description("No schema components defined")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define reusable schema components in the components/schemas section")
                    .build());
        } else {
            data.getStrengths().add("Schema components are defined");
        }
    }

    private Integer checkRequestBodySchema(
            String path,
            Operation operation,
            String operationId,
            CategoryScoreData data
    )
    {
        int schemaIssues = 0;
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            for (Map.Entry<String, MediaType> mediaEntry : operation.getRequestBody().getContent().entrySet()) {
                String mediaTypeName = mediaEntry.getKey();
                MediaType mediaType = mediaEntry.getValue();

                CategoryScore.Issue.Location location = new CategoryScore.Issue.Location(path, operationId,
                        "requestBody/" + mediaTypeName);

                if (mediaType.getSchema() == null) {
                    schemaIssues++;
                    data.getIssues().add(
                            CategoryScore.Issue.builder()
                            .location(location)
                            .description("Missing schema definition")
                            .severity(CategoryScore.Severity.HIGH)
                            .suggestion("Define a proper schema for the request body")
                            .build());
                } else if (
                        !scoringConfig.getValidation().getSchema().isAllowGenericObjects() &&
                        isGenericObjectSchema(mediaType.getSchema())) {
                    schemaIssues++;
                    data.getIssues().add(
                            CategoryScore.Issue.builder()
                            .location(location)
                            .description("Generic object schema without properties")
                            .severity(CategoryScore.Severity.MEDIUM)
                            .suggestion("Define specific properties for the object schema or use a $ref to a component schema")
                            .build());
                }
            }
        }
        return schemaIssues;
    }

    private Integer checkResponseBodySchema(
            String path,
            Operation operation,
            String operationId,
            CategoryScoreData data
    ) {
        int schemaIssues = 0;
        if (operation.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                String responseCode = responseEntry.getKey();
                ApiResponse response = responseEntry.getValue();

                if (response.getContent() != null) {
                    for (Map.Entry<String, MediaType> mediaEntry : response.getContent().entrySet()) {
                        String mediaTypeName = mediaEntry.getKey();
                        MediaType mediaType = mediaEntry.getValue();

                        CategoryScore.Issue.Location location = new CategoryScore.Issue.Location(path, operationId,
                                "responses/" + responseCode + "/" + mediaTypeName);

                        if (mediaType.getSchema() == null) {
                            schemaIssues++;
                            data.getIssues().add(
                                    CategoryScore.Issue.builder()
                                    .location(location)
                                    .description("Missing schema definition")
                                    .severity(CategoryScore.Severity.HIGH)
                                    .suggestion("Define a proper schema for the response body")
                                    .build());
                        } else if (
                                !scoringConfig.getValidation().getSchema().isAllowGenericObjects() &&
                                isGenericObjectSchema(mediaType.getSchema())) {
                            schemaIssues++;
                            data.getIssues().add(
                                    CategoryScore.Issue.builder()
                                    .location(location)
                                    .description("Generic object schema without properties")
                                    .severity(CategoryScore.Severity.MEDIUM)
                                    .suggestion("Define specific properties for the object schema or use a $ref to a component schema")
                                    .build());
                        }
                    }
                }
            }
        }
        return schemaIssues;
    }

    private boolean isGenericObjectSchema(Schema<?> schema) {
        return schema.getType() == null ||
                ("object".equals(schema.getType()) &&
                        (schema.getProperties() == null || schema.getProperties().isEmpty()));
    }

}
