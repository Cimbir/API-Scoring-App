package com.scoring.core.scoring.model.category;

import com.scoring.core.scoring.model.CategoryScoreData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseData extends CategoryScoreData {
    private int totalOperations = 0;
    private int operationsWithProperCodes = 0;
}
