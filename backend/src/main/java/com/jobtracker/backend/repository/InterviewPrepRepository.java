package com.jobtracker.backend.repository;

import com.jobtracker.backend.entity.InterviewPrep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewPrepRepository extends JpaRepository<InterviewPrep, UUID> {
    Optional<InterviewPrep> findByCompanyIgnoreCaseAndRoleIgnoreCase(
            String company, String role);
}