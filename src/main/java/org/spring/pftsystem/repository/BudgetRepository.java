package org.spring.pftsystem.repository;

import org.spring.pftsystem.entity.schema.main.Budget;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends MongoRepository<Budget, String> {
    Optional<Budget> findByUserID(String id);
    List<Budget> findByUserIDAndWarningTrue(String userId);
}