package com.scoring.core.scoring.model.category.description;

public record DocumentationStats(
        int totalElements,
        int missingDescriptions,
        int totalPaths,
        int pathsWithDescriptions,
        int totalOperations,
        int operationsWithDescriptions,
        int totalParameters,
        int parametersWithDescriptions,
        int totalResponses,
        int responsesWithDescriptions,
        int totalRequestBodies,
        int requestBodiesWithDescriptions
) {}
