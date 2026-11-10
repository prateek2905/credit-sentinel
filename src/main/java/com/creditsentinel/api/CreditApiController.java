package com.creditsentinel.api;

import com.creditsentinel.service.DecisionService;
import com.creditsentinel.service.InvestigationService;
import com.creditsentinel.web.CasePage;
import com.creditsentinel.web.DeskPage;
import com.creditsentinel.web.DeskQuery;
import com.creditsentinel.web.StepRow;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CreditApiController {

    private final DeskQuery deskQuery;
    private final InvestigationService investigations;
    private final DecisionService decisions;

    public CreditApiController(DeskQuery deskQuery, InvestigationService investigations, DecisionService decisions) {
        this.deskQuery = deskQuery;
        this.investigations = investigations;
        this.decisions = decisions;
    }

    @GetMapping("/portfolio")
    public DeskPage portfolio() {
        return deskQuery.desk();
    }

    @GetMapping("/alerts")
    public Object alerts() {
        return deskQuery.desk().alerts();
    }

    @PostMapping("/alerts/{alertId}/investigate")
    public Map<String, UUID> investigate(@PathVariable UUID alertId) {
        return Map.of("caseId", investigations.investigate(alertId));
    }

    @GetMapping("/cases/{caseId}")
    public CasePage getCase(@PathVariable UUID caseId) {
        return deskQuery.casePage(caseId);
    }

    @PostMapping("/cases/{caseId}/decision")
    public CasePage decide(@PathVariable UUID caseId, @Valid @RequestBody DecisionRequest request) {
        decisions.decide(caseId, request.analystId(), request.decision(), request.comment());
        return deskQuery.casePage(caseId);
    }

    @GetMapping("/cases/{caseId}/audit")
    public List<StepRow> audit(@PathVariable UUID caseId) {
        return deskQuery.audit(caseId);
    }
}
