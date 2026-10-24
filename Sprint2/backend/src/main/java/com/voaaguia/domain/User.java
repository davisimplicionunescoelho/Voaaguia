package com.voaaguia.domain;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Document(collection = "users")
public class User {
    @org.springframework.data.annotation.Transient public long points;
    @org.springframework.data.annotation.Transient public long ideasCount;
    @org.springframework.data.annotation.Transient public long approvedIdeasCount;
    @Id public String id;
    @Version public Long version;
    public java.time.Instant createdAt = java.time.Instant.now();
    public java.time.Instant updatedAt = java.time.Instant.now();
    public String name = "";
    @Indexed(unique = true)
    public String email = "";
    @JsonIgnore
    public String passwordHash = "";
    public String role = "OPERADOR";
    public String department = "";
}
