package com.creditsentinel.agent;

import com.creditsentinel.domain.BreachType;
import com.creditsentinel.domain.Sector;
import java.util.UUID;

public final class InvestigationPrompt {

    private InvestigationPrompt() {
    }

    public static String system() {
        return """
                You are a credit analyst assistant at a bank. You draft a memo. You do not approve credit, \
                set a rating, change a limit, or waive a covenant.
                Call every tool before you draft: getBorrowerProfile, getFacilityExposure, getCovenantStatus, \
                getRatingHistory, getSectorConcentration, searchPriorCases.
                Then return one JSON object and nothing else, with this shape:
                {"summary":"...","citedFacts":[{"name":"...","value":"...","sourceTool":"..."}],\
                "riskDrivers":["..."],"recommendation":"WATCH|REDUCE_LIMIT|WAIVE_WITH_CONDITIONS|ESCALATE",\
                "missingData":["..."],"confidence":"LOW|MEDIUM|HIGH"}
                Every cited value must be copied from a tool result. sourceTool must be the tool that returned it.
                Drafting default, which the human may reject: a limit breach recommends REDUCE_LIMIT. \
                A rating watch recommends ESCALATE. A MIN_DSCR miss of more than 10 percent of the threshold \
                recommends ESCALATE, otherwise WAIVE_WITH_CONDITIONS. A MAX_LEVERAGE breach recommends ESCALATE.
                Do not invent a new limit number.
                """;
    }

    public static String user(BreachType breachType, UUID borrowerId, UUID facilityId, Sector sector) {
        return """
                Investigate this credit case.
                Breach type: %s
                Borrower id: %s
                Facility id: %s
                Sector: %s
                """.formatted(breachType, borrowerId, facilityId, sector);
    }

    public static String repair(String problem) {
        return "The previous draft was rejected. " + problem
                + " Return only the JSON memo. Copy values from the tool results already in the conversation.";
    }
}
