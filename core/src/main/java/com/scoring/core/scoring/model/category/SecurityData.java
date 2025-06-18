package com.scoring.core.scoring.model.category;

import com.scoring.core.scoring.model.CategoryScoreData;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class SecurityData extends CategoryScoreData {
    private int totalSecuritySchemes = 0;
    private int wrongSecuritySchemes = 0;
    private int totalOperationsSecurity = 0;
    private int wrongOperationsSecurity = 0;
    private int totalGlobalSecurity = 0;
    private int wrongGlobalSecurity = 0;
    private Set<String> usedSchemes = new HashSet<>();
    private Set<String> securitySchemes = new HashSet<>();
}
