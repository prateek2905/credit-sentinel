package com.creditsentinel.repo;

import com.creditsentinel.domain.AlertStatus;
import com.creditsentinel.domain.BreachType;
import com.creditsentinel.domain.CreditAlert;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAlertRepository extends JpaRepository<CreditAlert, UUID> {
    Optional<CreditAlert> findFirstByFacilityIdAndBreachTypeAndStatusNot(
            UUID facilityId, BreachType breachType, AlertStatus status);

    List<CreditAlert> findByStatusNotOrderByCreatedAtAsc(AlertStatus status);

    List<CreditAlert> findByFacilityId(UUID facilityId);
}
