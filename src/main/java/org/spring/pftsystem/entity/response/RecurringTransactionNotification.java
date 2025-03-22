package org.spring.pftsystem.entity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionNotification {
    private String transactionId;
    private String userId;
    private String beneficiary;
    private Double amount;
    private String currency;
    private String type;
    private String nextExecutionDate;
    private int daysRemaining;
    private String message;
    private String timestamp;
}
