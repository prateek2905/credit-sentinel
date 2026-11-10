package com.creditsentinel.repo;

import com.creditsentinel.domain.Borrower;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerRepository extends JpaRepository<Borrower, UUID> {
}
