package com.scoring.core.scoring.controller;

import com.scoring.core.scoring.model.ErrorResponse;
import com.scoring.core.scoring.model.SpecScore;
import com.scoring.core.scoring.model.exception.OpenAPILoadException;
import com.scoring.core.scoring.service.APIScoringService;
import com.scoring.core.scoring.service.SpecLoaderService;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scoring")
public class ScoringController {

    @Autowired
    private final APIScoringService apiScoringService;

    @Autowired
    private final SpecLoaderService specLoaderService;

    @PostMapping("/score-input")
    public ResponseEntity<?> scoreInput(@RequestBody String raw) {
        try {
            OpenAPI spec = specLoaderService.readJson(raw);
            SpecScore score = apiScoringService.score(spec);
            return ResponseEntity.ok(score);
        } catch (OpenAPILoadException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid OpenAPI JSON or YAML", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        }
    }

    @PostMapping("/score-uri-or-local")
    public ResponseEntity<?> scoreUriOrLocal(@RequestBody String loc) {
        try {
            OpenAPI spec = specLoaderService.load(loc);
            SpecScore score = apiScoringService.score(spec);
            return ResponseEntity.ok(score);
        } catch (OpenAPILoadException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid URI or Local", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Internal Server Error", e.getMessage()));
        }

    }
}
