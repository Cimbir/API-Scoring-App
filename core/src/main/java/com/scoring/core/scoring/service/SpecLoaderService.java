package com.scoring.core.scoring.service;

import com.scoring.core.scoring.model.exception.OpenAPILoadException;
import com.scoring.core.scoring.model.exception.OpenAPIReadException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.exception.ReadContentException;
import org.springframework.stereotype.Service;

@Service
public class SpecLoaderService {
    private final OpenAPIV3Parser parser;

    public SpecLoaderService() {
        this.parser = new OpenAPIV3Parser();
    }

    public OpenAPI load(String specLocation) {
        try {
            SwaggerParseResult result = parser.readLocation(specLocation, null, null);

            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                throw new OpenAPILoadException("Failed to load OpenAPI spec: " + String.join(", ", result.getMessages()));
            }

            return result.getOpenAPI();
        } catch (ReadContentException e) {
            return null;
        }
    }

    public OpenAPI readJson(String rawJson) {
        try {
            SwaggerParseResult result = parser.readContents(rawJson, null, null);

            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                throw new OpenAPIReadException("Invalid OpenAPI specification: " + String.join(", ", result.getMessages()));
            }

            return result.getOpenAPI();
        } catch (ReadContentException e) {
            return null;
        }
    }
}
