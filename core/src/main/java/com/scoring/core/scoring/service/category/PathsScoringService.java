package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.category.PathsData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class PathsScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    // Common REST naming conventions
    private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z]+(-[a-z]+)*$");
    private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z]+(_[a-z]+)*$");
    private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z]+([A-Z][a-z]*)*$");

    public PathsScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getPathsAndOperations();
        PathsData data = new PathsData();
        data.setPoints(maxPoints);

        if (spec.getPaths() == null || spec.getPaths().isEmpty()) {
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("No paths defined in the API specification")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define API paths and operations to create a functional API")
                    .build());

            data.setPoints(0);
            return data.buildScore(maxPoints, "Paths & Operations");
        }

        data.setPathNames(new ArrayList<>(spec.getPaths().keySet()));

        // Check for consistent naming (5 points)
        if(scoringConfig.getValidation().getPath().isEnforceNamingConventions()) analyzeNamingConsistency(data);

        // Check for CRUD operations (5 points)
        if(scoringConfig.getValidation().getPath().isEnforceCrudOperationConventions()) analyzeCrudOperations(spec, data);

        // Check for overlapping paths (5 points)
        if(scoringConfig.getValidation().getPath().isCheckForRedundantPaths()) analyzeOverlappingPaths(data);

        return data.buildScore(maxPoints, "Paths & Operations");
    }

    private void analyzeNamingConsistency(PathsData data) {
        if (data.getPathNames().isEmpty()) return;

        Map<String, Integer> patternCounts = new HashMap<>();
        List<String> inconsistentPaths = new ArrayList<>();

        // Determine naming patterns for each path segment
        for (String path : data.getPathNames()) {
            String[] segments = path.split("/");
            for (String segment : segments) {
                if (segment.isEmpty() || segment.startsWith("{")) continue; // Skip empty and parameter segments

                String pattern = detectNamingPattern(segment);
                patternCounts.merge(pattern, 1, Integer::sum);
            }
        }

        // Determine the dominant pattern
        String dominantPattern = patternCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        // Check for inconsistencies
        for (String path : data.getPathNames()) {
            String[] segments = path.split("/");
            for (String segment : segments) {
                if (segment.isEmpty() || segment.startsWith("{")) continue;

                String pattern = detectNamingPattern(segment);
                if (!pattern.equals(dominantPattern) && !pattern.equals("unknown")) {
                    inconsistentPaths.add(path);
                    break;
                }
            }
        }

        // Add issues for inconsistent naming
        if (!inconsistentPaths.isEmpty()) {
            for (String path : inconsistentPaths) {
                data.getIssues().add(CategoryScore.Issue.builder()
                        .location(String.format("#/paths/%s", path))
                        .description("Path uses inconsistent naming convention")
                        .severity(CategoryScore.Severity.LOW)
                        .suggestion("Use consistent naming convention across all paths (detected dominant pattern: " + dominantPattern + ")")
                        .build());
            }

            // Add summary issue
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description(String.format("Inconsistent path naming: %d paths don't follow the dominant %s pattern",
                            inconsistentPaths.size(), dominantPattern))
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Standardize all path segments to use " + dominantPattern + " naming convention")
                    .build());
        }

        if (inconsistentPaths.isEmpty()) {
            data.getStrengths().add("Consistent path naming convention detected: " + dominantPattern);
        }else {
            data.setPoints(data.getPoints() + scoringConfig.getValidation().getPath().getPenaltyForMissingCrudOperations());
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description(String.format("Found %d paths with inconsistent naming conventions", inconsistentPaths.size()))
                    .severity(CategoryScore.Severity.LOW)
                    .suggestion("Consider standardizing path naming conventions to improve API clarity")
                    .build());
        }
    }

    private void analyzeCrudOperations(OpenAPI spec, PathsData data) {
        Map<String, Set<PathItem.HttpMethod>> resourceMethods = new HashMap<>();

        // Group methods by resource path
        spec.getPaths().forEach((path, pathItem) -> {
            Set<PathItem.HttpMethod> methods = pathItem.readOperationsMap().keySet();
            resourceMethods.merge(path, methods, (existing, newMethods) -> {
                Set<PathItem.HttpMethod> combined = new HashSet<>(existing);
                combined.addAll(newMethods);
                return combined;
            });
        });

        int previousSize = data.getIssues().size();

        // Check for invalid CRUD operations convention
        resourceMethods.forEach((path, methods) -> {
            // check if path is a resource path for POST
            if (
                    methods.contains(PathItem.HttpMethod.POST) &&
                            path.endsWith("}")
            ) {
                data.getIssues().add(CategoryScore.Issue.builder()
                        .location(String.format("#/paths/%s", path))
                        .description("POST operation found on a path with parameters, which may not be suitable for resource creation")
                        .severity(CategoryScore.Severity.LOW)
                        .suggestion("Consider using POST on a base resource path without parameters")
                        .build());
            }

            // check if path is a resource path for PUT, PATCH, DELETE
            if (
                    (methods.contains(PathItem.HttpMethod.PUT) ||
                    methods.contains(PathItem.HttpMethod.PATCH) ||
                    methods.contains(PathItem.HttpMethod.DELETE)) &&
                    !path.endsWith("}")
            ) {
                data.getIssues().add(CategoryScore.Issue.builder()
                        .location(String.format("#/paths/%s", path))
                        .description("PUT, PATCH, or DELETE operation found on a path without parameters, which may not be suitable for resource management")
                        .severity(CategoryScore.Severity.LOW)
                        .suggestion("Consider using these methods on a base resource path with parameters")
                        .build());
            }
        });

        if(previousSize == data.getIssues().size()) {
            data.getStrengths().add("All paths follow proper CRUD operation conventions");
        } else {
            data.setPoints(data.getPoints() + scoringConfig.getValidation().getPath().getPenaltyForMissingCrudOperations());
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("Some paths do not follow proper CRUD operation conventions")
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Review paths to ensure they follow standard RESTful CRUD conventions")
                    .build());
        }
    }

    private void analyzeOverlappingPaths(PathsData data) {
        List<String> overlappingPaths = new ArrayList<>();

        for (int i = 0; i < data.getPathNames().size(); i++) {
            for (int j = i + 1; j < data.getPathNames().size(); j++) {
                String path1 = data.getPathNames().get(i);
                String path2 = data.getPathNames().get(j);

                if (pathsOverlap(path1, path2)) {
                    overlappingPaths.add(path1 + " <-> " + path2);

                    data.getIssues().add(CategoryScore.Issue.builder()
                            .location(String.format("#/paths/%s - #/paths/%s", path1, path2))
                            .description("Path potentially overlaps with " + path2)
                            .severity(CategoryScore.Severity.MEDIUM)
                            .suggestion("Review path structure to ensure no ambiguous routing")
                            .build());
                }
            }
        }

        if (!overlappingPaths.isEmpty()) {
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description(String.format("Found %d potential path overlaps", overlappingPaths.size()))
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Redesign overlapping paths to have clear, unambiguous routing")
                    .build());
        }

        if (overlappingPaths.isEmpty()) {
            data.getStrengths().add("No overlapping or ambiguous paths detected");
        } else {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getPath().getPenaltyForRedundantPaths());
            overlappingPaths.forEach(path ->
                data.getIssues().add(CategoryScore.Issue.builder()
                        .location(String.format("#/paths/%s", path))
                        .description("Path overlaps with another path, which may cause routing issues")
                        .severity(CategoryScore.Severity.MEDIUM)
                        .suggestion("Consider redesigning paths to avoid overlaps")
                        .build())
            );
        }
    }

    private String detectNamingPattern(String segment) {
        if (scoringConfig.getValidation().getPath().getAllowedNamingConventions().contains("kebab-case") &&
                KEBAB_CASE.matcher(segment).matches()) return "kebab-case";
        if (scoringConfig.getValidation().getPath().getAllowedNamingConventions().contains("snake_case") &&
                SNAKE_CASE.matcher(segment).matches()) return "snake_case";
        if (scoringConfig.getValidation().getPath().getAllowedNamingConventions().contains("camelCase") &&
                CAMEL_CASE.matcher(segment).matches()) return "camelCase";
        return "unknown";
    }

    private boolean pathsOverlap(String path1, String path2) {
        String[] segments1 = path1.split("/");
        String[] segments2 = path2.split("/");

        if (segments1.length != segments2.length) return false;

        boolean hasParameterDifference = false;
        for (int i = 0; i < segments1.length; i++) {
            String seg1 = segments1[i];
            String seg2 = segments2[i];

            boolean isParam1 = seg1.startsWith("{") && seg1.endsWith("}");
            boolean isParam2 = seg2.startsWith("{") && seg2.endsWith("}");

            if (isParam1 && isParam2) {
                continue; // Both parameters, could overlap
            } else if (isParam1 || isParam2) {
                hasParameterDifference = true;
            } else if (!seg1.equals(seg2)) {
                return false; // Different literal segments
            }
        }

        // Paths overlap if they have the same structure but different parameter names
        return hasParameterDifference;
    }
}
