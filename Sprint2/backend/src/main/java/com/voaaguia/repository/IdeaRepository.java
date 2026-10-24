package com.voaaguia.repository;
import com.voaaguia.domain.Idea;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface IdeaRepository extends MongoRepository<Idea, String> { java.util.List<Idea> findByDeletedFalseOrderByCreatedAtDesc(); }
