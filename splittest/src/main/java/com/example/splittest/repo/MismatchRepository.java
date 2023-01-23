package com.example.splittest.repo;

import com.example.splittest.entity.Mismatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MismatchRepository extends JpaRepository<Mismatch, Long> {

  List<Mismatch> findAllByAttemptsBeforeAndResultMismatch(int attempts, boolean resultMismatch);

  List<Mismatch> findAllByAttemptsBeforeAndTagsMismatch(int attempts, boolean tagsMismatch);
}
