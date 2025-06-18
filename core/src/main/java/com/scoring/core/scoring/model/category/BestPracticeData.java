package com.scoring.core.scoring.model.category;

import com.scoring.core.scoring.model.CategoryScoreData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BestPracticeData extends CategoryScoreData {
    private int total = 0;
    private int passed = 0;

    private boolean usesTags = false;
    private List<String> untaggedOperations = new ArrayList<>();

    private boolean hasOperationIds = false;
    private List<String> operationsWithoutIds = new ArrayList<>();
}
