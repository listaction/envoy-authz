package com.example.splittest.repo;

import com.example.splittest.entity.Mismatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MismatchRepository extends JpaRepository<Mismatch, Long> {

    List<Mismatch> findAllByAttemptsBeforeAndResultMismatch(int attempts, boolean resultMismatch);
    List<Mismatch> findAllByAttemptsBeforeAndTagsMismatch(int attempts, boolean tagsMismatch);
}
