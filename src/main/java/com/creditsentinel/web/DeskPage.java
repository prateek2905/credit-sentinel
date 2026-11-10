package com.creditsentinel.web;

import java.util.List;

public record DeskPage(List<AlertRow> alerts, List<FacilityRow> facilities, List<DecisionRow> decisions) {
}
