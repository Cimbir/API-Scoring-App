package com.scoring.core.scoring.service;

import com.scoring.core.scoring.model.exception.OpenAPILoadException;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpecLoaderServiceTest {
    private static SpecLoaderService specLoaderService;

    @BeforeAll
    public static void setUp() {
        specLoaderService = new SpecLoaderService();
    }

    @Test
    public void testLoadJsonSpecFromUri() {
        String specLocation = "https://petstore3.swagger.io/api/v3/openapi.json";
        OpenAPI openAPI = specLoaderService.load(specLocation);

        assert openAPI != null : "OpenAPI should not be null";
        assert openAPI.getInfo() != null : "OpenAPI info should not be null";
        assert openAPI.getInfo().getTitle() != null : "OpenAPI title should not be null";
        assert openAPI.getInfo().getTitle().contains("Swagger Petstore") : "OpenAPI title should contain 'Swagger Petstore'";
    }

    @Test
    public void testLoadYamlSpecFromUri() {
        String specLocation = "https://petstore3.swagger.io/api/v3/openapi.yaml";
        OpenAPI openAPI = specLoaderService.load(specLocation);

        assert openAPI != null : "OpenAPI should not be null";
        assert openAPI.getInfo() != null : "OpenAPI info should not be null";
        assert openAPI.getInfo().getTitle() != null : "OpenAPI title should not be null";
        assert openAPI.getInfo().getTitle().contains("Swagger Petstore") : "OpenAPI title should contain 'Swagger Petstore'";
    }

    @Test
    public void testLoadJsonSpecFromLocal() {
        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.json");
        OpenAPI openAPI = specLoaderService.load(specLocation);

        assert openAPI != null : "OpenAPI should not be null";
        assert openAPI.getInfo() != null : "OpenAPI info should not be null";
        assert openAPI.getInfo().getTitle() != null : "OpenAPI title should not be null";
        assert openAPI.getInfo().getTitle().contains("Train Travel API") : "OpenAPI title should contain 'Train Travel API'";
    }

    @Test
    public void testLoadYamlSpecFromLocal() {
        String specLocation = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/train-travel-api-openapi-source.yaml");
        OpenAPI openAPI = specLoaderService.load(specLocation);

        assert openAPI != null : "OpenAPI should not be null";
        assert openAPI.getInfo() != null : "OpenAPI info should not be null";
        assert openAPI.getInfo().getTitle() != null : "OpenAPI title should not be null";
        assert openAPI.getInfo().getTitle().contains("Train Travel API") : "OpenAPI title should contain 'Train Travel API'";
    }

    @Test
    public void testLoadSpecInvalidUri() {
        String invalidUri = "https://invalid-uri.com/openapi.json";
        try {
            specLoaderService.load(invalidUri);
            assert false : "Expected an exception for invalid URI";
        } catch (OpenAPILoadException e) {
            assert e.getMessage().contains("Failed to load OpenAPI spec") : "Exception message should indicate failure to load spec";
        }
    }

    @Test
    public void testLoadSpecInvalidLocal() {
        String invalidPath = String.format(
                "%s%s",
                Paths.get("").toAbsolutePath(),
                "/src/test/resources/invalid-openapi.json");
        try {
            specLoaderService.load(invalidPath);
            assert false : "Expected an exception for invalid local file";
        } catch (OpenAPILoadException e) {
            assert e.getMessage().contains("Failed to load OpenAPI spec") : "Exception message should indicate failure to load spec";
        }
    }

    @Test
    public void testReadJsonSpec() throws IOException {
        String rawJson = Files.readString(Path.of("src/test/resources/train-travel-api-openapi-source.json"));
        OpenAPI openAPI = specLoaderService.readJson(rawJson);

        assert openAPI != null : "OpenAPI should not be null";
        assert openAPI.getInfo() != null : "OpenAPI info should not be null";
        assert openAPI.getInfo().getTitle() != null : "OpenAPI title should not be null";
    }

    @Test
    public void testReadYamlSpec() throws IOException {
        String rawYaml = Files.readString(Path.of("src/test/resources/train-travel-api-openapi-source.yaml"));
        OpenAPI openAPI = specLoaderService.readJson(rawYaml);

        assert openAPI != null : "OpenAPI should not be null";
        assert openAPI.getInfo() != null : "OpenAPI info should not be null";
        assert openAPI.getInfo().getTitle() != null : "OpenAPI title should not be null";
    }
}
