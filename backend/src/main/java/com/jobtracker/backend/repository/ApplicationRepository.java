package com.jobtracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import com.jobtracker.backend.entity.JobApplication;
import com.jobtracker.backend.enums.ApplicationStatus;

@Repository
public interface ApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByDeletedFalse();

    List<JobApplication> findByDeletedTrue();

    List<JobApplication> findByStatusAndDeletedFalse(ApplicationStatus status);

    List<JobApplication> findByCompanyNameContainingIgnoreCaseAndDeletedFalse(String companyName);

    List<JobApplication> findByCompanyNameIgnoreCaseAndDeletedFalse(String companyName);

    boolean existsByCompanyNameIgnoreCaseAndRoleIgnoreCaseAndAppliedDate(
            String companyName, String role, java.time.LocalDate appliedDate);
}