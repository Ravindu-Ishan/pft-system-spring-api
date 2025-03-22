package org.spring.pftsystem.repository.customImp;

import org.spring.pftsystem.entity.schema.main.Transaction;

import java.util.Date;
import java.util.List;

public interface TransactionRepositoryCustom {
    List<Transaction> findFilteredTransactions(String userId, String startDate, String endDate, List<String> categories, List<String> tags, List<String> types);
}

