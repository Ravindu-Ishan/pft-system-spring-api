package org.spring.pftsystem.repository.customImp;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Log
@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public TransactionRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Transaction> findFilteredTransactions(String userId, String startDate, String endDate, List<String> categories, List<String> tags, List<String> types) {
        Query query = new Query();

        query.addCriteria(Criteria.where("userId").is(userId));

        // Date filter using string comparisons
        query.addCriteria(Criteria.where("transactionDate").gte(startDate).lte(endDate));

        // Type filter
        if (types != null && !types.isEmpty()) {
            query.addCriteria(Criteria.where("type").in(types));
        }

        // Category filter
        if (categories != null && !categories.isEmpty()) {
            query.addCriteria(Criteria.where("category").in(categories));
        }

        // Tags filter
        if (tags != null && !tags.isEmpty()) {
            query.addCriteria(Criteria.where("tags").in(tags));
        }

        return mongoTemplate.find(query, Transaction.class);
    }
}