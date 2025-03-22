package org.spring.pftsystem.entity.schema.sub;

import lombok.Data;

@Data
public class Summary {
    private double totalIncome;
    private double totalExpense;
    private double netBalance;
    private double totalSavings;
    private double balanceAfterSavings;
    private double averageDailyExpense;
    private HighestExpense highestExpense;
    private HighestIncome highestIncome;

    @Data
    public static class HighestExpense {
        private double amount;
        private String category;
        private String date;
    }

    @Data
    public static class HighestIncome {
        private double amount;
        private String source;
        private String date;
    }
}
