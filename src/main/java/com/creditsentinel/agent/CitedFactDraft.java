package com.creditsentinel.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CitedFactDraft(String name, String value, String sourceTool) {
}
