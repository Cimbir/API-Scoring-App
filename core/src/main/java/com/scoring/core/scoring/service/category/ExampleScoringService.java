package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.CategoryScoreData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExampleScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public ExampleScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getExamplesAndSamples();
        CategoryScoreData data = new CategoryScoreData();

        int totalMediaTypes = 0;
        int mediaTypesWithExamples = 0;

        if (spec.getPaths() != null) {
            for (String path : spec.getPaths().keySet()) {
                PathItem pathItem = spec.getPaths().get(path);
                for (Operation operation : pathItem.readOperations()) {
                    String operationId = operation.getOperationId() != null ?
                            operation.getOperationId() : "unknown";

                    // Check request body examples
                    if (scoringConfig.getValidation().getExample().isRequireRequestExamples() &&
                            operation.getRequestBody() != null &&
                            operation.getRequestBody().getContent() != null) {
                        totalMediaTypes += operation.getRequestBody().getContent().size();
                        mediaTypesWithExamples += checkRequestBodyExample(path, operationId, operation, data);
                    }

                    // Check response examples
                    if (scoringConfig.getValidation().getExample().isRequireResponseExamples() &&
                            operation.getResponses() != null) {
                        totalMediaTypes += (int) operation.getResponses().values().stream()
                                .filter(response -> response.getContent() != null)
                                .mapToInt(response -> response.getContent().size())
                                .sum();
                        mediaTypesWithExamples += checkResponseExample(path, operationId, operation, data);
                    }
                }
            }
        }

        if (totalMediaTypes > 0) {
            double exampleScore = (double) mediaTypesWithExamples / totalMediaTypes;
            data.setPoints((int) (maxPoints * exampleScore));

            if (exampleScore > scoringConfig.getValidation().getExample().getMinimumExampleCoverage()) {
                data.getStrengths().add(String.format(
                        "Good coverage of request/response examples: %d%%",
                        (int) (exampleScore * 100)));
            }
        } else {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("", "", "paths"))
                    .description("No request/response bodies found to evaluate")
                    .severity(CategoryScore.Severity.LOW)
                    .suggestion("Add operations with request/response bodies and include examples")
                    .build();
            data.getIssues().add(issue);
        }

        return CategoryScore.builder()
                .categoryName("Examples & Samples")
                .score(Math.max(0, data.getPoints()))
                .maxScore(maxPoints)
                .issues(data.getIssues())
                .strengths(data.getStrengths())
                .build();
    }

    private int checkRequestBodyExample(String path, String operationId, Operation operation, CategoryScoreData data) {
        int mediaTypesWithExamples = 0;
        for (String contentType : operation.getRequestBody().getContent().keySet()) {
            MediaType mediaType = operation.getRequestBody().getContent().get(contentType);

            if (hasExamples(mediaType)) {
                mediaTypesWithExamples++;
            } else {
                CategoryScore.Issue.Location location = new CategoryScore.Issue.Location(
                        path,
                        operationId,
                        "requestBody." + contentType
                );

                CategoryScore.Issue issue = CategoryScore.Issue.builder()
                        .location(location)
                        .description("Request body missing examples for content type: " + contentType)
                        .severity(CategoryScore.Severity.MEDIUM)
                        .suggestion("Add example or examples property to request body media type")
                        .build();

                data.getIssues().add(issue);
            }
        }
        return mediaTypesWithExamples;
    }

    private int checkResponseExample(String path, String operationId, Operation operation, CategoryScoreData data) {
        int mediaTypesWithExamples = 0;
        for (String responseCode : operation.getResponses().keySet()) {
            ApiResponse response = operation.getResponses().get(responseCode);
            if (response.getContent() != null) {
                for (String contentType : response.getContent().keySet()) {
                    MediaType mediaType = response.getContent().get(contentType);

                    if (hasExamples(mediaType)) {
                        mediaTypesWithExamples++;
                    } else {
                        CategoryScore.Issue.Location location = new CategoryScore.Issue.Location(
                                path,
                                operationId,
                                "responses." + responseCode + "." + contentType
                        );

                        CategoryScore.Issue issue = CategoryScore.Issue.builder()
                                .location(location)
                                .description("Response (" + responseCode + ") missing examples for content type: " + contentType)
                                .severity(CategoryScore.Severity.MEDIUM)
                                .suggestion("Add example or examples property to response media type")
                                .build();

                        data.getIssues().add(issue);
                    }
                }
            }
        }
        return mediaTypesWithExamples;
    }

    private boolean hasExamples(MediaType mediaType) {
        return (mediaType.getExample() != null) ||
                (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty());
    }
}
