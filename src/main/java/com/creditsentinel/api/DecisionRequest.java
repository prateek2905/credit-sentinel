package com.creditsentinel.api;

import com.creditsentinel.domain.DecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotBlank @Size(max = 80) String analystId,
        @NotNull DecisionType decision,
        @NotBlank @Size(max = 2000) String comment) {
}
