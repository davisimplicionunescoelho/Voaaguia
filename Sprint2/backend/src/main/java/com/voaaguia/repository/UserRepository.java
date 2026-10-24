package com.voaaguia.repository;
import com.voaaguia.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface UserRepository extends MongoRepository<User, String> { java.util.Optional<User> findByEmail(String email); }
