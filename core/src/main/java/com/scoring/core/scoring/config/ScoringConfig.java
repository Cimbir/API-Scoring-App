package com.scoring.core.scoring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scoring")
public class ScoringConfig {

    /**
     * Scoring weights for each category (should total 100)
     */
    private CategoryWeights weights;

    /**
     * Scoring thresholds for different quality levels
     */
    private QualityThresholds thresholds;

    /**
     * Validation rules configuration
     */
    private ValidationRules validation;

    @Setter
    @Getter
    public static class CategoryWeights {
        private int schemaAndTypes;
        private int descriptionsAndDocumentation;
        private int pathsAndOperations;
        private int responseCodes;
        private int examplesAndSamples;
        private int security;
        private int bestPractices;
        private double categoryMinimumPercentage;

        public int getTotalWeight() {
            return schemaAndTypes + descriptionsAndDocumentation + pathsAndOperations +
                    responseCodes + examplesAndSamples + security + bestPractices;
        }
    }

    /**
     * Quality level thresholds
     */
    @Setter
    @Getter
    public static class QualityThresholds {
        private int excellent;
        private int veryGood;
        private int good;
        private int fair;
        private int poor;
        private int veryPoor;
    }

    /**
     * Validation rules configuration
     */
    @Setter
    @Getter
    public static class ValidationRules {
        private SchemaValidation schema;
        private DescriptionValidation description;
        private PathValidation path;
        private ResponseValidation response;
        private ExampleValidation example;
        private SecurityValidation security;
        private BestPracticeValidation bestPractice;

    }

    @Setter
    @Getter
    public static class SchemaValidation {
        private boolean requireSchemaComponents;
        private boolean requireRequestBodySchema;
        private boolean requireResponseBodySchema;
        private boolean allowedGenericSchema;
        private List<String> requiredDataTypes;
        private int penaltyForMissingSchema;

    }

    @Setter
    @Getter
    public static class DescriptionValidation {
        private int minimumDescriptionLength;
        private boolean requireGeneralDescription;
        private boolean requireOperationDescriptions;
        private boolean requireParameterDescriptions;
        private boolean requireResponseDescriptions;
        private boolean requireSchemaDescriptions;
        private boolean requireRequestDescriptions;

    }

    @Setter
    @Getter
    public static class PathValidation {
        private boolean enforceNamingConventions;
        private boolean enforceCrudOperationConventions;
        private boolean checkForRedundantPaths;
        private List<String> allowedNamingConventions;
        private double pathSimilarityThreshold;
        private int penaltyForNamingConventionMismatch;
        private int penaltyForMissingCrudOperations;
        private int penaltyForRedundantPaths;

    }

    @Setter
    @Getter
    public static class ResponseValidation {
        private boolean requireSuccessResponses;
        private boolean requireErrorResponses;
        private boolean requireDefaultResponse;
        private List<String> requiredErrorCodes;

    }

    @Setter
    @Getter
    public static class ExampleValidation {
        private boolean requireRequestExamples;
        private boolean requireResponseExamples;
        private double minimumExampleCoverage;

    }

    @Setter
    @Getter
    public static class SecurityValidation {
        private boolean requireSecuritySchemes;
        private boolean requireGlobalSecurity;
        private boolean requireOperationLevelSecurity;
        private List<String> recommendedSecurityTypes;
        private int penaltyForWeakSecuritySchemes;
        private int penaltyForWeakOperationSecurity;
        private int penaltyForWeakGlobalSecurity;

    }

    @Setter
    @Getter
    public static class BestPracticeValidation {
        private boolean requireVersioning;
        private boolean requireServersArray;
        private boolean requireTags;
        private boolean requireComponentReuse;
        private boolean requireOperationIds;
        private int minimumReusableComponents;
    }

}
