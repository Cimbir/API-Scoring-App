package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.category.SecurityData;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

import static com.scoring.core.scoring.service.APIParserHelper.*;

@Service
public class SecurityScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public SecurityScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getSecurity();
        SecurityData data = new SecurityData();
        data.setPoints(maxPoints);
        if(spec.getComponents() != null && spec.getComponents().getSecuritySchemes() != null) {
            data.setSecuritySchemes(spec.getComponents().getSecuritySchemes().keySet());
        }

        if(spec.getPaths() == null || spec.getPaths().isEmpty()) {
            // No paths means no operations, hence no security checks needed
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("paths")
                    .description("No paths defined in the OpenAPI spec")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Add paths to the OpenAPI spec to enable security checks")
                    .build());
            data.setPoints(0);
            return data.buildScore(maxPoints, "Security");
        }

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

        return data.buildScore(maxPoints, "Security");
    }

    private void checkSecuritySchemes(OpenAPI spec, SecurityData data) {
        if (
                spec.getComponents() == null ||
                spec.getComponents().getSecuritySchemes() == null ||
                spec.getComponents().getSecuritySchemes().isEmpty()
        ) {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getSecurity().getPenaltyForWeakSecuritySchemes());
            System.out.println("No security schemes defined in components");

            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/components/securitySchemes")
                    .description("No security schemes defined")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define security schemes in components section (e.g., Bearer token, API key, OAuth2)")
                    .build();

            data.getIssues().add(issue);
            return;
        }

        goOverSecuritySchemes(spec, (schemeName, scheme, d) -> {
            data.setTotalSecuritySchemes(data.getTotalSecuritySchemes() + 1);
            if (scheme == null || scheme.getType() == null ||
                    !scoringConfig.getValidation().getSecurity().getRecommendedSecurityTypes().contains(scheme.getType().toString())) {
                d.setWrongSecuritySchemes(d.getWrongSecuritySchemes() + 1);

                CategoryScore.Issue issue = CategoryScore.Issue.builder()
                        .location(String.format("#/components.securitySchemes/%s", schemeName))
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
        }, data);

        int penalty = (int)(scoringConfig.getValidation().getSecurity().getPenaltyForWeakSecuritySchemes()
                * (double) (data.getWrongSecuritySchemes()) / data.getTotalSecuritySchemes());
        data.setPoints(data.getPoints() - penalty);

        if(data.getWrongSecuritySchemes() == 0) {
            data.getStrengths().add("Security schemes are defined");
        }
    }

    private void checkOperationSecurity(OpenAPI spec, SecurityData data) {
        goOverOperationSecuritySchemes(spec, (path, operationId, schemeName, d) -> {
            d.setTotalOperationsSecurity(d.getTotalOperationsSecurity() + 1);
            if (!d.getSecuritySchemes().contains(schemeName)) {
                d.setWrongOperationsSecurity(d.getWrongOperationsSecurity() + 1);
                CategoryScore.Issue issue = CategoryScore.Issue.builder()
                        .location(String.format("#/%s/%s/security", path, operationId))
                        .description("Security scheme '" + schemeName + "' not defined in components")
                        .severity(CategoryScore.Severity.HIGH)
                        .suggestion("Define security scheme in components section")
                        .build();
                data.getIssues().add(issue);
            } else {
                d.getUsedSchemes().add(schemeName);
            }
        }, data);

        Set<String> leftoverSchemes = new HashSet<>(data.getSecuritySchemes());
        leftoverSchemes.removeAll(data.getUsedSchemes());

        for(String scheme : leftoverSchemes) {
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(String.format("#/components.securitySchemes/%s", scheme))
                    .description("Security scheme '" + scheme + "' defined but not used in any operation")
                    .severity(CategoryScore.Severity.LOW)
                    .suggestion("Consider removing unused security scheme or applying it to operations")
                    .build();
            data.getIssues().add(issue);
        }

        if (data.getTotalOperationsSecurity() > 0) {
            int penalty = (int) (scoringConfig.getValidation().getSecurity().getPenaltyForWeakSecuritySchemes()
                    * (double) data.getWrongOperationsSecurity() / data.getTotalOperationsSecurity());
            data.setPoints(data.getPoints() - penalty);

            if (data.getWrongOperationsSecurity() == 0) {
                data.getStrengths().add("All operations have valid security requirements");
            }
        }
    }

    private void checkGlobalSecurity(OpenAPI spec, SecurityData data) {
        goOverGlobalSecuritySchemes(spec, (schemeName, T) -> {
            data.setTotalGlobalSecurity(data.getTotalGlobalSecurity() + 1);
            if (!data.getSecuritySchemes().contains(schemeName)) {
                data.setWrongGlobalSecurity(data.getWrongGlobalSecurity() + 1);

                CategoryScore.Issue issue = CategoryScore.Issue.builder()
                        .location(String.format("#/security/%s", schemeName))
                        .description("Global security requirement '" + schemeName + "' not defined in components")
                        .severity(CategoryScore.Severity.HIGH)
                        .suggestion("Define security scheme in components section")
                        .build();

                data.getIssues().add(issue);
            }
        }, data);

        if (spec.getSecurity() == null || spec.getSecurity().isEmpty()) {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getSecurity().getPenaltyForWeakGlobalSecurity());
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("security")
                    .description("No global security requirements defined")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define global security requirements in the OpenAPI spec")
                    .build();

            data.getIssues().add(issue);
        }else{
            if(data.getWrongGlobalSecurity() == 0) {
                data.getStrengths().add("Global security requirements are defined");
            }else{
                int penalty = (int) (scoringConfig.getValidation().getSecurity().getPenaltyForWeakGlobalSecurity()
                        * (double) data.getWrongGlobalSecurity() / data.getTotalGlobalSecurity());
                data.setPoints(data.getPoints() - penalty);
            }
        }
    }
}
