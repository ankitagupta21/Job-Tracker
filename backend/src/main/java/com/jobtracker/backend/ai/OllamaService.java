package com.jobtracker.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class OllamaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.enabled}")
    private boolean enabled;

    public OllamaService() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String generate(String prompt) {
        if (!enabled)
            return null;

        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "format", "json");

            Map response = restTemplate.postForObject(
                    baseUrl + "/api/generate", request, Map.class);

            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
        } catch (Exception e) {
            log.error("Ollama call failed: {}", e.getMessage());
        }
        return null;
    }

    public boolean isAvailable() {
        try {
            restTemplate.getForObject(baseUrl + "/api/tags", Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}