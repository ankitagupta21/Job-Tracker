package com.jobtracker.backend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.Map;

@Service
@Slf4j
public class OllamaService {

    private final RestTemplate restTemplate;

    public OllamaService() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 sec
        factory.setReadTimeout(30000); // 30 sec

        this.restTemplate = new RestTemplate(factory);
    }

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.enabled}")
    private boolean enabled;

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