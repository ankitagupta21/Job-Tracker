package com.jobtracker.backend.controller;

import com.jobtracker.backend.api.response.ApiResponse;
import com.jobtracker.backend.entity.JobApplication;
import com.jobtracker.backend.entity.StatusHistory;
import com.jobtracker.backend.enums.ApplicationStatus;
import com.jobtracker.backend.model.CreateApplicationRequest;
import com.jobtracker.backend.model.UpdateApplicationRequest;
import com.jobtracker.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobApplication>> create(
            @Valid @RequestBody CreateApplicationRequest request) {
        JobApplication created = applicationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application added successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobApplication>>> getAll(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company) {

        List<JobApplication> result;
        if (status != null) {
            result = applicationService.getByStatus(status);
        } else if (company != null) {
            result = applicationService.searchByCompany(company);
        } else {
            result = applicationService.getAll();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobApplication>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getById(id)));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<StatusHistory>>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getHistory(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobApplication>> update(
            @PathVariable UUID id,
            @RequestBody UpdateApplicationRequest request) {
        JobApplication updated = applicationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Application updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        applicationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Application deleted successfully", null));
    }
}