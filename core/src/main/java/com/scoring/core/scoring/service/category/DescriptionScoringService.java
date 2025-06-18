package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.category.DescriptionData;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import static com.scoring.core.scoring.service.APIParserHelper.*;

@Service
public class DescriptionScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public DescriptionScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getDescriptionsAndDocumentation();
        DescriptionData data = new DescriptionData();
        data.setPoints(maxPoints);
        data.setSpec(spec);

        analyzeDocumentation(spec, data);

        summarize(data);

        return data.buildScore(maxPoints, "Descriptions & Documentation");
    }

    private void summarize(DescriptionData data) {
        if (data.getTotalElements() > 0) {
            double descriptionScore = 1.0 - ((double) data.getMissingDescriptions() / data.getTotalElements());
            data.setPoints((int) (data.getPoints() * descriptionScore));

            // Add summary issue if there are missing descriptions
            if (data.getMissingDescriptions() > 0) {
                CategoryScore.Severity overallSeverity = data.getMissingDescriptions() > data.getTotalElements() * 0.5 ?
                        CategoryScore.Severity.HIGH : CategoryScore.Severity.MEDIUM;

                data.getIssues().add(CategoryScore.Issue.builder()
                        .location("#")
                        .description(String.format("Documentation coverage issues: %d out of %d elements missing descriptions (%.1f%%)",
                                data.getMissingDescriptions(), data.getTotalElements(),
                                (double) data.getMissingDescriptions() / data.getTotalElements() * 100))
                        .severity(overallSeverity)
                        .suggestion("Add meaningful descriptions to all API elements for better developer experience")
                        .build());
            } else {
                data.getStrengths().add("All API elements have proper descriptions");
            }

            // Add specific strengths based on coverage
            if (data.getOperationsWithDescriptions() == data.getTotalOperations() && data.getTotalOperations() > 0) {
                data.getStrengths().add("All operations have descriptions");
            }
            if (data.getParametersWithDescriptions() == data.getTotalParameters() && data.getTotalParameters() > 0) {
                data.getStrengths().add("All parameters have descriptions");
            }
            if (data.getResponsesWithDescriptions() == data.getTotalResponses() && data.getTotalResponses() > 0) {
                data.getStrengths().add("All responses have descriptions");
            }
            if (data.getRequestBodiesWithDescriptions() == data.getTotalRequestBodies() && data.getTotalRequestBodies() > 0) {
                data.getStrengths().add("All request bodies have descriptions");
            }

        } else {
            data.setPoints(0);
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("No API operations found to evaluate")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define API paths and operations with proper documentation")
                    .build());
        }
    }

    private void analyzeDocumentation(OpenAPI spec, DescriptionData data) {
        // Check API-level description
        if (spec.getInfo() != null) {
            data.setTotalElements(data.getTotalElements() + 1);
            if(scoringConfig.getValidation().getDescription().isRequireGeneralDescription() &&
                isInvalidDescription(spec.getInfo().getDescription())) {
                data.setMissingDescriptions(data.getMissingDescriptions() + 1);
                data.getIssues().add(CategoryScore.Issue.builder()
                        .location("#/info")
                        .description("API info lacks description")
                        .severity(CategoryScore.Severity.MEDIUM)
                        .suggestion("Add a clear description of what your API does in the info section")
                        .build());
            }
        }

        // Check operations
        if(scoringConfig.getValidation().getDescription().isRequireOperationDescriptions()){
            goOverOperations(spec, (path, operationId, operation, d) -> {
                d.setTotalElements(d.getTotalElements() + 1);
                d.setTotalOperations(d.getTotalOperations() + 1);
                if (isInvalidDescription(operation.getDescription()) && isInvalidDescription(operation.getSummary())) {
                    d.setMissingDescriptions(d.getMissingDescriptions() + 1);
                    data.getIssues().add(
                            CategoryScore.Issue.builder()
                                    .location(String.format("#/paths/%s/operations/%s", path, operationId))
                                    .description(String.format("Operation '%s' on path '%s' lacks description", operationId, path))
                                    .severity(CategoryScore.Severity.MEDIUM)
                                    .suggestion("Add a description or summary explaining what this operation does")
                                    .build());
                } else {
                    d.setOperationsWithDescriptions(d.getOperationsWithDescriptions() + 1);
                }
            }, data);
        }

        // Check parameters
        if(scoringConfig.getValidation().getDescription().isRequireParameterDescriptions()){
            goOverParameters(spec, (path, operationId, parameter, d) -> {
                d.setTotalResponses(d.getTotalResponses() + 1);
                d.setTotalParameters(d.getTotalParameters() + 1);
                if (isInvalidDescription(parameter.getDescription()) &&
                        doesReferenceExist(d.getSpec(), parameter.get$ref())) {
                    d.setMissingDescriptions(d.getMissingDescriptions() + 1);
                    d.getIssues().add(
                            CategoryScore.Issue.builder()
                                    .location(String.format("#/paths/%s/operations/%s/parameters/%s", path, operationId, parameter.getName()))
                                    .description(String.format("Parameter '%s' in operation '%s' on path '%s' lacks description", parameter.getName(), operationId, path))
                                    .severity(CategoryScore.Severity.LOW)
                                    .suggestion(String.format("Add a description explaining the purpose and expected format of parameter '%s'", parameter.getName()))
                                    .build());
                } else {
                    d.setParametersWithDescriptions(d.getParametersWithDescriptions() + 1);
                }
            }, data);
        }

        // Check request body
        if(scoringConfig.getValidation().getDescription().isRequireRequestDescriptions()){
            goOverOperations(spec, (path, operationId, operation, d) -> {
                if (operation.getRequestBody() != null) {
                    d.setTotalParameters(d.getTotalParameters() + 1);
                    d.setTotalRequestBodies(d.getTotalRequestBodies() + 1);
                    if (isInvalidDescription(operation.getRequestBody().getDescription())) {
                        d.setMissingDescriptions(d.getMissingDescriptions() + 1);
                        d.getIssues().add(
                                CategoryScore.Issue.builder()
                                        .location(String.format("#/paths/%s/operations/%s/requestBody", path, operationId))
                                        .description(String.format("Request body in operation '%s' on path '%s' lacks description", operationId, path))
                                        .severity(CategoryScore.Severity.LOW)
                                        .suggestion("Add a description explaining the expected request body structure and purpose")
                                        .build());
                    } else {
                        d.setRequestBodiesWithDescriptions(d.getRequestBodiesWithDescriptions() + 1);
                    }
                }
            }, data);
        }

        // Check responses
        if (scoringConfig.getValidation().getDescription().isRequireResponseDescriptions()) {
            goOverResponses(spec, (path, operationId, responseCode, response, d) -> {
                d.setTotalElements(d.getTotalElements() + 1);
                d.setTotalResponses(d.getTotalResponses() + 1);
                if (isInvalidDescription(response.getDescription()) &&
                        doesReferenceExist(d.getSpec(), response.get$ref())) {
                    d.setMissingDescriptions(d.getMissingDescriptions() + 1);
                    d.getIssues().add(
                            CategoryScore.Issue.builder()
                                    .location(String.format("#/paths/%s/operations/%s/responses/%s", path, operationId, responseCode))
                                    .description(String.format("Response '%s' lacks description", responseCode))
                                    .severity(CategoryScore.Severity.LOW)
                                    .suggestion("Add a description explaining what this response means and when it occurs")
                                    .build());
                } else {
                    d.setResponsesWithDescriptions(d.getResponsesWithDescriptions() + 1);
                }
            }, data);
        }

        // Check schemas
        if(scoringConfig.getValidation().getDescription().isRequireSchemaDescriptions()){
            goOverSchemas(spec, (schemaName, schema, d) -> {
                d.setTotalElements(d.getTotalElements() + 1);
                d.setTotalSchemas(d.getTotalSchemas() + 1);
                if (isInvalidDescription(schema.getDescription()) &&
                        doesReferenceExist(d.getSpec(), schema.get$ref())) {
                    d.setMissingDescriptions(d.getMissingDescriptions() + 1);
                    d.getIssues().add(
                            CategoryScore.Issue.builder()
                                    .location(String.format("#/components/schemas/%s", schemaName))
                                    .description(String.format("Schema '%s' lacks description", schemaName))
                                    .severity(CategoryScore.Severity.LOW)
                                    .suggestion("Add a description explaining the purpose and structure of this schema")
                                    .build());
                } else {
                    d.setSchemasWithDescriptions(d.getSchemasWithDescriptions() + 1);
                }
            }, data);
        }
    }

    private boolean isInvalidDescription(String description) {
        return
                description == null ||
                description.trim().isEmpty() ||
                description.length() < scoringConfig.getValidation().getDescription().getMinimumDescriptionLength();
    }
}
