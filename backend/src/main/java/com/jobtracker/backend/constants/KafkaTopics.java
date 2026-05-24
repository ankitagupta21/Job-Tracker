package com.jobtracker.backend.constants;

public class KafkaTopics {
    public static final String APPLICATION_CREATED = "application.created";
    public static final String APPLICATION_INTERVIEW = "application.interview";
    public static final String APPLICATION_REJECTED = "application.rejected";
    public static final String APPLICATION_OFFERED = "application.offered";
    public static final String APPLICATION_OA = "application.oa";

    private KafkaTopics() {
    }
}