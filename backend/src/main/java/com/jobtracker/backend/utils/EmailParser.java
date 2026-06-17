package com.jobtracker.backend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backend.ai.OllamaService;
import com.jobtracker.backend.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailParser {

    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedEmail parse(String subject, String from, String body) {

        // Truncate body to avoid token limits
        String truncatedBody = body.length() > 800
                ? body.substring(0, 800)
                : body;

        // Try AI parsing first
        if (ollamaService.isAvailable()) {
            ParsedEmail result = parseWithAI(subject, from, truncatedBody);
            if (result != null)
                return result;
        }

        // Fallback to keyword matching
        log.info("Falling back to keyword parser");
        return parseWithKeywords(subject, from, body);
    }

    // ── AI Parsing ───────────────────────────────────────────────────────────

    private ParsedEmail parseWithAI(String subject, String from, String body) {
        String prompt = """
                Analyze this job email. Return JSON only, no explanation.
                Subject: %s
                From: %s
                Body: %s

                JSON: {"company":"","role":"","status":"APPLIED|ONLINE_TEST|INTERVIEW|OFFERED|REJECTED|UNKNOWN","confidence":0.0,"summary":""}
                """
                .formatted(subject, from,
                        body.length() > 300 ? body.substring(0, 300) : body);

        try {
            String response = ollamaService.generate(prompt);
            if (response == null)
                return null;

            JsonNode json = objectMapper.readTree(response);

            String company = json.path("company").asText("Unknown");
            String role = json.path("role").asText("Unknown");
            String statusStr = json.path("status").asText("UNKNOWN");
            double confidence = json.path("confidence").asDouble(0.0);
            String summary = json.path("summary").asText("");

            ApplicationStatus status = parseStatus(statusStr);

            log.info("AI parsed email — Company: {}, Status: {}, Confidence: {}",
                    company, status, confidence);

            return new ParsedEmail(company, role, status, confidence, summary, true);

        } catch (Exception e) {
            log.error("AI parsing failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Keyword Fallback ─────────────────────────────────────────────────────

    private ParsedEmail parseWithKeywords(String subject, String from, String body) {
        String text = (subject + " " + body).toLowerCase();
        ApplicationStatus status = null;

        if (containsAny(text, "pleased to offer", "offer of employment",
                "congratulations", "welcome to the team")) {
            status = ApplicationStatus.OFFERED;
        } else if (containsAny(text, "schedule an interview", "interview invitation",
                "invite you for an interview", "like to speak with you")) {
            status = ApplicationStatus.INTERVIEW;
        } else if (containsAny(text, "online assessment", "coding challenge",
                "hackerrank", "codility", "technical assessment")) {
            status = ApplicationStatus.ONLINE_TEST;
        } else if (containsAny(text, "unfortunately", "not moving forward",
                "decided to move forward with other", "regret to inform")) {
            status = ApplicationStatus.REJECTED;
        } else if (containsAny(text, "thank you for applying",
                "received your application", "application has been received")) {
            status = ApplicationStatus.APPLIED;
        }

        String company = extractCompanyFromEmail(from);
        return new ParsedEmail(company, "Unknown", status, 0.6, "", false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ApplicationStatus parseStatus(String statusStr) {
        try {
            ApplicationStatus status = ApplicationStatus.valueOf(statusStr.toUpperCase());
            return status;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractCompanyFromEmail(String from) {
        try {
            if (from.contains("@")) {
                String domain = from.split("@")[1]
                        .replace(">", "").trim()
                        .split("\\.")[0];
                return domain.substring(0, 1).toUpperCase() + domain.substring(1);
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword))
                return true;
        }
        return false;
    }

    // ── Result Model ─────────────────────────────────────────────────────────

    public record ParsedEmail(
            String company,
            String role,
            ApplicationStatus status,
            double confidence,
            String summary,
            boolean parsedByAI) {
    }
}