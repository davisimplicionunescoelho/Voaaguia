package com.voaaguia.domain;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Document(collection = "projects")
public class Project {
    @Id public String id;
    @Version public Long version;
    public java.time.Instant createdAt = java.time.Instant.now();
    public java.time.Instant updatedAt = java.time.Instant.now();
    public String title = "";
    public String description = "";
    public String ideaId = "";
    public String strategyId = "";
    public String managerId = "";
    public String managerName = "";
    public String status = "PLANEJAMENTO";
    public String stage = "IDEACAO";
    public java.time.Instant startDate = null;
    public java.time.Instant expectedEndDate = null;
    public java.time.Instant actualEndDate = null;
    public java.math.BigDecimal investment = java.math.BigDecimal.ZERO;
    public java.math.BigDecimal expectedReturn = java.math.BigDecimal.ZERO;
    public java.math.BigDecimal actualReturn = java.math.BigDecimal.ZERO;
    public java.math.BigDecimal costReduction = java.math.BigDecimal.ZERO;
    public double productivityGain = 0;
    public int progressPercent = 0;
    public String notes = "";
    @JsonIgnore
    public boolean deleted = false;
}
