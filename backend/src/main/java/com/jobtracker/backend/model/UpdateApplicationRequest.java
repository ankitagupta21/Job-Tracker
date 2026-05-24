package com.jobtracker.backend.model;

import com.jobtracker.backend.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateApplicationRequest {

    private String companyName;
    private String role;
    private String jobUrl;
    private String notes;
    private ApplicationStatus status;
}