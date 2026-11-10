package com.creditsentinel.repo;

import com.creditsentinel.domain.HumanDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HumanDecisionRepository extends JpaRepository<HumanDecision, UUID> {
    Optional<HumanDecision> findByCreditCaseId(UUID caseId);

    List<HumanDecision> findAllByOrderByDecidedAtDesc();
}
