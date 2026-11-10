package com.creditsentinel.agent;

import java.util.List;

public record VerificationResult(boolean passed, List<String> errors) {

    public static VerificationResult pass() {
        return new VerificationResult(true, List.of());
    }

    public static VerificationResult fail(List<String> errors) {
        return new VerificationResult(false, List.copyOf(errors));
    }
}
