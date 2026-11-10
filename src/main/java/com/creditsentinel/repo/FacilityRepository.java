package com.creditsentinel.repo;

import com.creditsentinel.domain.Facility;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    List<Facility> findByBorrowerId(UUID borrowerId);
}
