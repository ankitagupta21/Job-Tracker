package com.jobtracker.backend.ai;

import com.jobtracker.backend.api.response.ApiResponse;
import com.jobtracker.backend.utils.EmailParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIController {

    private final OllamaService ollamaService;
    private final EmailParser emailParser;

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

        String prompt = """
                You are a resume expert. Analyze the match between this resume and job description.
                Return ONLY a JSON object:
                {
                  "matchScore": 0-100,
                  "strongPoints": ["skill1", "skill2"],
                  "missingSkills": ["skill1", "skill2"],
                  "recommendation": "Apply / Consider / Skip",
                  "reason": "one sentence explanation"
                }

                Job Description: %s

                Resume: %s
                """.formatted(jobDescription, resume);

        String result = ollamaService.generate(prompt);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/interview-coach")
    public ResponseEntity<ApiResponse<String>> interviewCoach(
            @RequestBody Map<String, String> request) {
        String company = request.getOrDefault("company", "");
        String role = request.getOrDefault("role", "");

        String prompt = """
                You are an interview coach. For a %s interview at %s, return ONLY JSON:
                {
                  "commonQuestions": ["question1", "question2", "question3"],
                  "technicalTopics": ["topic1", "topic2", "topic3"],
                  "tips": ["tip1", "tip2"],
                  "redFlags": ["what to avoid1", "what to avoid2"]
                }
                """.formatted(role, company);

        String result = ollamaService.generate(prompt);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}