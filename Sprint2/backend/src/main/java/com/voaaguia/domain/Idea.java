package com.voaaguia.domain;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Document(collection = "ideas")
public class Idea {
    @Id public String id;
    @Version public Long version;
    public java.time.Instant createdAt = java.time.Instant.now();
    public java.time.Instant updatedAt = java.time.Instant.now();
    public String title = "";
    public String description = "";
    public String category = "";
    public String strategyId = "";
    public String authorId = "";
    public String authorName = "";
    public String department = "";
    public String status = "PENDENTE";
    public String priority = "MEDIA";
    public String feedback = "";
    @JsonIgnore
    public java.util.Set<String> voters = new java.util.HashSet<>();
    @JsonIgnore
    public boolean everApproved = false;
    @JsonIgnore
    public boolean deleted = false;
    public int getVotes() { return voters.size(); }
}
