package com.voaaguia.domain;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Document(collection = "guidelines")
public class Guideline {
    @Id public String id;
    @Version public Long version;
    public java.time.Instant createdAt = java.time.Instant.now();
    public java.time.Instant updatedAt = java.time.Instant.now();
    public String title = "";
    public String description = "";
    public String category = "";
    public String campaign = "";
    public String authorId = "";
    public String authorName = "";
    public boolean isActive = true;
    @JsonIgnore
    public java.util.List<Revision> history = new java.util.ArrayList<>();
}
