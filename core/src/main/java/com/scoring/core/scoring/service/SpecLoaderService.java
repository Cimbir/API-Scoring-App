package com.scoring.core.scoring.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;

@Service
public class SpecLoaderService {
    private final OpenAPIV3Parser parser;

    public SpecLoaderService() {
        this.parser = new OpenAPIV3Parser();
    }

    public OpenAPI load(String specLocation) {
        SwaggerParseResult result = parser.readLocation(specLocation, null, null);

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            throw new IllegalArgumentException("Failed to load OpenAPI spec: " + String.join(", ", result.getMessages()));
        }

        return result.getOpenAPI();
    }
}
