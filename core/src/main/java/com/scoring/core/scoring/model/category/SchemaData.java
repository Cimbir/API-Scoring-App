package com.scoring.core.scoring.model.category;

import com.scoring.core.scoring.model.CategoryScoreData;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchemaData extends CategoryScoreData {
    private int schemaIssues;
    private int totalSchemas;
    OpenAPI spec;
}
