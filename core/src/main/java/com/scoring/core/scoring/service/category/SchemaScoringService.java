package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.category.SchemaData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Service;

import static com.scoring.core.scoring.service.APIParserHelper.*;

@Service
public class SchemaScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public SchemaScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getSchemaAndTypes();
        SchemaData data = new SchemaData();
        data.setPoints(maxPoints);
        data.setSpec(spec);

        // Check if components/schemas section is defined
        if (scoringConfig.getValidation().getSchema().isRequireSchemaComponents()){
            checkComponentsSchemas(spec, data);
        }

        // Check request body schemas
        if(scoringConfig.getValidation().getSchema().isRequireRequestBodySchema()) {
            goOverRequestContents(spec, this::checkRequestBodySchema, data);
        }

        // Check response schemas
        if(scoringConfig.getValidation().getSchema().isRequireResponseBodySchema()) {
            goOverResponseContents(spec, this::checkResponseBodySchema, data);
        }

        summarize(spec, data);

        return data.buildScore(maxPoints, "Schema & Types");
    }

    private void checkComponentsSchemas(OpenAPI spec, SchemaData data) {
        // Check if components/schemas section is defined
        if (
                spec.getComponents() == null ||
                spec.getComponents().getSchemas() == null ||
                spec.getComponents().getSchemas().isEmpty()
        ) {
            data.setPoints(data.getPoints() - scoringConfig.getValidation().getSchema().getPenaltyForMissingSchema());
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/components/schemas")
                    .description("No schema components defined")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define reusable schema components in the components/schemas section")
                    .build());

            return;
        }

        // Validate schema objects
        goOverSchemas(spec, (schemaName, schema, d) -> {
            String schemaType = schema.getType();
            d.setTotalSchemas(d.getTotalSchemas() + 1);
            if (schemaType == null){
                d.getIssues().add(CategoryScore.Issue.builder()
                        .location("#/components/schemas/" + schemaName)
                        .description("Missing data type for schema (still infered as object)")
                        .severity(CategoryScore.Severity.LOW)
                        .suggestion("Define an 'object' data type for the schema")
                        .build());
            }
        }, data);

        // Validate schema properties
        goOverSchemaProperties(spec, (schemaName, propertyName, schema, d) -> {
            String schemaType = parseType(schema);

            boolean hasRef = doesReferenceExist(d.getSpec(), schema.get$ref());
            boolean hasType = schemaType != null;
            boolean hasValidType = hasType && scoringConfig.getValidation().getSchema()
                    .getRequiredDataTypes().contains(schemaType);

            d.setTotalSchemas(d.getTotalSchemas() + 1);

            String location = String.format("#/components/schemas/%s/properties/%s", schemaName, propertyName);

            if (!hasRef && !hasType) {
                d.setSchemaIssues(d.getSchemaIssues() + 1);
                d.getIssues().add(CategoryScore.Issue.builder()
                        .location(location)
                        .description("Property '" + propertyName + "' in schema '" + schemaName + "' has no defined data type or reference")
                        .severity(CategoryScore.Severity.HIGH)
                        .suggestion("Define a proper data type or reference for the property")
                        .build());
            } else if (!hasRef && !hasValidType) {
                d.setSchemaIssues(d.getSchemaIssues() + 1);
                d.getIssues().add(CategoryScore.Issue.builder()
                        .location(location)
                        .description("Property '" + propertyName + "' in schema '" + schemaName + "' has an invalid data type: " + schemaType)
                        .severity(CategoryScore.Severity.MEDIUM)
                        .suggestion("Use one of the required data types: " +
                                String.join(", ", scoringConfig.getValidation().getSchema().getRequiredDataTypes()))
                        .build());
            }
        }, data);
    }

    private void checkRequestBodySchema(
            String path,
            String operationId,
            String mediaTypeName,
            MediaType mediaType,
            SchemaData data
    )
    {
        data.setTotalSchemas(data.getTotalSchemas() + 1);

        String location = String.format("#/paths/%s/operations/%s/requestBody/%s",
                path, operationId, mediaTypeName);

        // Check if the media type has a schema defined
        if (mediaType.getSchema() == null) {
            data.setSchemaIssues(data.getSchemaIssues() + 1);
            data.getIssues().add(
                    CategoryScore.Issue.builder()
                            .location(location)
                            .description("Missing schema definition")
                            .severity(CategoryScore.Severity.HIGH)
                            .suggestion("Define a proper schema for the request body")
                            .build());
        } else if (
                !scoringConfig.getValidation().getSchema().isAllowedGenericSchema() &&
                        isGenericObjectSchema(data.getSpec(), mediaType.getSchema())
        ) {
            data.setSchemaIssues(data.getSchemaIssues() + 1);
            data.getIssues().add(
                    CategoryScore.Issue.builder()
                            .location(location)
                            .description("Generic object schema without properties")
                            .severity(CategoryScore.Severity.MEDIUM)
                            .suggestion("Define specific properties for the object schema or use a $ref to a component schema")
                            .build());
        }
    }

    private void checkResponseBodySchema(
            String path,
            String operationId,
            String responseCode,
            String mediaTypeName,
            MediaType mediaType,
            SchemaData data
    ) {
        data.setTotalSchemas(data.getTotalSchemas() + 1);

        String location = String.format("#/paths/%s/operations/%s/responses/%s/%s",
                path, operationId, responseCode, mediaTypeName);

        // Check if the media type has a schema defined
        if (mediaType.getSchema() == null) {
            data.setSchemaIssues(data.getSchemaIssues() + 1);
            data.getIssues().add(
                    CategoryScore.Issue.builder()
                            .location(location)
                            .description("Missing schema definition")
                            .severity(CategoryScore.Severity.HIGH)
                            .suggestion("Define a proper schema for the response body")
                            .build());
        } else if (
                !scoringConfig.getValidation().getSchema().isAllowedGenericSchema() &&
                        isGenericObjectSchema(data.getSpec(), mediaType.getSchema())) {
            data.setSchemaIssues(data.getSchemaIssues() + 1);
            data.getIssues().add(
                    CategoryScore.Issue.builder()
                            .location(location)
                            .description("Generic object schema without properties")
                            .severity(CategoryScore.Severity.MEDIUM)
                            .suggestion("Define specific properties for the object schema or use a $ref to a component schema")
                            .build());
        }
    }

    private void summarize(OpenAPI spec, SchemaData data) {
        if (spec.getPaths() != null) {
            if (data.getTotalSchemas() > 0) {
                // Calculate score reduction based on schema issues
                double schemaQualityRatio = 1.0 - ((double) data.getSchemaIssues() / data.getTotalSchemas());
                data.setPoints((int) (data.getPoints() * schemaQualityRatio));

                if (data.getSchemaIssues() == 0) {
                    data.getStrengths().add("All schemas have proper data types");
                } else {
                    // Additional summary for overall schema quality
                    CategoryScore.Severity overallSeverity = data.getSchemaIssues() > data.getTotalSchemas() * 0.5 ?
                            CategoryScore.Severity.HIGH : CategoryScore.Severity.MEDIUM;

                    data.getIssues().add(CategoryScore.Issue.builder()
                            .location("#/paths")
                            .description(String.format("Schema quality issues detected in %d out of %d schemas (%.1f%%)",
                                    data.getSchemaIssues(),
                                    data.getTotalSchemas(),
                                    (double) data.getSchemaIssues() / data.getTotalSchemas() * 100))
                            .severity(overallSeverity)
                            .suggestion("Review and improve schema definitions to ensure proper typing")
                            .build());
                }
            }
        }
        else {
            data.setPoints(0);
            data.getIssues().add(CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("No paths defined in the OpenAPI specification")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Define at least one path with operations and schemas")
                    .build());
        }
    }

    private boolean isGenericObjectSchema(OpenAPI spec, Schema<?> schema) {
        String schemaType = parseType(schema);
        return (schemaType == null && doesReferenceExist(spec, schema.get$ref())) ||
                ("object".equals(schemaType) &&
                        (schema.getProperties() == null || schema.getProperties().isEmpty()));
    }

}
