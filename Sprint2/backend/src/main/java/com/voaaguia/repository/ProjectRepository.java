package com.voaaguia.repository;
import com.voaaguia.domain.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ProjectRepository extends MongoRepository<Project, String> {
    java.util.List<Project> findByDeletedFalseOrderByCreatedAtDesc();
    boolean existsByIdeaId(String ideaId);
}
