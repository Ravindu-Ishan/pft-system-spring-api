package org.spring.pftsystem.services;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service responsible for scheduled tasks that update system state
 */
@Service
@EnableScheduling
@Log
public class ScheduledUpdateService {

    @Autowired
    private TransactionsService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private GoalService goalService;

    /**
     * Daily job to process recurring transactions, update budgets, and collect goal contributions
     * Runs at midnight every day (0 0 0 * * ?)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void performDailyUpdates() {
        log.info("Starting daily scheduled updates");

        try {
            // Process recurring transactions
            transactionService.processRecurringTransactions();

            // Update budget statuses
            budgetService.updateAllBudgets();

            // Process goal auto-collections
            goalService.updateAllGoals();

            log.info("Daily scheduled updates completed successfully");
        } catch (Exception e) {
            log.severe("Error during daily scheduled updates: " + e.getMessage() + " " + e);
        }
    }

    /**
     * Hourly job to update budgets (more frequent updates for budgets)
     * Runs at minute 0 of every hour (0 0 * * * ?)
     *//*
    @Scheduled(cron = "0 0 * * * ?")
    public void performHourlyBudgetUpdates() {
        log.info("Starting hourly budget updates");

        try {
            budgetService.updateAllBudgets();
            log.info("Hourly budget updates completed successfully");
        } catch (Exception e) {
            log.severe("Error during hourly budget updates: {}" + e.getMessage() + " " +  e);
        }
    }*/

    /*@Scheduled(cron = "0 0 * * * ?")  // Run once a day
    public void updateNotificationStatus() {
        TransactionsRepo transactionRepository;
        List<Transaction> recurringTransactions = transactionRepository.findByIsRecurringTrue();
        LocalDate currentDate = LocalDate.now();

        for (Transaction transaction : recurringTransactions) {
            if (transaction.getRecurrence() != null) {
                LocalDate nextExecution = LocalDate.parse(
                        transaction.getRecurrence().getNextExecutionDate(),
                        DATE_FORMATTER
                );
                long daysRemaining = ChronoUnit.DAYS.between(currentDate, nextExecution);

                // Enable notifications if execution is within 3 days
                if (daysRemaining <= 3 && daysRemaining >= 0) {
                    transaction.setNotify(true);
                    transactionRepository.save(transaction);
                }
            }
        }
    }*/
}
