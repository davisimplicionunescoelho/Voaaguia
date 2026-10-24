package com.voaaguia.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Instant createdAt = Instant.now();
    public String action = "";
    public String userId = "";

    public AuditEvent() { }
    public AuditEvent(String action, String userId) { this.action = action; this.userId = userId; }
}
