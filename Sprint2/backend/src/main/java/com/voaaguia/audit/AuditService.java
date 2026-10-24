package com.voaaguia.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) { this.events = events; }

    @Transactional
    public void record(String action, String userId) { events.save(new AuditEvent(action, userId)); }
}
