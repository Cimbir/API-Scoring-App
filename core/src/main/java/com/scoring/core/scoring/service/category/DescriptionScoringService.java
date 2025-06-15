package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.CategoryScoreData;
import com.scoring.core.scoring.model.category.description.DocumentationStats;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

// TODO: Add schema descriptions too

@Service
public class DescriptionScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public DescriptionScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getDescriptionsAndDocumentation();
        CategoryScoreData data = new CategoryScoreData();

        DocumentationStats stats = analyzeDocumentation(spec, data);

        if (stats.totalElements() > 0) {
            double descriptionScore = 1.0 - ((double) stats.missingDescriptions() / stats.totalElements());
            data.setPoints((int) (maxPoints * descriptionScore));

            // Add summary issue if there are missing descriptions
            if (stats.missingDescriptions() > 0) {
                CategoryScore.Severity overallSeverity = stats.missingDescriptions() > stats.totalElements() * 0.5 ?
                        CategoryScore.Severity.HIGH : CategoryScore.Severity.MEDIUM;

                data.getIssues().add(CategoryScore.Issue.builder()
                        .location(new CategoryScore.Issue.Location("overall", "all", "documentation"))
                        .description(String.format("Documentation coverage issues: %d out of %d elements missing descriptions (%.1f%%)",
                                stats.missingDescriptions(), stats.totalElements(),
                                (double) stats.missingDescriptions() / stats.totalElements() * 100))
                        .severity(overallSeverity)
                        .suggestion("Add meaningful descriptions to all API elements for better developer experience")
                        .build());
            } else {
                data.getStrengths().add("All API elements have proper descriptions");
            }

            // Add specific strengths based on coverage
            if (stats.operationsWithDescriptions() == stats.totalOperations() && stats.totalOperations() > 0) {
                data.getStrengths().add("All operations have descriptions");
            }
            if (stats.parametersWithDescriptions() == stats.totalParameters() && stats.totalParameters() > 0) {
                data.getStrengths().add("All parameters have descriptions");
            }
            if (stats.responsesWithDescriptions() == stats.totalResponses() && stats.totalResponses() > 0) {
                data.getStrengths().add("All responses have descriptions");
            }
            if (stats.requestBodiesWithDescriptions() == stats.totalRequestBodies() && stats.totalRequestBodies() > 0) {
                data.getStrengths().add("All request bodies have descriptions");
            }

        } else {
            data.setPoints(0);
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("paths", "none", "root"))
                    .description("No API operations found to evaluate")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define API paths and operations with proper documentation")
                    .build());
        }

        return CategoryScore.builder()
                .maxScore(maxPoints)
                .score(Math.max(0, data.getPoints()))
                .categoryName("Descriptions & Documentation")
                .issues(data.getIssues())
                .strengths(data.getStrengths())
                .build();
    }

    private DocumentationStats analyzeDocumentation(OpenAPI spec, CategoryScoreData data) {
        int totalElements = 0;
        int missingDescriptions = 0;
        int totalPaths = 0;
        int pathsWithDescriptions = 0;
        int totalOperations = 0;
        int operationsWithDescriptions = 0;
        int totalParameters = 0;
        int parametersWithDescriptions = 0;
        int totalResponses = 0;
        int responsesWithDescriptions = 0;
        int totalRequestBodies = 0;
        int requestBodiesWithDescriptions = 0;

        // Check API-level description
        if (spec.getInfo() != null) {
            totalElements++;
            if (isInvalidDescription(spec.getInfo().getDescription())) {
                missingDescriptions++;
                data.getIssues().add(CategoryScore.Issue.builder()
                        .location(new CategoryScore.Issue.Location("", "", "info"))
                        .description("API info lacks description")
                        .severity(CategoryScore.Severity.MEDIUM)
                        .suggestion("Add a clear description of what your API does in the info section")
                        .build());
            }
        }

        if (spec.getPaths() != null) {
            for (Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                // TODO: Maybe also check summary?
                totalElements++;
                totalPaths++;
                if(isInvalidDescription(pathItem.getDescription())) {
                    missingDescriptions++;
                    data.getIssues().add(CategoryScore.Issue.builder()
                            .location(new CategoryScore.Issue.Location(path, "", "path"))
                            .description("Path '" + path + "' lacks description")
                            .severity(CategoryScore.Severity.MEDIUM)
                            .suggestion("Add a description explaining what this path does")
                            .build());
                }else{
                    pathsWithDescriptions++;
                }

                for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : pathItem.readOperationsMap().entrySet()) {
                    PathItem.HttpMethod method = operationEntry.getKey();
                    Operation operation = operationEntry.getValue();
                    String operationId = operation.getOperationId() != null ? operation.getOperationId() : method.toString();

                    // Check operation description
                    if (
                            scoringConfig.getValidation().getDescription().isRequireOperationDescriptions()
                    ){
                        totalElements++;
                        totalOperations++;
                        if (isInvalidDescription(operation.getDescription()) && isInvalidDescription(operation.getSummary())) {
                            missingDescriptions++;
                            data.getIssues().add(
                                    CategoryScore.Issue.builder()
                                            .location(new CategoryScore.Issue.Location(path, operationId, "operation"))
                                            .description(String.format("Operation '%s' on path '%s' lacks description", operationId, path))
                                            .severity(CategoryScore.Severity.MEDIUM)
                                            .suggestion("Add a description or summary explaining what this operation does")
                                            .build());
                        } else {
                            operationsWithDescriptions++;
                        }
                    }

                    // Check parameters
                    if (
                            operation.getParameters() != null &&
                            scoringConfig.getValidation().getDescription().isRequireParameterDescriptions()
                    ) {
                        for (Parameter param : operation.getParameters()) {
                            totalElements++;
                            totalParameters++;
                            if (isInvalidDescription(param.getDescription())) {
                                missingDescriptions++;
                                data.getIssues().add(
                                        CategoryScore.Issue.builder()
                                        .location(new CategoryScore.Issue.Location(path, operationId, "parameter/" + param.getName()))
                                        .description(String.format("Parameter '%s' in operation '%s' on path '%s' lacks description", param.getName(), operationId, path))
                                        .severity(CategoryScore.Severity.LOW)
                                        .suggestion(String.format("Add a description explaining the purpose and expected format of parameter '%s'", param.getName()))
                                        .build());
                            } else {
                                parametersWithDescriptions++;
                            }
                        }
                    }

                    // Check request body
                    if (
                            operation.getRequestBody() != null &&
                            scoringConfig.getValidation().getDescription().isRequireRequestBodyDescriptions()
                    ) {
                        totalElements++;
                        totalRequestBodies++;
                        if (isInvalidDescription(operation.getRequestBody().getDescription())) {
                            missingDescriptions++;
                            data.getIssues().add(
                                    CategoryScore.Issue.builder()
                                            .location(new CategoryScore.Issue.Location(path, operationId, "requestBody"))
                                            .description(String.format("Request body in operation '%s' on path '%s' lacks description", operationId, path))
                                            .severity(CategoryScore.Severity.LOW)
                                            .suggestion("Add a description explaining the expected request body structure and purpose")
                                            .build());
                        } else {
                            requestBodiesWithDescriptions++;
                        }
                    }

                    // Check responses
                    if (
                            operation.getResponses() != null &&
                            scoringConfig.getValidation().getDescription().isRequireResponseDescriptions()
                    ) {
                        for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                            String responseCode = responseEntry.getKey();
                            ApiResponse response = responseEntry.getValue();

                            totalElements++;
                            totalResponses++;
                            if (isInvalidDescription(response.getDescription())) {
                                missingDescriptions++;
                                data.getIssues().add(
                                        CategoryScore.Issue.builder()
                                                .location(new CategoryScore.Issue.Location(path, operationId, "responses/" + responseCode))
                                                .description(String.format("Response '%s' lacks description", responseCode))
                                                .severity(CategoryScore.Severity.LOW)
                                                .suggestion("Add a description explaining what this response means and when it occurs")
                                                .build());
                            } else {
                                responsesWithDescriptions++;
                            }
                        }
                    }
                }
            }
        }

        return new DocumentationStats(
                totalElements, missingDescriptions,
                totalPaths, pathsWithDescriptions,
                totalOperations, operationsWithDescriptions,
                totalParameters, parametersWithDescriptions,
                totalResponses, responsesWithDescriptions,
                totalRequestBodies, requestBodiesWithDescriptions
        );
    }

    private boolean isInvalidDescription(String description) {
        return
                description == null ||
                description.trim().isEmpty() ||
                description.length() < scoringConfig.getValidation().getDescription().getMinimumDescriptionLength();
    }
}
