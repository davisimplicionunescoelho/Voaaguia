package com.voaaguia.security;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
import java.time.Duration;
@Configuration
public class DatabaseIndexes {
    @Bean CommandLineRunner indexes(MongoTemplate mongo) { return args -> {
        mongo.indexOps("projects").ensureIndex(new Index().on("ideaId",Sort.Direction.ASC).unique());
        mongo.indexOps("revoked_tokens").ensureIndex(new Index().on("expiresAt",Sort.Direction.ASC).expire(Duration.ZERO));
    }; }
}
