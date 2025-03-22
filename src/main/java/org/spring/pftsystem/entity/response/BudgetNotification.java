package org.spring.pftsystem.entity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetNotification {
    private String budgetId;
    private String userID;
    private float monthlyLimit;
    private float currentExpenditure;
    private float remainingAmount;
    private float percentageUsed;
    private String currency;
    private String message;
    private boolean isExceeded;
    private String timestamp;
}
