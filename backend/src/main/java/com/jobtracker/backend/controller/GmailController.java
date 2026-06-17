package com.jobtracker.backend.controller;

import com.jobtracker.backend.api.response.ApiResponse;
import com.jobtracker.backend.gmail.GmailService;
import com.jobtracker.backend.scheduler.GmailScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GmailController {

    private final GmailService gmailService;
    private final GmailScheduler gmailScheduler;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        boolean connected = gmailService.isConnected();
        String email = connected ? gmailService.getConnectedEmail() : null;
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "connected", connected,
                "email", email != null ? email : "")));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Void>> sync() {
        gmailScheduler.scanGmail();
        return ResponseEntity.ok(ApiResponse.success("Sync triggered", null));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        gmailService.disconnect();
        return ResponseEntity.ok(ApiResponse.success("Gmail disconnected", null));
    }
}