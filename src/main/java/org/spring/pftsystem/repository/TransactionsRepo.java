package org.spring.pftsystem.repository;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.repository.customImp.TransactionRepositoryCustom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface TransactionsRepo extends MongoRepository<Transaction, String> {
    List<Transaction> findAllByUserId(String id);
    long countByUserId(String id);
    List<Transaction> findByUserIdAndIsRecurringTrue(String userId);
    List<Transaction> findByIsRecurringTrue();

    // Find transactions by user and date range
    @Query("{'userId': ?0, 'transactionDate': {$gte: ?1, $lte: ?2}}")
    List<Transaction> findByUserIdAndTransactionDateBetween(String userId, String startDate, String endDate);

    // Find transactions by category for a user
    List<Transaction> findByUserIdAndCategory(String userId, String category);

    @Query(value = "{ 'transactionDate': { $gte: ?0, $lt: ?1 } }", count = true)
    long countTransactionsByDateBetween(String startDate, String endDate);

    @Query(value = "{ 'userId': ?0, 'transactionDate': { $gte: ?1, $lt: ?2 } }", count = true)
    long countTransactionsByUserIdAndTransactionDateBetween(String userId, String startDate, String endDate);

    @Query("{ 'userId': ?0, 'type': 'Savings', 'transactionDate': { $gte: ?1, $lt: ?2 } }")
    List<Transaction> findSavingsTransactionsByUserIdAndTransactionDateBetween(String userId, String startDate, String endDate);

    @Query("{ 'userId': ?0, 'type': 'Expense', 'transactionDate': { $gte: ?1, $lt: ?2 } }")
    List<Transaction> findExpenseTransactionsByUserIdAndTransactionDateBetween(String userId, String startDate, String endDate);

    @Query("{ 'userId': ?0, 'type': 'Income', 'transactionDate': { $gte: ?1, $lt: ?2 } }")
    List<Transaction> findIncomeTransactionsByUserIdAndTransactionDateBetween(String userId, String startDate, String endDate);
}

