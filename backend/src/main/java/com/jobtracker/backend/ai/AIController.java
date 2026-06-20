package com.jobtracker.backend.ai;

import com.jobtracker.backend.api.response.ApiResponse;
import com.jobtracker.backend.entity.InterviewPrep;
import com.jobtracker.backend.repository.InterviewPrepRepository;
import com.jobtracker.backend.utils.EmailParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AIController {

    private final OllamaService ollamaService;
    private final EmailParser emailParser;
    private final InterviewPrepRepository interviewPrepRepository;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "ollamaAvailable", ollamaService.isAvailable())));
    }

    @PostMapping("/parse-email")
    public ResponseEntity<ApiResponse<EmailParser.ParsedEmail>> parseEmail(
            @RequestBody Map<String, String> request) {
        String subject = request.getOrDefault("subject", "");
        String from = request.getOrDefault("from", "");
        String body = request.getOrDefault("body", "");

        EmailParser.ParsedEmail result = emailParser.parse(subject, from, body);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/match-resume")
    public ResponseEntity<ApiResponse<String>> matchResume(
            @RequestBody Map<String, String> request) {
        String jobDescription = request.getOrDefault("jobDescription", "");
        String resume = request.getOrDefault("resume", "");

        // No cache — every JD + Resume combo is unique
        String prompt = """
                You are a resume expert. Return ONLY JSON, no explanation.
                Job Description: %s
                Resume: %s

                JSON: {"matchScore":0,"strongPoints":[],"missingSkills":[],"recommendation":"","reason":""}
                """.formatted(
                jobDescription.length() > 500 ? jobDescription.substring(0, 500) : jobDescription,
                resume.length() > 500 ? resume.substring(0, 500) : resume);

        String result = ollamaService.generate(prompt);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/interview-coach")
    public ResponseEntity<ApiResponse<String>> interviewCoach(
            @RequestBody Map<String, String> request) {

        String company = request.getOrDefault("company", "").trim();
        String role = request.getOrDefault("role", "").trim();

        // Check PostgreSQL first — company:role may already exist
        var cached = interviewPrepRepository
                .findByCompanyIgnoreCaseAndRoleIgnoreCase(company, role);

        if (cached.isPresent()) {
            log.info("Interview prep found in DB for {}:{}", company, role);
            return ResponseEntity.ok(ApiResponse.success(cached.get().getAiResponse()));
        }

        // Not in DB — generate with AI
        log.info("Generating interview prep for {}:{}", company, role);
        String prompt = """
                Interview coach for %s role at %s. Return ONLY JSON, no explanation.

                JSON: {"commonQuestions":[],"technicalTopics":[],"tips":[],"redFlags":[]}
                """.formatted(role, company);

        String result = ollamaService.generate(prompt);

        if (result != null) {
            // Store in PostgreSQL for future use
            InterviewPrep prep = new InterviewPrep();
            prep.setCompany(company);
            prep.setRole(role);
            prep.setAiResponse(result);
            interviewPrepRepository.save(prep);
            log.info("Interview prep saved to DB for {}:{}", company, role);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}