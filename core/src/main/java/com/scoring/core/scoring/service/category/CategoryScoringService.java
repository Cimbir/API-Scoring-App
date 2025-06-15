package com.scoring.core.scoring.service.category;

import com.scoring.core.scoring.model.CategoryScore;
import io.swagger.v3.oas.models.OpenAPI;

public interface CategoryScoringService {
    CategoryScore scoreCategory(OpenAPI spec);
}
