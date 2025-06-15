package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.CategoryScoreData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ResponseScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public ResponseScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getResponseCodes();
        CategoryScoreData data = new CategoryScoreData();

        int totalOperations = 0;
        int operationsWithProperCodes = 0;

        if (spec.getPaths() != null) {
            for (String path : spec.getPaths().keySet()) {
                PathItem pathItem = spec.getPaths().get(path);
                for (Operation operation : pathItem.readOperations()) {
                    totalOperations++;
                    String operationId = operation.getOperationId() != null ?
                            operation.getOperationId() : "unknown";

                    CategoryScore.Issue.Location location = new CategoryScore.Issue.Location(
                            path,
                            operationId,
                            "responses"
                    );

                    if (operation.getResponses() != null) {
                        Set<String> responseCodes = operation.getResponses().keySet();

                        // check if the operation has required response codes
                        boolean hasRequiredSuccessCodes = responseCodes.containsAll(
                                scoringConfig.getValidation().getResponse().getRequiredSuccessCodes());
                        boolean hasRequiredErrorCodes = responseCodes.containsAll(
                                scoringConfig.getValidation().getResponse().getRequiredErrorCodes());
                        boolean hasDefaultCode = responseCodes.contains("default");

                        // Check if the operation passes the validation criteria
                        boolean passedSuccessCodes =
                                !scoringConfig.getValidation().getResponse().isRequireSuccessResponses() ||
                                hasRequiredSuccessCodes;
                        boolean passedErrorCodes =
                                !scoringConfig.getValidation().getResponse().isRequireErrorResponses() ||
                                hasRequiredErrorCodes;
                        boolean passedDefaultCode =
                                !scoringConfig.getValidation().getResponse().isRequireDefaultResponse() ||
                                hasDefaultCode;

                        if (passedSuccessCodes && passedErrorCodes && passedDefaultCode) {
                            operationsWithProperCodes++;
                        } else {
                            // Operation has issues with response codes
                            if(!passedSuccessCodes) {
                                data.getIssues().add(CategoryScore.Issue.builder()
                                        .location(location)
                                        .description(
                                                String.format(
                                                        "Operation missing required success (2xx) response codes: %s",
                                                        String.join(", ", scoringConfig.getValidation().getResponse().getRequiredSuccessCodes())))
                                        .severity(CategoryScore.Severity.MEDIUM)
                                        .suggestion("Define appropriate success (2xx) response codes")
                                        .build());
                            }
                            if(!passedErrorCodes) {
                                data.getIssues().add(CategoryScore.Issue.builder()
                                        .location(location)
                                        .description(
                                                String.format(
                                                        "Operation missing required error (4xx/5xx) response codes: %s",
                                                        String.join(", ", scoringConfig.getValidation().getResponse().getRequiredErrorCodes())))
                                        .severity(CategoryScore.Severity.MEDIUM)
                                        .suggestion("Define appropriate error (4xx/5xx) response codes")
                                        .build());
                            }
                            if(!passedDefaultCode) {
                                data.getIssues().add(CategoryScore.Issue.builder()
                                        .location(location)
                                        .description("Operation missing required default response code")
                                        .severity(CategoryScore.Severity.MEDIUM)
                                        .suggestion("Define a default response code for unexpected cases")
                                        .build());
                            }
                        }
                    } else {
                        // No responses defined at all
                        CategoryScore.Issue issue = CategoryScore.Issue.builder()
                                .location(location)
                                .description("Operation has no response codes defined")
                                .severity(CategoryScore.Severity.HIGH)
                                .suggestion("Define response codes including success (2xx) and error (4xx/5xx) codes")
                                .build();

                        data.getIssues().add(issue);
                    }
                }
            }
        }

        if (totalOperations > 0) {
            double responseScore = (double) operationsWithProperCodes / totalOperations;
            data.setPoints((int) (maxPoints * responseScore));

            if (operationsWithProperCodes == totalOperations) {
                data.getStrengths().add("All operations have appropriate response codes");
            }
        } else {
            data.setPoints(0);
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("", "", "paths"))
                    .description("No operations found to evaluate")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Add API operations with proper response code definitions")
                    .build();
            data.getIssues().add(issue);
        }

        return CategoryScore.builder()
                .categoryName("Response Codes")
                .score(Math.max(0, data.getPoints()))
                .maxScore(maxPoints)
                .issues(data.getIssues())
                .strengths(data.getStrengths())
                .build();
    }

}
