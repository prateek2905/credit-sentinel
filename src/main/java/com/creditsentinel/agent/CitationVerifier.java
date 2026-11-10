package com.creditsentinel.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CitationVerifier {

    private final ObjectMapper objectMapper;

    public CitationVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VerificationResult verify(MemoDraft draft, List<ToolEvidence> evidence) {
        List<String> errors = new ArrayList<>();
        if (draft.citedFacts().isEmpty()) {
            errors.add("memo cites no facts");
        }
        for (CitedFactDraft fact : draft.citedFacts()) {
            List<ToolEvidence> matches = evidence.stream()
                    .filter(item -> item.toolName().equals(fact.sourceTool()))
                    .toList();
            if (matches.isEmpty()) {
                errors.add(fact.name() + " cites " + fact.sourceTool() + ", which was not called");
                continue;
            }
            boolean found = matches.stream().anyMatch(item -> contains(item.outputJson(), fact.value()));
            if (!found) {
                errors.add(fact.name() + " value " + fact.value() + " was not returned by " + fact.sourceTool());
            }
        }
        return errors.isEmpty() ? VerificationResult.pass() : VerificationResult.fail(errors);
    }

    private boolean contains(String outputJson, String citedValue) {
        try {
            List<String> scalars = new ArrayList<>();
            collect(objectMapper.readTree(outputJson), scalars);
            BigDecimal citedNumber = decimalOrNull(citedValue);
            for (String scalar : scalars) {
                if (citedValue.equals(scalar)) {
                    return true;
                }
                if (citedNumber != null) {
                    BigDecimal scalarNumber = decimalOrNull(scalar);
                    if (scalarNumber != null && citedNumber.compareTo(scalarNumber) == 0) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception exception) {
            return outputJson != null && outputJson.contains(citedValue);
        }
    }

    private static void collect(JsonNode node, List<String> scalars) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.properties().forEach(entry -> collect(entry.getValue(), scalars));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collect(child, scalars));
            return;
        }
        scalars.add(node.asText());
    }

    private static BigDecimal decimalOrNull(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
