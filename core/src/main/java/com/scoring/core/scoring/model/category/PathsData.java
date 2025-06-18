package com.scoring.core.scoring.model.category;

import com.scoring.core.scoring.model.CategoryScoreData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PathsData extends CategoryScoreData {
    private List<String> pathNames = new ArrayList<>();
}
