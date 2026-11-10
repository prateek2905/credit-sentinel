package com.creditsentinel.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.creditsentinel.domain.Confidence;
import com.creditsentinel.domain.Recommendation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CitationVerifierTest {

    private final CitationVerifier verifier = new CitationVerifier(new ObjectMapper());

    @Test
    void acceptsANumberTheToolReturned() {
        VerificationResult result = verifier.verify(memo("1.0800"), List.of(evidence("1.0800")));
        assertTrue(result.passed());
    }

    @Test
    void rejectsAUtilizationFigureNoToolReturned() {
        VerificationResult result = verifier.verify(memo("9.99"), List.of(evidence("1.0800")));
        assertFalse(result.passed());
        assertTrue(result.errors().get(0).contains("9.99"));
    }

    @Test
    void rejectsACitationForAToolThatWasNotCalled() {
        MemoDraft draft = new MemoDraft(
                "summary",
                List.of(new CitedFactDraft("latestRating", "BBB", "getRatingHistory")),
                List.of(),
                Recommendation.WATCH,
                List.of(),
                Confidence.LOW);
        assertFalse(verifier.verify(draft, List.of(evidence("1.0800"))).passed());
    }

    private static MemoDraft memo(String utilization) {
        return new MemoDraft(
                "summary",
                List.of(new CitedFactDraft("utilization", utilization, "getFacilityExposure")),
                List.of(),
                Recommendation.REDUCE_LIMIT,
                List.of(),
                Confidence.MEDIUM);
    }

    private static ToolEvidence evidence(String utilization) {
        return new ToolEvidence(
                "getFacilityExposure",
                "{}",
                "{\"utilization\":\"" + utilization + "\",\"committedLimit\":\"25000000.00\"}");
    }
}
