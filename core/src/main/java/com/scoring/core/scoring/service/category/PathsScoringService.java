package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.CategoryScoreData;
import com.scoring.core.scoring.model.category.path.CrudAnalysis;
import com.scoring.core.scoring.model.category.path.NamingAnalysis;
import com.scoring.core.scoring.model.category.path.OverlapAnalysis;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

// TODO: path similarity threshold configuration

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
        CategoryScoreData data = new CategoryScoreData();

        if (spec.getPaths() == null || spec.getPaths().isEmpty()) {
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("paths", "none", "root"))
                    .description("No paths defined in the API specification")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define API paths and operations to create a functional API")
                    .build());

            return CategoryScore.builder()
                    .maxScore(maxPoints)
                    .score(0)
                    .categoryName("Paths & Operations")
                    .issues(data.getIssues())
                    .strengths(data.getStrengths())
                    .build();
        }

        List<String> pathNames = new ArrayList<>(spec.getPaths().keySet());

        // Check for consistent naming (5 points)
        NamingAnalysis namingAnalysis = analyzeNamingConsistency(pathNames, data);
        if (!namingAnalysis.isConsistent()) {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getPath().getPenaltyForNamingConventionMismatch());
        } else {
            data.getStrengths().add("Consistent path naming conventions (" + namingAnalysis.detectedPattern() + ")");
        }

        // Check for CRUD operations (5 points)
        CrudAnalysis crudAnalysis = analyzeCrudOperations(spec, data);
        if (!crudAnalysis.hasProperCrud()) {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getPath().getPenaltyForMissingCrudOperations());
        } else {
            data.getStrengths().add("Proper CRUD operations implemented");
        }

        // Check for overlapping paths (5 points)
        OverlapAnalysis overlapAnalysis = analyzeOverlappingPaths(pathNames, data);
        if (scoringConfig.getValidation().getPath().isCheckForRedundantPaths()) {
            if (overlapAnalysis.hasOverlaps()) {
                data.setPoints(data.getPoints() - scoringConfig.getValidation().getPath().getPenaltyForRedundantPaths());
            } else {
                data.getStrengths().add("No overlapping or redundant paths detected");
            }
        }

        return CategoryScore.builder()
                .maxScore(maxPoints)
                .score(Math.max(0, data.getPoints()))
                .categoryName("Paths & Operations")
                .issues(data.getIssues())
                .strengths(data.getStrengths())
                .build();
    }

    private NamingAnalysis analyzeNamingConsistency(List<String> pathNames, CategoryScoreData data) {
        if (pathNames.isEmpty()) {
            return new NamingAnalysis(true, "none");
        }

        Map<String, Integer> patternCounts = new HashMap<>();
        List<String> inconsistentPaths = new ArrayList<>();

        // Determine naming patterns for each path segment
        for (String path : pathNames) {
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
        for (String path : pathNames) {
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
                        .location(new CategoryScore.Issue.Location(path, "all", "naming"))
                        .description("Path uses inconsistent naming convention")
                        .severity(CategoryScore.Severity.LOW)
                        .suggestion("Use consistent naming convention across all paths (detected dominant pattern: " + dominantPattern + ")")
                        .build());
            }

            // Add summary issue
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("overall", "all", "naming"))
                    .description(String.format("Inconsistent path naming: %d paths don't follow the dominant %s pattern",
                            inconsistentPaths.size(), dominantPattern))
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Standardize all path segments to use " + dominantPattern + " naming convention")
                    .build());
        }

        return new NamingAnalysis(inconsistentPaths.isEmpty(), dominantPattern);
    }

    private CrudAnalysis analyzeCrudOperations(OpenAPI spec, CategoryScoreData data) {
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
                        .location(new CategoryScore.Issue.Location(path, "post", "crud"))
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
                        .location(new CategoryScore.Issue.Location(path, "put-patch-delete", "crud"))
                        .description("PUT, PATCH, or DELETE operation found on a path without parameters, which may not be suitable for resource management")
                        .severity(CategoryScore.Severity.LOW)
                        .suggestion("Consider using these methods on a base resource path with parameters")
                        .build());
            }
        });

        return new CrudAnalysis(previousSize == data.getIssues().size());
    }

    private OverlapAnalysis analyzeOverlappingPaths(List<String> pathNames, CategoryScoreData data) {
        List<String> overlappingPaths = new ArrayList<>();

        for (int i = 0; i < pathNames.size(); i++) {
            for (int j = i + 1; j < pathNames.size(); j++) {
                String path1 = pathNames.get(i);
                String path2 = pathNames.get(j);

                if (pathsOverlap(path1, path2)) {
                    overlappingPaths.add(path1 + " <-> " + path2);

                    data.getIssues().add(CategoryScore.Issue.builder()
                            .location(new CategoryScore.Issue.Location(path1, "all", "overlap"))
                            .description("Path potentially overlaps with " + path2)
                            .severity(CategoryScore.Severity.MEDIUM)
                            .suggestion("Review path structure to ensure no ambiguous routing")
                            .build());
                }
            }
        }

        if (!overlappingPaths.isEmpty()) {
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location(new CategoryScore.Issue.Location("overall", "all", "overlaps"))
                    .description(String.format("Found %d potential path overlaps", overlappingPaths.size()))
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Redesign overlapping paths to have clear, unambiguous routing")
                    .build());
        }

        return new OverlapAnalysis(!overlappingPaths.isEmpty());
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
