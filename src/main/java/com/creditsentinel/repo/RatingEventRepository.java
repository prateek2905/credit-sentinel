package com.creditsentinel.repo;

import com.creditsentinel.domain.RatingEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingEventRepository extends JpaRepository<RatingEvent, UUID> {
    List<RatingEvent> findByBorrowerIdOrderByEffectiveDateAsc(UUID borrowerId);
}
