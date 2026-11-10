package com.creditsentinel.repo;

import com.creditsentinel.domain.Covenant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CovenantRepository extends JpaRepository<Covenant, UUID> {
    List<Covenant> findByFacilityId(UUID facilityId);
}
