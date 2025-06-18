package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.category.BestPracticeData;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import static com.scoring.core.scoring.service.APIParserHelper.goOverOperations;

@Service
public class BestPracticesScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public BestPracticesScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getBestPractices();
        BestPracticeData data = new BestPracticeData();
        data.setPoints(maxPoints);

        // Check versioning (in info or paths)
        if(scoringConfig.getValidation().getBestPractice().isRequireVersioning()) {
            checkVersioning(spec, data);
        }

        // Check servers array
        if(scoringConfig.getValidation().getBestPractice().isRequireServersArray()) {
            checkServersArray(spec, data);
        }

        // Check tags usage
        if(scoringConfig.getValidation().getBestPractice().isRequireTags()) {
            checkTagUsage(spec, data);
        }

        // Check component reuse
        if(scoringConfig.getValidation().getBestPractice().isRequireComponentReuse()) {
            checkComponentReuse(spec, data);
        }

        // Check operation IDs
        if(scoringConfig.getValidation().getBestPractice().isRequireOperationIds()) {
            checkOperationIds(spec, data);
        }

        if(data.getTotal() > 0){
            data.setPoints((int)(maxPoints * (double) data.getPassed() / data.getTotal()));
        }

        return data.buildScore(maxPoints, "Best Practices");
    }

    private void checkVersioning(OpenAPI spec, BestPracticeData data) {
        data.setTotal(data.getTotal() + 1);
        if (spec.getInfo() != null && spec.getInfo().getVersion() != null) {
            data.getStrengths().add("API version is specified");
            data.setPassed(data.getPassed() + 1);
        } else {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/info/version")
                    .description("API version not specified")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Add version information in the info section")
                    .build();

            data.getIssues().add(issue);
        }
    }

    private void checkServersArray(OpenAPI spec, BestPracticeData data) {
        data.setTotal(data.getTotal() + 1);
        if (spec.getServers() == null || spec.getServers().isEmpty()) {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/servers")
                    .description("No servers defined")
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Define server information including base URLs for different environments")
                    .build();

            data.getIssues().add(issue);
        } else {
            data.getStrengths().add("Server information provided");
            data.setPassed(data.getPassed() + 1);
        }
    }

    private void checkTagUsage(OpenAPI spec, BestPracticeData data) {
        data.setTotal(data.getTotal() + 1);

        goOverOperations(spec, (path, operationId, operation, d) -> {
            if (operation.getTags() != null && !operation.getTags().isEmpty()) {
                d.setUsesTags(true);
            } else {
                d.getUntaggedOperations().add(path + " (" + operationId + ")");
            }
        }, data);

        if (!data.isUsesTags()) {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("Operations not properly tagged")
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Add tags to operations for better organization and documentation")
                    .build();

            data.getIssues().add(issue);
        } else {
            data.getStrengths().add("Operations are properly tagged");

            // Add specific issues for untagged operations if some but not all are tagged
            if (!data.getUntaggedOperations().isEmpty()) {
                for (String untaggedOp : data.getUntaggedOperations()) {
                    CategoryScore.Issue issue = CategoryScore.Issue.builder()
                            .location(String.format("#/paths/%s", untaggedOp))
                            .description("Operation missing tags")
                            .severity(CategoryScore.Severity.LOW)
                            .suggestion("Add appropriate tags to this operation for better organization")
                            .build();

                    data.getIssues().add(issue);
                }
            }

            data.setPassed(data.getPassed() + 1);
        }
    }

    private void checkComponentReuse(OpenAPI spec, BestPracticeData data) {
        data.setTotal(data.getTotal() + 1);

        boolean hasReusableComponents = spec.getComponents() != null &&
                spec.getComponents().getSchemas() != null &&
                spec.getComponents().getSchemas().size() > 1;

        if (!hasReusableComponents) {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/components/schemas")
                    .description("Limited use of reusable components")
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Define reusable schema components to avoid duplication and improve maintainability")
                    .build();

            data.getIssues().add(issue);
        } else {
            data.getStrengths().add("Good use of reusable components");

            // Additional check for other component types
            int componentTypeCount = 0;
            if (spec.getComponents().getSchemas() != null && !spec.getComponents().getSchemas().isEmpty()) {
                componentTypeCount++;
            }
            if (spec.getComponents().getParameters() != null && !spec.getComponents().getParameters().isEmpty()) {
                componentTypeCount++;
            }
            if (spec.getComponents().getResponses() != null && !spec.getComponents().getResponses().isEmpty()) {
                componentTypeCount++;
            }
            if (spec.getComponents().getRequestBodies() != null && !spec.getComponents().getRequestBodies().isEmpty()) {
                componentTypeCount++;
            }

            if (componentTypeCount > scoringConfig.getValidation().getBestPractice().getMinimumReusableComponents()) {
                data.getStrengths().add("Excellent use of diverse reusable components");
            }

            data.setPassed(data.getPassed() + 1);
        }
    }

    private void checkOperationIds(OpenAPI spec, BestPracticeData data) {
        data.setTotal(data.getTotal() + 1);

        goOverOperations(spec, (path, operationId, operation, d) -> {
            if (operation.getOperationId() != null && !operation.getOperationId().trim().isEmpty()) {
                data.setHasOperationIds(true);
            } else {
                data.getOperationsWithoutIds().add(path);
            }
        }, data);

        if (!data.isHasOperationIds()) {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("Operations missing operationId")
                    .severity(CategoryScore.Severity.LOW)
                    .suggestion("Add unique operationId to operations for better code generation and tooling support")
                    .build();

            data.getIssues().add(issue);
        } else if (!data.getOperationsWithoutIds().isEmpty()) {
            // Some operations have IDs, some don't
            for (String pathWithoutId : data.getOperationsWithoutIds()) {
                CategoryScore.Issue issue = CategoryScore.Issue.builder()
                        .location(String.format("#/paths/%s", pathWithoutId))
                        .description("Operation missing operationId")
                        .severity(CategoryScore.Severity.LOW)
                        .suggestion("Add unique operationId for better tooling support")
                        .build();

                data.getIssues().add(issue);
            }
        } else {
            data.getStrengths().add("All operations have operationId defined");
            data.setPassed(data.getPassed() + 1);
        }
    }

}
