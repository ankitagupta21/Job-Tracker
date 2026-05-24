package com.jobtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import com.jobtracker.backend.entity.StatusHistory;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {
    List<StatusHistory> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);
}