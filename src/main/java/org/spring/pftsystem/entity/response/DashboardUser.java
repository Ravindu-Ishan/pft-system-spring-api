package org.spring.pftsystem.entity.response;

import lombok.Data;
import org.spring.pftsystem.entity.schema.main.Budget;
import org.spring.pftsystem.entity.schema.main.Goal;

import java.util.List;

@Data
public class DashboardUser {
    private String username;
    private TransactionsSummary transactionsSummary;
    private Budget budget;
    private long ongoingGoalsCount;
    private List<Goal> ongoingGoals;
}
