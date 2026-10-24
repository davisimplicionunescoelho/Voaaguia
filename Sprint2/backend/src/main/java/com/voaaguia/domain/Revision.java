package com.voaaguia.domain;
import java.time.Instant;
public record Revision(String id, Instant date, String category, String campaign, String title, String description, boolean isActive, String changedBy) {}
