package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.category.ResponseData;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.scoring.core.scoring.service.APIParserHelper.goOverOperations;

@Service
public class ResponseScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public ResponseScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getResponseCodes();
        ResponseData data = new ResponseData();
        data.setPoints(maxPoints);

        checkResponses(spec, data);

        summarize(data);

        return data.buildScore(maxPoints, "Response Codes");
    }

    private void checkResponses(OpenAPI spec, ResponseData data) {
        goOverOperations(spec, (path, operationId, operation, d) -> {
            d.setTotalOperations(d.getTotalOperations() + 1);

            String location = String.format("#/%s/%s/responses", path, operationId);

            if (operation.getResponses() != null) {
                Set<String> responseCodes = operation.getResponses().keySet();

                // check if the operation has required response codes
                boolean hasRequiredSuccessCodes = responseCodes
                        .stream().anyMatch(code -> code.startsWith("2"));
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
                    d.setOperationsWithProperCodes(d.getOperationsWithProperCodes() + 1);
                } else {
                    // Operation has issues with response codes
                    if(!passedSuccessCodes) {
                        data.getIssues().add(CategoryScore.Issue.builder()
                                .location(location)
                                .description("Operation missing success (2xx) response code")
                                .severity(CategoryScore.Severity.MEDIUM)
                                .suggestion("Define appropriate success (2xx) response code")
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
        }, data);
    }

    private void summarize(ResponseData data) {
        if (data.getTotalOperations() > 0) {
            double responseScore = (double) data.getOperationsWithProperCodes() / data.getTotalOperations();
            data.setPoints((int) (data.getPoints() * responseScore));

            if (data.getOperationsWithProperCodes() == data.getTotalOperations()) {
                data.getStrengths().add("All operations have appropriate response codes");
            }
        } else {
            data.setPoints(0);
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("No operations found to evaluate")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Add API operations with proper response code definitions")
                    .build();
            data.getIssues().add(issue);
        }
    }
}
