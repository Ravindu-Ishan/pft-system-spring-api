package org.spring.pftsystem.entity.response;

import lombok.Data;

@Data
public class TransactionsSummary {
    private long totalTransactionsToDate;
    private long totalTransactionsThisMonth;
    private Double totalSavingsThisMonth;
    private Double totalExpensesThisMonth;
    private Double totalIncomeThisMonth;
}