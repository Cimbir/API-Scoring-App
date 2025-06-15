package com.scoring.core.scoring.service;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.SpecScore;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class APIScoringService {
    private final ScoringConfig scoringConfig;

    public APIScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    public SpecScore score(OpenAPI spec) {
        return SpecScore.builder()
                .withTotalScore(100)
                .withCategoryScores(new HashMap<>())
                .withRecommendations(new ArrayList<>())
                .build();
    }

    private CategoryScore scoreDescriptions(OpenAPI openAPI) {
        CategoryScore score = new CategoryScore("Descriptions & Documentation", 20);
        int points = 20;
        int totalElements = 0;
        int missingDescriptions = 0;

        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                PathItem pathItem = pathEntry.getValue();

                for (Operation operation : getOperations(pathItem)) {
                    totalElements++;
                    if (operation.getDescription() == null || operation.getDescription().trim().isEmpty()) {
                        missingDescriptions++;
                    }

                    // Check parameters
                    if (operation.getParameters() != null) {
                        for (Parameter param : operation.getParameters()) {
                            totalElements++;
                            if (param.getDescription() == null || param.getDescription().trim().isEmpty()) {
                                missingDescriptions++;
                            }
                        }
                    }

                    // Check responses
                    if (operation.getResponses() != null) {
                        for (ApiResponse response : operation.getResponses().values()) {
                            totalElements++;
                            if (response.getDescription() == null || response.getDescription().trim().isEmpty()) {
                                missingDescriptions++;
                            }
                        }
                    }
                }
            }
        }

        if (totalElements > 0) {
            double descriptionScore = 1.0 - ((double) missingDescriptions / totalElements);
            points = (int) (points * descriptionScore);

            if (missingDescriptions > 0) {
                score.getIssues().add(String.format("%d out of %d elements missing descriptions", missingDescriptions, totalElements));
            } else {
                score.getStrengths().add("All elements have proper descriptions");
            }
        } else {
            points = 0;
            score.getIssues().add("No API operations found to evaluate");
        }

        score.setScore(Math.max(0, points));
        return score;
    }

    private CategoryScore scorePathsAndOperations(OpenAPI openAPI) {
        CategoryScore score = new CategoryScore("Paths & Operations", 15);
        int points = 15;

        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            score.setScore(0);
            score.getIssues().add("No paths defined");
            return score;
        }

        // Check for consistent naming
        List<String> pathNames = new ArrayList<>(openAPI.getPaths().keySet());
        boolean hasConsistentNaming = checkConsistentNaming(pathNames);
        if (!hasConsistentNaming) {
            points -= 5;
            score.getIssues().add("Inconsistent path naming conventions");
        } else {
            score.getStrengths().add("Consistent path naming conventions");
        }

        // Check for CRUD operations
        boolean hasCrudOperations = checkCrudOperations(openAPI);
        if (!hasCrudOperations) {
            points -= 5;
            score.getIssues().add("Missing standard CRUD operations");
        } else {
            score.getStrengths().add("Proper CRUD operations implemented");
        }

        // Check for overlapping paths
        boolean hasOverlappingPaths = checkOverlappingPaths(pathNames);
        if (hasOverlappingPaths) {
            points -= 5;
            score.getIssues().add("Overlapping or redundant paths detected");
        } else {
            score.getStrengths().add("No overlapping paths detected");
        }

        score.setScore(Math.max(0, points));
        return score;
    }

    private CategoryScore scoreResponseCodes(OpenAPI openAPI) {
        CategoryScore score = new CategoryScore("Response Codes", 15);
        int points = 15;
        int totalOperations = 0;
        int operationsWithProperCodes = 0;

        if (openAPI.getPaths() != null) {
            for (PathItem pathItem : openAPI.getPaths().values()) {
                for (Operation operation : getOperations(pathItem)) {
                    totalOperations++;

                    if (operation.getResponses() != null) {
                        Set<String> responseCodes = operation.getResponses().keySet();
                        boolean hasSuccessCode = responseCodes.stream().anyMatch(code -> code.startsWith("2"));
                        boolean hasErrorCode = responseCodes.stream().anyMatch(code -> code.startsWith("4") || code.startsWith("5"));

                        if (hasSuccessCode && hasErrorCode) {
                            operationsWithProperCodes++;
                        }
                    }
                }
            }
        }

        if (totalOperations > 0) {
            double responseScore = (double) operationsWithProperCodes / totalOperations;
            points = (int) (points * responseScore);

            if (operationsWithProperCodes < totalOperations) {
                score.getIssues().add(String.format("%d out of %d operations missing proper response codes",
                        totalOperations - operationsWithProperCodes, totalOperations));
            } else {
                score.getStrengths().add("All operations have appropriate response codes");
            }
        } else {
            points = 0;
            score.getIssues().add("No operations found to evaluate");
        }

        score.setScore(Math.max(0, points));
        return score;
    }

    private CategoryScore scoreExamples(OpenAPI openAPI) {
        CategoryScore score = new CategoryScore("Examples & Samples", 10);
        int points = 10;
        int totalMediaTypes = 0;
        int mediaTypesWithExamples = 0;

        if (openAPI.getPaths() != null) {
            for (PathItem pathItem : openAPI.getPaths().values()) {
                for (Operation operation : getOperations(pathItem)) {
                    // Check request body examples
                    if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                        for (MediaType mediaType : operation.getRequestBody().getContent().values()) {
                            totalMediaTypes++;
                            if (hasExamples(mediaType)) {
                                mediaTypesWithExamples++;
                            }
                        }
                    }

                    // Check response examples
                    if (operation.getResponses() != null) {
                        for (ApiResponse response : operation.getResponses().values()) {
                            if (response.getContent() != null) {
                                for (MediaType mediaType : response.getContent().values()) {
                                    totalMediaTypes++;
                                    if (hasExamples(mediaType)) {
                                        mediaTypesWithExamples++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (totalMediaTypes > 0) {
            double exampleScore = (double) mediaTypesWithExamples / totalMediaTypes;
            points = (int) (points * exampleScore);

            if (mediaTypesWithExamples < totalMediaTypes) {
                score.getIssues().add(String.format("%d out of %d request/response bodies missing examples",
                        totalMediaTypes - mediaTypesWithExamples, totalMediaTypes));
            } else {
                score.getStrengths().add("All request/response bodies have examples");
            }
        } else {
            score.getIssues().add("No request/response bodies found to evaluate");
        }

        score.setScore(Math.max(0, points));
        return score;
    }

    private CategoryScore scoreSecurity(OpenAPI openAPI) {
        CategoryScore score = new CategoryScore("Security", 10);
        int points = 10;

        // Check if security schemes are defined
        boolean hasSecuritySchemes = openAPI.getComponents() != null &&
                openAPI.getComponents().getSecuritySchemes() != null &&
                !openAPI.getComponents().getSecuritySchemes().isEmpty();

        if (!hasSecuritySchemes) {
            points -= 5;
            score.getIssues().add("No security schemes defined");
        } else {
            score.getStrengths().add("Security schemes are defined");
        }

        // Check if security is applied to operations
        boolean hasSecurityRequirements = false;
        if (openAPI.getPaths() != null) {
            for (PathItem pathItem : openAPI.getPaths().values()) {
                for (Operation operation : getOperations(pathItem)) {
                    if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
                        hasSecurityRequirements = true;
                        break;
                    }
                }
                if (hasSecurityRequirements) break;
            }
        }

        // Also check global security
        if (!hasSecurityRequirements && openAPI.getSecurity() != null && !openAPI.getSecurity().isEmpty()) {
            hasSecurityRequirements = true;
        }

        if (!hasSecurityRequirements) {
            points -= 5;
            score.getIssues().add("Security schemes not applied to operations");
        } else {
            score.getStrengths().add("Security requirements are applied");
        }

        score.setScore(Math.max(0, points));
        return score;
    }

    private CategoryScore scoreBestPractices(OpenAPI openAPI) {
        CategoryScore score = new CategoryScore("Best Practices", 10);
        int points = 10;

        // Check versioning (in info or paths)
        boolean hasVersioning = false;
        if (openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null) {
            hasVersioning = true;
            score.getStrengths().add("API version is specified");
        } else {
            points -= 3;
            score.getIssues().add("API version not specified");
        }

        // Check servers array
        if (openAPI.getServers() == null || openAPI.getServers().isEmpty()) {
            points -= 2;
            score.getIssues().add("No servers defined");
        } else {
            score.getStrengths().add("Server information provided");
        }

        // Check tags usage
        boolean usesTags = false;
        if (openAPI.getPaths() != null) {
            for (PathItem pathItem : openAPI.getPaths().values()) {
                for (Operation operation : getOperations(pathItem)) {
                    if (operation.getTags() != null && !operation.getTags().isEmpty()) {
                        usesTags = true;
                        break;
                    }
                }
                if (usesTags) break;
            }
        }

        if (!usesTags) {
            points -= 2;
            score.getIssues().add("Operations not properly tagged");
        } else {
            score.getStrengths().add("Operations are properly tagged");
        }

        // Check component reuse
        boolean hasReusableComponents = openAPI.getComponents() != null &&
                openAPI.getComponents().getSchemas() != null &&
                openAPI.getComponents().getSchemas().size() > 1;

        if (!hasReusableComponents) {
            points -= 3;
            score.getIssues().add("Limited use of reusable components");
        } else {
            score.getStrengths().add("Good use of reusable components");
        }

        score.setScore(Math.max(0, points));
        return score;
    }

    // Helper methods

    private boolean checkConsistentNaming(List<String> paths) {
        // Simple check: all paths should follow similar patterns
        // This is a basic implementation - you could make it more sophisticated
        long kebabCaseCount = paths.stream().filter(p -> p.contains("-")).count();
        long camelCaseCount = paths.stream().filter(p -> p.matches(".*[a-z][A-Z].*")).count();

        // If most paths follow one convention, consider it consistent
        return (kebabCaseCount > paths.size() * 0.8) || (camelCaseCount > paths.size() * 0.8) ||
                (kebabCaseCount == 0 && camelCaseCount == 0);
    }

    private boolean checkCrudOperations(OpenAPI openAPI) {
        // Check if there are basic CRUD operations (GET, POST, PUT, DELETE)
        Set<String> httpMethods = new HashSet<>();

        if (openAPI.getPaths() != null) {
            for (PathItem pathItem : openAPI.getPaths().values()) {
                if (pathItem.getGet() != null) httpMethods.add("GET");
                if (pathItem.getPost() != null) httpMethods.add("POST");
                if (pathItem.getPut() != null) httpMethods.add("PUT");
                if (pathItem.getDelete() != null) httpMethods.add("DELETE");
            }
        }

        return httpMethods.size() >= 3; // At least 3 out of 4 CRUD operations
    }

    private boolean checkOverlappingPaths(List<String> paths) {
        // Simple check for very similar paths that might be redundant
        for (int i = 0; i < paths.size(); i++) {
            for (int j = i + 1; j < paths.size(); j++) {
                String path1 = paths.get(i);
                String path2 = paths.get(j);

                // Check if paths are very similar (basic check)
                if (arePathsSimilar(path1, path2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean arePathsSimilar(String path1, String path2) {
        // Basic similarity check - this could be more sophisticated
        String[] parts1 = path1.split("/");
        String[] parts2 = path2.split("/");

        if (parts1.length != parts2.length) return false;

        int similarities = 0;
        for (int i = 0; i < parts1.length; i++) {
            if (parts1[i].equals(parts2[i]) ||
                    (parts1[i].startsWith("{") && parts2[i].startsWith("{"))) {
                similarities++;
            }
        }

        return similarities >= parts1.length - 1; // Allow one difference
    }

    private boolean hasExamples(MediaType mediaType) {
        return (mediaType.getExample() != null) ||
                (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty());
    }

    private void generateRecommendations(ScoreResult result) {
        for (CategoryScore categoryScore : result.getCategoryScores().values()) {
            if (categoryScore.getScore() < categoryScore.getMaxScore() * 0.8) {
                result.getRecommendations().add(
                        String.format("Improve %s: %s",
                                categoryScore.getCategory(),
                                String.join(", ", categoryScore.getIssues()))
                );
            }
        }

        if (result.getTotalScore() < 70) {
            result.getRecommendations().add("Overall score is below 70% - consider comprehensive review of OpenAPI specification");
        }
    }
}
