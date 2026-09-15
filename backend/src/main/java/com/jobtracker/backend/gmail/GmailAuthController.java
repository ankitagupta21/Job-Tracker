package com.jobtracker.backend.gmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GmailAuthController {

    private final GmailService gmailService;

    @Value("${gmail.redirect-uri}")
    private String redirectUri;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @GetMapping("/auth/gmail")
    public ResponseEntity<Void> authorize() {
        try {
            String authorizationUrl = gmailService.getAuthorizationUrl(redirectUri);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, authorizationUrl)
                    .build();
        } catch (Exception e) {
            log.error("Failed to build Gmail authorization URL", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, frontendBaseUrl + "/settings?gmail=error")
                    .build();
        }
    }

    @GetMapping("/auth/gmail/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        if (error != null || code == null) {
            log.warn("Gmail OAuth callback returned an error: {}", error);
            return redirectTo(frontendBaseUrl + "/settings?gmail=error");
        }

        try {
            gmailService.exchangeCodeForToken(code, redirectUri);
            return redirectTo(frontendBaseUrl + "/settings?gmail=connected");
        } catch (Exception e) {
            log.error("Gmail OAuth token exchange failed", e);
            return redirectTo(frontendBaseUrl + "/settings?gmail=error");
        }
    }

    @GetMapping("/auth/gmail/status")
    public ResponseEntity<GmailStatus> status() {
        boolean connected = gmailService.isConnected();
        String email = connected ? gmailService.getConnectedEmail() : null;
        return ResponseEntity.ok(new GmailStatus(connected, email));
    }

    @PostMapping("/auth/gmail/disconnect")
    public ResponseEntity<GmailStatus> disconnect() {
        gmailService.disconnect();
        return ResponseEntity.ok(new GmailStatus(false, null));
    }

    public record GmailStatus(boolean connected, String email) {}

    private ResponseEntity<Void> redirectTo(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(location))
                .build();
    }
}