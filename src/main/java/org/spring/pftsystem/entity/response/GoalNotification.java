package org.spring.pftsystem.entity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalNotification {
    private String goalId;
    private String userID;
    private String goalName;
    private double amountRequired;
    private double currentAmount;
    private double remainingAmount;
    private double percentageComplete;
    private double monthlyCommitment;
    private int collectionDayOfMonth;
    private int daysUntilNextCollection;
    private String message;
    private String timestamp;
}
