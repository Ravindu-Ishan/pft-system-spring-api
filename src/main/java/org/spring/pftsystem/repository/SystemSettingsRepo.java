package org.spring.pftsystem.repository;

import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingsRepo extends MongoRepository <SystemSettings, Long> {
    SystemSettings findFirstByOrderByIdAsc();
}
