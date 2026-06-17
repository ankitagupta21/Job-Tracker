package com.jobtracker.backend.scheduler;

import com.google.api.services.gmail.model.Message;
import com.jobtracker.backend.entity.GmailThread;
import com.jobtracker.backend.entity.JobApplication;
import com.jobtracker.backend.entity.StatusHistory;
import com.jobtracker.backend.enums.ApplicationSource;
import com.jobtracker.backend.enums.ApplicationStatus;
import com.jobtracker.backend.gmail.GmailService;
import com.jobtracker.backend.repository.ApplicationRepository;
import com.jobtracker.backend.repository.GmailThreadRepository;
import com.jobtracker.backend.repository.StatusHistoryRepository;
import com.jobtracker.backend.utils.EmailParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GmailScheduler {

    private final GmailService gmailService;
    private final EmailParser emailParser;
    private final ApplicationRepository applicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final GmailThreadRepository gmailThreadRepository;

    @Value("${gmail.scan.enabled}")
    private boolean scanEnabled;

    @Scheduled(fixedDelay = 1800000) // every 30 minutes
    public void scanGmail() {
        if (!scanEnabled || !gmailService.isConnected()) {
            log.info("Gmail scan skipped — not connected");
            return;
        }

        log.info("Starting Gmail scan...");
        try {
            List<Message> messages = gmailService.fetchRecentMessages(50);
            int processed = 0;

            for (Message message : messages) {
                String threadId = message.getThreadId();

                // Skip already processed threads
                if (gmailThreadRepository.existsByThreadId(threadId))
                    continue;

                String subject = gmailService.getSubject(message);
                String from = gmailService.getFrom(message);
                String body = gmailService.getBody(message);

                EmailParser.ParsedEmail parsed = emailParser.parse(subject, from, body);

                ApplicationStatus detectedStatus = parsed.status();

                if (detectedStatus == null) {
                    saveNeedsReview(threadId, subject, from);
                    continue;
                }

                // Skip low confidence AI results
                if (parsed.parsedByAI() && parsed.confidence() < 0.6) {
                    saveNeedsReview(threadId, subject, from);
                    continue;
                }

                JobApplication app = findOrCreateApplication(
                        parsed.company(), parsed.role(), detectedStatus);

                // Update status if changed
                if (!app.getStatus().equals(detectedStatus)) {
                    ApplicationStatus oldStatus = app.getStatus();
                    app.setStatus(detectedStatus);
                    app.setLastUpdated(LocalDateTime.now());
                    applicationRepository.save(app);

                    // Log history
                    StatusHistory history = new StatusHistory();
                    history.setApplication(app);
                    history.setOldStatus(oldStatus);
                    history.setNewStatus(detectedStatus);
                    history.setChangedBy(StatusHistory.ChangeSource.AUTO);
                    history.setChangedAt(LocalDateTime.now());
                    statusHistoryRepository.save(history);
                }

                // Save thread reference
                GmailThread thread = new GmailThread();
                thread.setApplication(app);
                thread.setThreadId(threadId);
                thread.setSubject(subject);
                thread.setReceivedAt(LocalDateTime.now());
                gmailThreadRepository.save(thread);

                processed++;
            }

            log.info("Gmail scan complete. Processed {} new emails.", processed);
        } catch (Exception e) {
            log.error("Gmail scan failed", e);
        }
    }

    private JobApplication findOrCreateApplication(String companyName,
            String role,
            ApplicationStatus status) {
        return applicationRepository
                .findByCompanyNameIgnoreCase(companyName)
                .stream().findFirst()
                .orElseGet(() -> {
                    JobApplication app = new JobApplication();
                    app.setCompanyName(companyName);
                    app.setRole(role != null ? role : "Unknown");
                    app.setSource(ApplicationSource.GMAIL);
                    app.setStatus(status);
                    app.setAppliedDate(LocalDate.now());
                    return applicationRepository.save(app);
                });
    }

    private void saveNeedsReview(String threadId, String subject, String from) {
        log.info("Needs review: {} from {}", subject, from);
        // Will implement needs-review queue in polish phase
    }
}