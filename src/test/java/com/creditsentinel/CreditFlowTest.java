package com.creditsentinel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.creditsentinel.domain.BreachType;
import com.creditsentinel.domain.CaseStatus;
import com.creditsentinel.domain.Borrower;
import com.creditsentinel.domain.DecisionType;
import com.creditsentinel.domain.Facility;
import com.creditsentinel.repo.BorrowerRepository;
import com.creditsentinel.repo.CreditAlertRepository;
import com.creditsentinel.repo.CreditCaseRepository;
import com.creditsentinel.repo.FacilityRepository;
import com.creditsentinel.service.DecisionService;
import com.creditsentinel.service.InvestigationService;
import com.creditsentinel.web.DeskQuery;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class CreditFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private BorrowerRepository borrowers;
    @Autowired
    private FacilityRepository facilities;
    @Autowired
    private CreditAlertRepository alerts;
    @Autowired
    private CreditCaseRepository cases;
    @Autowired
    private InvestigationService investigations;
    @Autowired
    private DecisionService decisions;
    @Autowired
    private DeskQuery desk;

    @Test
    void northwindDraftIsALimitReductionAndApprovalDoesNotChangeTheLimit() {
        assertEquals(2, alerts.count());
        UUID northwindCase = investigations.investigate(alertId("Northwind Manufacturing"));
        var page = desk.casePage(northwindCase);
        assertEquals("REDUCE_LIMIT", page.memo().recommendation());
        assertEquals("PENDING_APPROVAL", page.status());
        assertTrue(page.memo().citedFacts().stream().anyMatch(fact ->
                fact.name().equals("utilization") && fact.value().equals("1.0800")));
        assertEquals(0, limitOf("Northwind Manufacturing").compareTo(new BigDecimal("25000000.00")));

        decisions.decide(northwindCase, "analyst.demo", DecisionType.APPROVE, "Agree the limit should be reviewed. Do not book a new number here.");
        assertEquals(CaseStatus.APPROVED, cases.findById(northwindCase).orElseThrow().getStatus());
        assertEquals(0, limitOf("Northwind Manufacturing").compareTo(new BigDecimal("25000000.00")));
        assertThrows(IllegalStateException.class, () ->
                decisions.decide(northwindCase, "analyst.demo", DecisionType.REJECT, "Too late."));
    }

    @Test
    void harborCovenantMissIsEscalated() {
        UUID harborCase = investigations.investigate(alertId("Harbor Retail"));
        assertEquals("ESCALATE", desk.casePage(harborCase).memo().recommendation());
        assertEquals(BreachType.COVENANT, cases.findById(harborCase).orElseThrow().getBreachType());
    }

    private UUID alertId(String borrowerName) {
        Borrower borrower = borrowers.findAll().stream()
                .filter(item -> item.getLegalName().equals(borrowerName))
                .findFirst()
                .orElseThrow();
        Facility facility = facilities.findByBorrowerId(borrower.getId()).get(0);
        return alerts.findByFacilityId(facility.getId()).get(0).getId();
    }

    private BigDecimal limitOf(String borrowerName) {
        Borrower borrower = borrowers.findAll().stream()
                .filter(item -> item.getLegalName().equals(borrowerName))
                .findFirst()
                .orElseThrow();
        return facilities.findByBorrowerId(borrower.getId()).get(0).getCommittedLimit();
    }
}
