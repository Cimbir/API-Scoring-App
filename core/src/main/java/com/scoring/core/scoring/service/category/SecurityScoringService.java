package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.CategoryScoreData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SecurityScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public SecurityScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getSecurity();
        CategoryScoreData data = new CategoryScoreData();


        // Check if security schemes are defined
        if(scoringConfig.getValidation().getSecurity().isRequireSecuritySchemes()) {
            checkSecuritySchemes(spec, data);
        }

        // Check if security schemes are applied to operations
        if(scoringConfig.getValidation().getSecurity().isRequireOperationLevelSecurity()) {
            checkOperationSecurity(spec, data);
        }

        // Also check global security
        if(scoringConfig.getValidation().getSecurity().isRequireGlobalSecurity()) {
            checkGlobalSecurity(spec, data);
        }
        return CategoryScore.builder()
                .categoryName("Security")
                .score(Math.max(0, data.getPoints()))
                .maxScore(maxPoints)
                .issues(data.getIssues())
                .strengths(data.getStrengths())
                .build();
    }

    private void checkSecuritySchemes(OpenAPI spec, CategoryScoreData data) {
        boolean hasSecuritySchemes = spec.getComponents() != null &&
                spec.getComponents().getSecuritySchemes() != null &&
                !spec.getComponents().getSecuritySchemes().isEmpty();

        boolean hasIssues = false;

        if (!hasSecuritySchemes) {
            hasIssues = true;
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getSecurity().getPenaltyForWeakSecuritySchemes());

            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("", "", "components.securitySchemes"))
                    .description("No security schemes defined")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define security schemes in components section (e.g., Bearer token, API key, OAuth2)")
                    .build();

            data.getIssues().add(issue);
        }

        int totalSecuritySchemes = spec.getComponents().getSecuritySchemes().size();
        int wrongTypeSchemes = 0;

        for( String schemeName : spec.getComponents().getSecuritySchemes().keySet()) {
            SecurityScheme scheme = spec.getComponents().getSecuritySchemes().get(schemeName);
            if (scheme == null || scheme.getType() == null ||
                    scoringConfig.getValidation().getSecurity().getRecommendedSecurityTypes().contains(scheme.getType().toString())) {
                wrongTypeSchemes++;
                hasIssues = true;

                CategoryScore.Issue issue = CategoryScore.Issue.builder()
                        .location(new CategoryScore.Issue.Location("components.securitySchemes", schemeName, ""))
                        .description(
                                String.format(
                                        "Security scheme '%s' is defined but not configured with the recommended type: %s",
                                        schemeName,
                                        String.join(", ", scoringConfig.getValidation().getSecurity().getRecommendedSecurityTypes())
                                ))
                        .severity(CategoryScore.Severity.MEDIUM)
                        .suggestion("Ensure security scheme is properly configured")
                        .build();

                data.getIssues().add(issue);
            }
        }

        int penalty = (int)(scoringConfig.getValidation().getSecurity().getPenaltyForWeakSecuritySchemes()
                * (double) (wrongTypeSchemes) / totalSecuritySchemes);

        data.setPoints(data.getPoints() - penalty);

        if(!hasIssues) {
            data.getStrengths().add("Security schemes are defined");
        }
    }

    private void checkOperationSecurity(OpenAPI spec, CategoryScoreData data) {
        int totalOperationsSecurity = 0;
        int wrongOperationSecurity = 0;

        Set<String> securitySchemes = spec.getComponents().getSecuritySchemes().keySet();
        Set<String> usedSchemes = new HashSet<>();

        if (spec.getPaths() != null) {
            for (String path : spec.getPaths().keySet()) {
                PathItem pathItem = spec.getPaths().get(path);
                for (Operation operation : pathItem.readOperations()) {
                    String operationId = operation.getOperationId() != null ?
                            operation.getOperationId() : "unknown";

                    if(operation.getSecurity() != null) {
                        for (SecurityRequirement securityRequirement : operation.getSecurity()) {
                            for (String schemeName : securityRequirement.keySet()) {
                                totalOperationsSecurity++;
                                if (!securitySchemes.contains(schemeName)) {
                                    wrongOperationSecurity++;
                                    CategoryScore.Issue issue = CategoryScore.Issue.builder()
                                            .location(new CategoryScore.Issue.Location(path, operationId, "security"))
                                            .description("Security scheme '" + schemeName + "' not defined in components")
                                            .severity(CategoryScore.Severity.HIGH)
                                            .suggestion("Define security scheme in components section")
                                            .build();
                                    data.getIssues().add(issue);
                                } else {
                                    usedSchemes.add(schemeName);
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<String> leftoverSchemes = new HashSet<>(securitySchemes);
        leftoverSchemes.removeAll(usedSchemes);
        for(String scheme : leftoverSchemes) {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("components.securitySchemes", scheme, ""))
                    .description("Security scheme '" + scheme + "' defined but not used in any operation")
                    .severity(CategoryScore.Severity.LOW)
                    .suggestion("Consider removing unused security scheme or applying it to operations")
                    .build();
            data.getIssues().add(issue);
        }

        if (totalOperationsSecurity > 0) {
            int penalty = (int) (scoringConfig.getValidation().getSecurity().getPenaltyForWeakSecuritySchemes()
                    * (double) wrongOperationSecurity / totalOperationsSecurity);

            data.setPoints(data.getPoints() - penalty);

            if (wrongOperationSecurity == 0) {
                data.getStrengths().add("All operations have valid security requirements");
            }
        }
    }

    private void checkGlobalSecurity(OpenAPI spec, CategoryScoreData data) {
        int totalGlobalSecurity = 0;
        int wrongGlobalSecurity = 0;

        Set<String> securitySchemes = spec.getComponents().getSecuritySchemes().keySet();

        boolean hasSecurityRequirements = spec.getSecurity() != null && !spec.getSecurity().isEmpty();

        if (hasSecurityRequirements) {
            for (SecurityRequirement securityRequirement : spec.getSecurity()) {
                for (String schemeName : securityRequirement.keySet()) {
                    totalGlobalSecurity++;
                    if (!securitySchemes.contains(schemeName)) {
                        wrongGlobalSecurity++;

                        CategoryScore.Issue issue = CategoryScore.Issue.builder()
                                .location(new CategoryScore.Issue.Location("security", schemeName, ""))
                                .description("Global security requirement '" + schemeName + "' not defined in components")
                                .severity(CategoryScore.Severity.HIGH)
                                .suggestion("Define security scheme in components section")
                                .build();

                        data.getIssues().add(issue);
                    }
                }
            }
        }

        if (!hasSecurityRequirements) {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getSecurity().getPenaltyForWeakGlobalSecurity());

            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("", "", "security"))
                    .description("No global security requirements defined")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define global security requirements in the OpenAPI spec")
                    .build();

            data.getIssues().add(issue);
        }else{
            if(wrongGlobalSecurity == 0) {
                data.getStrengths().add("Global security requirements are defined");
            }else{
                int penalty = (int) (scoringConfig.getValidation().getSecurity().getPenaltyForWeakGlobalSecurity()
                        * (double) wrongGlobalSecurity / totalGlobalSecurity);

                data.setPoints(data.getPoints() - wrongGlobalSecurity);
            }
        }
    }
}
