package org.spring.pftsystem.repository;

import org.spring.pftsystem.entity.schema.main.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends MongoRepository<Goal, String> {
    List<Goal> findByUserID(String userID);
    long countByUserID(String id);
    List<Goal> findByUserIDAndNotifyTrue(String userId);
    List<Goal> findByEnableAutoCollectTrue();
}