package com.voaaguia.repository;
import com.voaaguia.domain.Guideline;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface GuidelineRepository extends MongoRepository<Guideline, String> { java.util.List<Guideline> findByIsActiveTrueOrderByCreatedAtDesc(); }
