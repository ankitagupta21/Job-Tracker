package com.jobtracker.backend.service;

import com.jobtracker.backend.constants.KafkaTopics;
import com.jobtracker.backend.entity.JobApplication;
import com.jobtracker.backend.entity.StatusHistory;
import com.jobtracker.backend.enums.ApplicationSource;
import com.jobtracker.backend.enums.ApplicationStatus;
import com.jobtracker.backend.model.CreateApplicationRequest;
import com.jobtracker.backend.model.UpdateApplicationRequest;
import com.jobtracker.backend.repository.ApplicationRepository;
import com.jobtracker.backend.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Create ──────────────────────────────────────────────────────────────

    @Transactional
    public JobApplication create(CreateApplicationRequest request) {
        // Duplicate check
        LocalDate appliedDate = request.getAppliedDate() != null
                ? request.getAppliedDate()
                : LocalDate.now();

        boolean duplicate = applicationRepository
                .existsByCompanyNameIgnoreCaseAndRoleIgnoreCaseAndAppliedDate(
                        request.getCompanyName(), request.getRole(), appliedDate);

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Application for " + request.getCompanyName() +
                            " - " + request.getRole() + " already exists.");
        }

        JobApplication app = new JobApplication();
        app.setCompanyName(request.getCompanyName());
        app.setRole(request.getRole());
        app.setJobUrl(request.getJobUrl());
        app.setNotes(request.getNotes());
        app.setAppliedDate(appliedDate);
        app.setSource(ApplicationSource.MANUAL);
        app.setStatus(ApplicationStatus.APPLIED);

        JobApplication saved = applicationRepository.save(app);

        // Publish Kafka event
        kafkaTemplate.send(KafkaTopics.APPLICATION_CREATED, saved.getId().toString(), saved);

        return saved;
    }

    // ── Read ────────────────────────────────────────────────────────────────

    public List<JobApplication> getAll() {
        return applicationRepository.findAll();
    }

    public List<JobApplication> getByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status);
    }

    public List<JobApplication> searchByCompany(String companyName) {
        return applicationRepository.findByCompanyNameContainingIgnoreCase(companyName);
    }

    public JobApplication getById(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }

    public List<StatusHistory> getHistory(UUID applicationId) {
        getById(applicationId); // validates existence
        return statusHistoryRepository.findByApplicationIdOrderByChangedAtAsc(applicationId);
    }

    // ── Update ──────────────────────────────────────────────────────────────

    @Transactional
    public JobApplication update(UUID id, UpdateApplicationRequest request) {
        JobApplication app = getById(id);

        if (request.getCompanyName() != null)
            app.setCompanyName(request.getCompanyName());
        if (request.getRole() != null)
            app.setRole(request.getRole());
        if (request.getJobUrl() != null)
            app.setJobUrl(request.getJobUrl());
        if (request.getNotes() != null)
            app.setNotes(request.getNotes());

        // Status change — log history and fire Kafka event
        if (request.getStatus() != null && !request.getStatus().equals(app.getStatus())) {
            ApplicationStatus oldStatus = app.getStatus();
            ApplicationStatus newStatus = request.getStatus();

            StatusHistory history = new StatusHistory();
            history.setApplication(app);
            history.setOldStatus(oldStatus);
            history.setNewStatus(newStatus);
            history.setChangedBy(StatusHistory.ChangeSource.MANUAL);
            history.setChangedAt(LocalDateTime.now());
            statusHistoryRepository.save(history);

            app.setStatus(newStatus);
            publishStatusEvent(newStatus, app);
        }

        app.setLastUpdated(LocalDateTime.now());
        return applicationRepository.save(app);
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        JobApplication app = getById(id);
        statusHistoryRepository.deleteAll(
                statusHistoryRepository.findByApplicationIdOrderByChangedAtAsc(id));
        applicationRepository.delete(app);
    }

    // ── Kafka helper ────────────────────────────────────────────────────────

    private void publishStatusEvent(ApplicationStatus status, JobApplication app) {
        String topic = switch (status) {
            case INTERVIEW -> KafkaTopics.APPLICATION_INTERVIEW;
            case REJECTED -> KafkaTopics.APPLICATION_REJECTED;
            case OFFERED -> KafkaTopics.APPLICATION_OFFERED;
            case ONLINE_TEST -> KafkaTopics.APPLICATION_OA;
            default -> null;
        };
        if (topic != null) {
            kafkaTemplate.send(topic, app.getId().toString(), app);
        }
    }
}