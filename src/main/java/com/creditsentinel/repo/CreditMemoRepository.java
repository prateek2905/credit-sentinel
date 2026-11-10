package com.creditsentinel.repo;

import com.creditsentinel.domain.CreditMemo;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditMemoRepository extends JpaRepository<CreditMemo, UUID> {
    Optional<CreditMemo> findByCreditCaseId(UUID caseId);

    void deleteByCreditCaseId(UUID caseId);
}
