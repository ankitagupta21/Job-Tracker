package com.jobtracker.backend.repository;

import com.jobtracker.backend.entity.GmailThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GmailThreadRepository extends JpaRepository<GmailThread, UUID> {
    boolean existsByThreadId(String threadId);
}