package com.creditsentinel.agent;

import com.creditsentinel.domain.Confidence;
import com.creditsentinel.domain.Recommendation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MemoDraft(
        String summary,
        List<CitedFactDraft> citedFacts,
        List<String> riskDrivers,
        Recommendation recommendation,
        List<String> missingData,
        Confidence confidence) {
}
