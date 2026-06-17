package com.jobtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import com.jobtracker.backend.entity.JobApplication;
import com.jobtracker.backend.enums.ApplicationStatus;

@Repository
public interface ApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByStatus(ApplicationStatus status);

    List<JobApplication> findByCompanyNameContainingIgnoreCase(String companyName);

    List<JobApplication> findByCompanyNameIgnoreCase(String companyName);

    boolean existsByCompanyNameIgnoreCaseAndRoleIgnoreCaseAndAppliedDate(
            String companyName, String role, java.time.LocalDate appliedDate);
}