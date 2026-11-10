package com.creditsentinel.config;

import com.creditsentinel.tools.BorrowerProfileTool;
import com.creditsentinel.tools.CovenantStatusTool;
import com.creditsentinel.tools.FacilityExposureTool;
import com.creditsentinel.tools.PriorCasesTool;
import com.creditsentinel.tools.RatingHistoryTool;
import com.creditsentinel.tools.SectorConcentrationTool;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    @Bean
    List<ToolCallback> creditToolCallbacks(
            BorrowerProfileTool borrowerProfileTool,
            FacilityExposureTool facilityExposureTool,
            CovenantStatusTool covenantStatusTool,
            RatingHistoryTool ratingHistoryTool,
            SectorConcentrationTool sectorConcentrationTool,
            PriorCasesTool priorCasesTool) {
        return Arrays.asList(ToolCallbacks.from(
                borrowerProfileTool,
                facilityExposureTool,
                covenantStatusTool,
                ratingHistoryTool,
                sectorConcentrationTool,
                priorCasesTool));
    }
}
