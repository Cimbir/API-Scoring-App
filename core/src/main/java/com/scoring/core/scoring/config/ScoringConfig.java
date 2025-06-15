package com.scoring.core.scoring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Parser configuration
     */
    private ParserConfig parser;

    /**
     * Output configuration
     */
    private OutputConfig output;

    /**
     * Category scoring weights
     */
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

        private double categoryMinimumPercentage;
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
        private boolean allowGenericObjects;
        private List<String> requiredDataTypes;
        private int penaltyForMissingSchema;

    }

    @Setter
    @Getter
    public static class DescriptionValidation {
        private int minimumDescriptionLength;
        private boolean requireOperationDescriptions;
        private boolean requireParameterDescriptions;
        private boolean requireResponseDescriptions;
        private boolean requireSchemaDescriptions;
        private boolean requireRequestDescriptions;

    }

    @Setter
    @Getter
    public static class PathValidation {
        private List<String> allowedNamingConventions;
        private int penaltyForNamingConventionMismatch;
        private boolean enforceCrudOperationConventions;
        private int penaltyForMissingCrudOperations;
        private boolean checkForRedundantPaths;
        private double pathSimilarityThreshold;
        private int penaltyForRedundantPaths;

    }

    @Setter
    @Getter
    public static class ResponseValidation {
        private boolean requireSuccessResponses;
        private boolean requireErrorResponses;
        private List<String> requiredSuccessCodes;
        private List<String> requiredErrorCodes;
        private boolean requireDefaultResponse;

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
        private int minimumReusableComponents;
        private boolean requireContactInfo;
        private boolean requireLicenseInfo;

    }

    /**
     * Parser configuration
     */
    @Setter
    @Getter
    public static class ParserConfig {
        private int connectionTimeoutMs = 10000;
        private int readTimeoutMs = 30000;
        private boolean followRedirects = true;
        private int maxRedirects = 5;
        private Map<String, String> defaultHeaders = new HashMap<>();
        private boolean validateSpec = true;
        private boolean resolveReferences = true;

        public ParserConfig() {
            defaultHeaders.put("User-Agent", "OpenAPI-Scorer/1.0");
            defaultHeaders.put("Accept", "application/json,application/yaml,text/yaml");
        }

    }

    /**
     * Output configuration
     */
    @Setter
    @Getter
    public static class OutputConfig {
        private boolean includeRecommendations = true;
        private boolean includeDetailedBreakdown = true;
        private boolean includeProgressBars = true;
        private String outputFormat = "console"; // console, json, html
        private boolean saveReport = false;
        private String reportDirectory = "./reports";

    }
}
