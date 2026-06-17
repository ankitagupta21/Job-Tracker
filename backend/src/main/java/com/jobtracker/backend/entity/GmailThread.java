package com.jobtracker.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gmail_threads")
@Getter
@Setter
@NoArgsConstructor
public class GmailThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @Column(nullable = false, unique = true)
    private String threadId;

    private String subject;

    private LocalDateTime receivedAt;
}