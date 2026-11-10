package com.creditsentinel.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MemoParser {

    private final ObjectMapper objectMapper;

    public MemoParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParseResult parse(String content) {
        if (content == null || content.isBlank()) {
            return ParseResult.error("model returned an empty memo");
        }
        try {
            MemoDraft raw = objectMapper.readValue(unwrap(content), MemoDraft.class);
            if (raw.summary() == null || raw.summary().isBlank()) {
                return ParseResult.error("summary is blank");
            }
            if (raw.recommendation() == null || raw.confidence() == null) {
                return ParseResult.error("recommendation and confidence are required");
            }
            List<CitedFactDraft> facts = new ArrayList<>();
            if (raw.citedFacts() != null) {
                for (CitedFactDraft fact : raw.citedFacts()) {
                    if (fact == null || fact.name() == null || fact.value() == null || fact.sourceTool() == null) {
                        return ParseResult.error("a cited fact is missing name, value, or sourceTool");
                    }
                    facts.add(fact);
                }
            }
            return ParseResult.ok(new MemoDraft(
                    raw.summary().trim(),
                    List.copyOf(facts),
                    raw.riskDrivers() == null ? List.of() : List.copyOf(raw.riskDrivers()),
                    raw.recommendation(),
                    raw.missingData() == null ? List.of() : List.copyOf(raw.missingData()),
                    raw.confidence()));
        } catch (Exception exception) {
            return ParseResult.error("memo was not valid JSON: " + exception.getMessage());
        }
    }

    static String unwrap(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
        return trimmed.replaceFirst("\\s*```$", "");
    }
}
