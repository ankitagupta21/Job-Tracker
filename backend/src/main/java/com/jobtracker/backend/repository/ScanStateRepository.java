package com.jobtracker.backend.repository;

import com.jobtracker.backend.entity.ScanState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScanStateRepository extends JpaRepository<ScanState, UUID> {
}
