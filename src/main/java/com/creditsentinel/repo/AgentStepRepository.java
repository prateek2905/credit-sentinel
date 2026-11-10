package com.creditsentinel.repo;

import com.creditsentinel.domain.AgentStep;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentStepRepository extends JpaRepository<AgentStep, UUID> {
    List<AgentStep> findByCaseIdOrderByStepIndexAsc(UUID caseId);

    void deleteByCaseId(UUID caseId);
}
