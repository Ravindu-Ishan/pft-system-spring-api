package org.spring.pftsystem.services;


import org.spring.pftsystem.entity.response.BudgetNotification;
import org.spring.pftsystem.entity.response.GoalNotification;
import org.spring.pftsystem.entity.response.RecurringTransactionNotification;
import org.spring.pftsystem.entity.schema.main.Budget;
import org.spring.pftsystem.entity.schema.main.Goal;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.repository.BudgetRepository;
import org.spring.pftsystem.repository.GoalRepository;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionsRepo transactionRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalService goalService;

    /**
     * Get all budget notifications for a user
     */
    public List<BudgetNotification> getBudgetNotifications(String userId) {
        // Get all budgets with warning flag set
        List<Budget> budgets = budgetRepository.findByUserIDAndWarningTrue(userId);
        List<BudgetNotification> notifications = new ArrayList<>();

        for (Budget budget : budgets) {
            // Calculate percentage used
            float percentageUsed = budget.getMonthlyLimit() > 0
                    ? (budget.getCurrentExpenditure() / budget.getMonthlyLimit()) * 100
                    : 0;

            boolean exceededBudget = budget.getCurrentExpenditure() >= budget.getMonthlyLimit();

            String message = exceededBudget
                    ? "You have exceeded your monthly budget limit by " +
                    String.format("%.2f", budget.getCurrentExpenditure() - budget.getMonthlyLimit()) + " " + budget.getCurrency() + "!"
                    : "You have used " + String.format("%.1f", percentageUsed) + "% of your monthly budget.";

            notifications.add(BudgetNotification.builder()
                    .budgetId(budget.getId())
                    .userID(budget.getUserID())
                    .monthlyLimit(budget.getMonthlyLimit())
                    .currentExpenditure(budget.getCurrentExpenditure())
                    .remainingAmount(budget.getMonthlyLimit() - budget.getCurrentExpenditure())
                    .percentageUsed(percentageUsed)
                    .currency(budget.getCurrency())
                    .message(message)
                    .isExceeded(exceededBudget)
                    .timestamp(LocalDateTime.now().toString())
                    .build());
        }

        return notifications;
    }

    /**
     * Get all recurring transaction notifications for a user
     */
    public List<RecurringTransactionNotification> getRecurringTransactionNotifications(String userId) {
        // Find all recurring transactions for the user
        List<Transaction> recurringTransactions = transactionRepository.findByUserIdAndIsRecurringTrue(userId);
        List<RecurringTransactionNotification> notifications = new ArrayList<>();

        LocalDate currentDate = LocalDate.now();

        for (Transaction transaction : recurringTransactions) {
            // Only include transactions with notify flag set to true
            if (transaction.isNotify() && transaction.getRecurrence() != null) {
                LocalDate nextExecution = LocalDate.parse(transaction.getRecurrence().getNextExecutionDate(), DATE_FORMATTER);
                long daysRemaining = ChronoUnit.DAYS.between(currentDate, nextExecution);

                // Notify if execution is within 3 days
                if (daysRemaining <= 3 && daysRemaining >= 0) {
                    String dayText = daysRemaining == 0 ? "today" :
                            daysRemaining == 1 ? "tomorrow" :
                                    "in " + daysRemaining + " days";

                    String message = String.format("Recurring %s of %s %s to %s is scheduled %s.",
                            transaction.getType().toLowerCase(),
                            transaction.getAmount(),
                            transaction.getCurrency(),
                            transaction.getBeneficiary(),
                            dayText);

                    notifications.add(RecurringTransactionNotification.builder()
                            .transactionId(transaction.getId())
                            .userId(transaction.getUserId())
                            .beneficiary(transaction.getBeneficiary())
                            .amount(transaction.getAmount())
                            .currency(transaction.getCurrency())
                            .type(transaction.getType())
                            .nextExecutionDate(transaction.getRecurrence().getNextExecutionDate())
                            .daysRemaining((int) daysRemaining)
                            .message(message)
                            .timestamp(LocalDateTime.now().toString())
                            .build());
                }
            }
        }

        return notifications;
    }

    /**
     * Get all goal notifications for a user
     */
    public List<GoalNotification> getGoalNotifications(String userId) {
        List<Goal> goals = goalRepository.findByUserIDAndNotifyTrue(userId);
        List<GoalNotification> notifications = new ArrayList<>();

        LocalDate currentDate = LocalDate.now();

        for (Goal goal : goals) {
            // Get current saved amount
            double currentAmount = goalService.getCurrentAmountForGoal(goal.getId());
            double remainingAmount = goal.getAmountRequired() - currentAmount;
            double percentageComplete = (currentAmount / goal.getAmountRequired()) * 100;

            if (goal.isEnableAutoCollect()) {
                // Calculate days until next collection
                LocalDate nextCollectionDate = getNextCollectionDate(currentDate, goal.getCollectionDayOfMonth());
                int daysUntilCollection = (int) ChronoUnit.DAYS.between(currentDate, nextCollectionDate);

                // Notify if collection is within 3 days or if amount is close to target
                boolean nearingCompletion = percentageComplete >= 90;
                boolean upcomingCollection = daysUntilCollection <= 3;

                if (upcomingCollection || nearingCompletion) {
                    String message;
                    if (upcomingCollection) {
                        String dayText = daysUntilCollection == 0 ? "today" :
                                daysUntilCollection == 1 ? "tomorrow" :
                                        "in " + daysUntilCollection + " days";
                        message = String.format("Auto-collection of %.2f for your '%s' goal is scheduled %s.",
                                goal.getMonthlyCommitment(), goal.getGoalName(), dayText);
                    } else {
                        // Close to target
                        message = String.format("You've reached %.1f%% of your '%s' goal! Only %.2f more to go!",
                                percentageComplete, goal.getGoalName(), remainingAmount);
                    }

                    notifications.add(GoalNotification.builder()
                            .goalId(goal.getId())
                            .userID(goal.getUserID())
                            .goalName(goal.getGoalName())
                            .amountRequired(goal.getAmountRequired())
                            .currentAmount(currentAmount)
                            .remainingAmount(remainingAmount)
                            .percentageComplete(percentageComplete)
                            .monthlyCommitment(goal.getMonthlyCommitment())
                            .collectionDayOfMonth(goal.getCollectionDayOfMonth())
                            .daysUntilNextCollection(daysUntilCollection)
                            .message(message)
                            .timestamp(LocalDateTime.now().toString())
                            .build());
                }
            }
        }

        return notifications;
    }

    /**
     * Helper method to get the next collection date
     */
    private LocalDate getNextCollectionDate(LocalDate currentDate, int dayOfMonth) {
        LocalDate candidateDate = currentDate.withDayOfMonth(Math.min(dayOfMonth, currentDate.lengthOfMonth()));
        if (candidateDate.isBefore(currentDate) || candidateDate.isEqual(currentDate)) {
            // Move to next month if the collection day has already passed
            candidateDate = currentDate.plusMonths(1).withDayOfMonth(
                    Math.min(dayOfMonth, currentDate.plusMonths(1).lengthOfMonth()));
        }
        return candidateDate;
    }
}