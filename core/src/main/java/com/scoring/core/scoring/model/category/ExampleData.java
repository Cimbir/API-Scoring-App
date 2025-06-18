package com.scoring.core.scoring.model.category;

import com.scoring.core.scoring.model.CategoryScoreData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExampleData extends CategoryScoreData {
    private int totalMediaTypes = 0;
    private int mediaTypesWithExamples = 0;
}
