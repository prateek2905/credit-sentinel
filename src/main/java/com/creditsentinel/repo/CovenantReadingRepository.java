package com.creditsentinel.repo;

import com.creditsentinel.domain.CovenantReading;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CovenantReadingRepository extends JpaRepository<CovenantReading, UUID> {
    Optional<CovenantReading> findFirstByCovenantIdOrderByAsOfDateDesc(UUID covenantId);
}
