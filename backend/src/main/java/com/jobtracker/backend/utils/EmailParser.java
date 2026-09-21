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
        log.warn("Ollama unavailable or parsing failed — marking for manual review");
        return new ParsedEmail(extractCompanyFromEmail(from), "Unknown", null, 0.0, "", false);
    }

    // ── AI Parsing ───────────────────────────────────────────────────────────

    private ParsedEmail parseWithAI(String subject, String from, String body) {
        String prompt = """
                Analyze this email. First determine if it is a genuine, personal update about
                a specific job application the recipient submitted — NOT a newsletter,
                job-alert digest, marketing email, or general recruitment promotion.

                Return JSON only, no explanation.
                Subject: %s
                From: %s
                Body: %s

                JSON: {"isJobApplicationEmail": true|false, "company":"", "role":"", "status":"APPLIED|ONLINE_TEST|INTERVIEW|OFFERED|REJECTED|UNKNOWN", "confidence":0.0, "summary":""}
                """
                .formatted(subject, from, body.length() > 300 ? body.substring(0, 300) : body);

        try {
            String response = ollamaService.generate(prompt);
            if (response == null)
                return null;

            JsonNode json = objectMapper.readTree(response);

            String company = blankToDefault(json.path("company").asText(""), extractCompanyFromEmail(from));
            String role = blankToDefault(json.path("role").asText(""), "Unknown");

            String statusStr = json.path("status").asText("UNKNOWN");
            double confidence = json.path("confidence").asDouble(0.0);
            String summary = json.path("summary").asText("");

            ApplicationStatus status = parseStatus(statusStr);

            boolean isJobApplicationEmail = json.path("isJobApplicationEmail").asBoolean(false);
            if (!isJobApplicationEmail) {
                log.info("AI classified as non-application email — skipping");
                return new ParsedEmail(company, role, null, confidence, summary, true);
            }
            log.info("AI parsed email — Subject: {}, From: {}, Company: {}, Status: {}, Confidence: {}",
                    subject, from, company, status, confidence);

            return new ParsedEmail(company, role, status, confidence, summary, true);

        } catch (Exception e) {
            log.error("AI parsing failed: {}", e.getMessage());
            return null;
        }
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

    private String blankToDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
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