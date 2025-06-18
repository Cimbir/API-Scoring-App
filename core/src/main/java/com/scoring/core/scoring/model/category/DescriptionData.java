package com.scoring.core.scoring.model.category;

import com.scoring.core.scoring.model.CategoryScoreData;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescriptionData extends CategoryScoreData {
    private int totalElements = 0;
    private int missingDescriptions = 0;
    private int totalPaths = 0;
    private int pathsWithDescriptions = 0;
    private int totalOperations = 0;
    private int operationsWithDescriptions = 0;
    private int totalParameters = 0;
    private int parametersWithDescriptions = 0;
    private int totalResponses = 0;
    private int responsesWithDescriptions = 0;
    private int totalRequestBodies = 0;
    private int requestBodiesWithDescriptions = 0;
    private int totalSchemas = 0;
    private int schemasWithDescriptions = 0;
    private OpenAPI spec;
}
