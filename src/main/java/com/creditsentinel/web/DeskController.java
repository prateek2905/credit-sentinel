package com.creditsentinel.web;

import com.creditsentinel.service.DecisionService;
import com.creditsentinel.service.InvestigationService;
import com.creditsentinel.service.NotFoundException;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DeskController {

    private final DeskQuery deskQuery;
    private final InvestigationService investigations;
    private final DecisionService decisions;

    public DeskController(DeskQuery deskQuery, InvestigationService investigations, DecisionService decisions) {
        this.deskQuery = deskQuery;
        this.investigations = investigations;
        this.decisions = decisions;
    }

    @GetMapping("/")
    public String desk(Model model) {
        model.addAttribute("desk", deskQuery.desk());
        return "desk";
    }

    @PostMapping("/alerts/{alertId}/investigate")
    public String investigate(@PathVariable UUID alertId, RedirectAttributes redirect) {
        try {
            UUID caseId = investigations.investigate(alertId);
            return "redirect:/cases/" + caseId;
        } catch (NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/cases/{caseId}")
    public String showCase(@PathVariable UUID caseId, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("creditCase", deskQuery.casePage(caseId));
            if (!model.containsAttribute("decisionForm")) {
                model.addAttribute("decisionForm", new DecisionForm());
            }
            return "case";
        } catch (NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/cases/{caseId}/decision")
    public String decide(
            @PathVariable UUID caseId,
            @Valid @ModelAttribute("decisionForm") DecisionForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("creditCase", deskQuery.casePage(caseId));
            return "case";
        }
        try {
            decisions.decide(caseId, form.getAnalystId(), form.getDecision(), form.getComment());
            return "redirect:/cases/" + caseId;
        } catch (IllegalStateException | NotFoundException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/cases/" + caseId;
        }
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) UUID caseId, Model model) {
        model.addAttribute("steps", deskQuery.audit(caseId));
        model.addAttribute("caseId", caseId);
        return "audit";
    }
}
