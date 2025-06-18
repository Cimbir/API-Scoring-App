package com.scoring.core.scoring.service;

import com.scoring.core.scoring.model.helper.*;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.Map;

public class APIParserHelper {
    public static <T> void goOverPathItems(
            OpenAPI spec,
            TriConsumer<String, PathItem, T> consumer,
            T data
    ) {
        if (spec.getPaths() != null) {
            for (Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                consumer.accept(
                        path,
                        pathItem,
                        data
                );

            }
        }
    }

    public static <T> void goOverOperations(
            OpenAPI spec,
            QuadConsumer<String, String, Operation, T> consumer,
            T data
    ) {
        goOverPathItems(spec, (path, pathItem, d) -> {
            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : pathItem.readOperationsMap().entrySet()) {
                PathItem.HttpMethod method = operationEntry.getKey();
                Operation operation = operationEntry.getValue();
                String operationId = operation.getOperationId() != null ? operation.getOperationId() : method.toString();

                consumer.accept(
                        path,
                        operationId,
                        operation,
                        d
                );
            }
        }, data);
    }

    public static <T> void goOverParameters(
            OpenAPI spec,
            QuadConsumer<String, String, Parameter, T> consumer,
            T data
    ) {
        goOverOperations(spec, (path, operationId, operation, d) -> {
                    // Check parameters
                    if (operation.getParameters() != null) {
                        for (Parameter param : operation.getParameters()) {
                            consumer.accept(
                                    path,
                                    operationId,
                                    param,
                                    d
                            );
                        }
                    }
                }, data);
    }

    public static <T> void goOverResponses(
            OpenAPI spec,
            PentaConsumer<String, String, String, ApiResponse, T> consumer,
            T data
    ) {
        goOverOperations(spec, (path, operationId, operation, d) -> {
            // Check responses
            if (operation.getResponses() != null) {
                for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                    String responseCode = responseEntry.getKey();
                    ApiResponse response = responseEntry.getValue();

                    consumer.accept(
                            path,
                            operationId,
                            responseCode,
                            response,
                            d
                    );
                }
            }
        }, data);
    }

    public static <T> void goOverSchemas(
            OpenAPI spec,
            TriConsumer<String, Schema<?>, T> consumer,
            T data
    ) {
        if (spec.getComponents() != null && spec.getComponents().getSchemas() != null) {
            for (Map.Entry<String, Schema> schemaEntry : spec.getComponents().getSchemas().entrySet()) {
                String schemaName = schemaEntry.getKey();
                Schema<?> schema = schemaEntry.getValue();

                consumer.accept(
                        schemaName,
                        schema,
                        data
                );
            }
        }
    }

    public static <T> void goOverSchemaProperties(
            OpenAPI spec,
            QuadConsumer<String, String, Schema<?>, T> consumer,
            T data
    ) {
        goOverSchemas(spec, (schemaName, schema, d) -> {
            if (schema.getProperties() != null) {
                for (Map.Entry<String, Schema> propertyEntry : schema.getProperties().entrySet()) {
                    String propertyName = propertyEntry.getKey();
                    Schema<?> propertySchema = propertyEntry.getValue();

                    consumer.accept(
                            schemaName,
                            propertyName,
                            propertySchema,
                            d
                    );
                }
            }
        }, data);
    }

    public static <T> void goOverRequestContents(
            OpenAPI spec,
            PentaConsumer<String, String, String, MediaType, T> consumer,
            T data
    ) {
        goOverOperations(spec, (path, operationId, operation, d) -> {
            // Check request body
            if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                for (Map.Entry<String, MediaType> mediaTypeEntry : operation.getRequestBody().getContent().entrySet()) {
                    String mediaType = mediaTypeEntry.getKey();
                    MediaType media = mediaTypeEntry.getValue();

                    consumer.accept(
                            path,
                            operationId,
                            mediaType,
                            media,
                            d
                    );
                }
            }
        }, data);
    }

    public static <T> void goOverResponseContents(
            OpenAPI spec,
            HexaConsumer<String, String, String, String, MediaType, T> consumer,
            T data
    ) {
        goOverResponses(spec, (path, operationId, responseCode, response, d) -> {
            // Check response content
            if (response.getContent() != null) {
                for (Map.Entry<String, MediaType> mediaTypeEntry : response.getContent().entrySet()) {
                    String mediaType = mediaTypeEntry.getKey();
                    MediaType media = mediaTypeEntry.getValue();

                    consumer.accept(
                            path,
                            operationId,
                            responseCode,
                            mediaType,
                            media,
                            d
                    );
                }
            }
        }, data);
    }

    public static <T> void goOverSecuritySchemes(
            OpenAPI spec,
            TriConsumer<String, SecurityScheme, T> consumer,
            T data
    ) {
        if (spec.getComponents() != null && spec.getComponents().getSecuritySchemes() != null) {
            for (Map.Entry<String, SecurityScheme> securitySchemeEntry : spec.getComponents().getSecuritySchemes().entrySet()) {
                String schemeName = securitySchemeEntry.getKey();
                SecurityScheme securityScheme = securitySchemeEntry.getValue();

                consumer.accept(
                        schemeName,
                        securityScheme,
                        data
                );
            }
        }
    }

    public static <T> void goOverOperationSecuritySchemes(
            OpenAPI spec,
            QuadConsumer<String, String, String, T> consumer,
            T data
    ) {
        goOverOperations(spec, (path, operationId, operation, d) -> {
            // Check security requirements
            if (operation.getSecurity() != null) {
                for (SecurityRequirement securityRequirement : operation.getSecurity()) {
                    for (String schemeName : securityRequirement.keySet()) {

                        consumer.accept(
                                path,
                                operationId,
                                schemeName,
                                d
                        );

                    }
                }
            }
        }, data);
    }

    public static <T> void goOverGlobalSecuritySchemes(
            OpenAPI spec,
            DuoConsumer<String, T> consumer,
            T data
    ) {
        if (spec.getSecurity() != null && !spec.getSecurity().isEmpty()) {
            for (SecurityRequirement securityRequirement : spec.getSecurity()) {
                for (String schemeName : securityRequirement.keySet()) {

                    consumer.accept(
                            schemeName,
                            data
                    );

                }
            }
        }
    }

    public static boolean doesReferenceExist(OpenAPI spec, String reference) {
        if (reference != null && reference.startsWith("#/components/schemas/")) {
            String schemaName = reference.substring("#/components/schemas/".length());
            return spec.getComponents() != null &&
                    spec.getComponents().getSchemas() != null &&
                    spec.getComponents().getSchemas().containsKey(schemaName);
        }
        return false;
    }

    public static String parseType(Schema<?> schema) {
        String representation = schema.toString();
        String type = null;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("type: (?:\\[([^\\]]+)\\]|([^\\s,}]+))");
        java.util.regex.Matcher matcher = pattern.matcher(representation);

        if (matcher.find()) {
            type = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }

        return type;
    }
}
