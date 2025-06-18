package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.config.ScoringConfig;
import com.scoring.core.scoring.model.CategoryScore;
import com.scoring.core.scoring.model.category.ExampleData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import org.springframework.stereotype.Service;

import static com.scoring.core.scoring.service.APIParserHelper.*;

@Service
public class ExampleScoringService implements CategoryScoringService {
    private final ScoringConfig scoringConfig;

    public ExampleScoringService(ScoringConfig scoringConfig) {
        this.scoringConfig = scoringConfig;
    }

    @Override
    public CategoryScore scoreCategory(OpenAPI spec) {
        int maxPoints = scoringConfig.getWeights().getExamplesAndSamples();
        ExampleData data = new ExampleData();
        data.setPoints(maxPoints);

        if(scoringConfig.getValidation().getExample().isRequireRequestExamples()){
            goOverRequestContents(spec, this::checkRequestBodyExample, data);
        }
        if(scoringConfig.getValidation().getExample().isRequireResponseExamples()){
            goOverResponseContents(spec, this::checkResponseExample, data);
        }

        summarize(spec, data);

        return data.buildScore(maxPoints, "Examples & Samples");
    }

    private void summarize(OpenAPI spec, ExampleData data) {
        if (spec.getPaths() != null && !spec.getPaths().isEmpty()) {
            double exampleScore =
                    data.getTotalMediaTypes() == 0 ?
                            1 :
                            (double) data.getMediaTypesWithExamples() / data.getTotalMediaTypes();
            data.setPoints((int) (data.getPoints() * exampleScore));

            if (exampleScore > scoringConfig.getValidation().getExample().getMinimumExampleCoverage()) {
                data.getStrengths().add(String.format(
                        "Good coverage of request/response examples: %d%%",
                        (int) (exampleScore * 100)));
            }
        }else{
            data.setPoints(0);
            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location("#/paths")
                    .description("No paths found in the OpenAPI specification")
                    .severity(CategoryScore.Severity.HIGH)
                    .suggestion("Add paths with operations to the OpenAPI specification")
                    .build();
            data.getIssues().add(issue);
        }
    }

    private void checkRequestBodyExample(String path, String operationId, String contentType, MediaType mediaType, ExampleData data) {
        if (hasExamples(mediaType)) {
            data.setMediaTypesWithExamples(data.getMediaTypesWithExamples() + 1);
        } else {
            String location = String.format("#/paths/%s/operations/%s/requestBody/%s",
                    path, operationId, contentType);

            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(location)
                    .description("Request body missing examples for content type: " + contentType)
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Add example or examples property to request body media type")
                    .build();

            data.getIssues().add(issue);
        }
    }

    private void checkResponseExample(String path, String operationId, String responseCode, String contentType, MediaType mediaType, ExampleData data) {
        if (hasExamples(mediaType)) {
            data.setMediaTypesWithExamples(data.getMediaTypesWithExamples() + 1);
        } else {
            String location = String.format("#/paths/%s/operations/%s/responses/%s/content/%s",
                    path, operationId, responseCode, contentType);

            CategoryScore.Issue issue = CategoryScore.Issue.builder()
                    .location(location)
                    .description("Response (" + responseCode + ") missing examples for content type: " + contentType)
                    .severity(CategoryScore.Severity.MEDIUM)
                    .suggestion("Add example or examples property to response media type")
                    .build();

            data.getIssues().add(issue);
        }
    }

    private boolean hasExamples(MediaType mediaType) {
        return (mediaType.getExample() != null) ||
                (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty());
    }
}
