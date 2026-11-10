package com.creditsentinel.repo;

import com.creditsentinel.domain.CaseStatus;
import com.creditsentinel.domain.CreditCase;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCaseRepository extends JpaRepository<CreditCase, UUID> {
    Optional<CreditCase> findByAlertId(UUID alertId);

    List<CreditCase> findByBorrowerIdAndStatusIn(UUID borrowerId, Collection<CaseStatus> statuses);
}
