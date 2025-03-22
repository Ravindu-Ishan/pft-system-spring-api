package org.spring.pftsystem.controllers;

import lombok.extern.slf4j.Slf4j;
import org.spring.pftsystem.entity.response.BudgetNotification;
import org.spring.pftsystem.entity.response.GoalNotification;
import org.spring.pftsystem.entity.response.RecurringTransactionNotification;
import org.spring.pftsystem.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for notification endpoints
 */
@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationsController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all budget notifications for the current user
     * @return List of budget notifications
     */
    @GetMapping("/budgets")
    public ResponseEntity<List<BudgetNotification>> getBudgetNotifications() {
        String userId = getCurrentUserId();
        log.info("Fetching budget notifications for user: {}", userId);

        List<BudgetNotification> notifications = notificationService.getBudgetNotifications(userId);

        log.info("Returning {} budget notifications", notifications.size());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get all recurring transaction notifications for the current user
     * @return List of recurring transaction notifications
     */
    @GetMapping("/recurring-transactions")
    public ResponseEntity<List<RecurringTransactionNotification>> getRecurringTransactionNotifications() {
        String userId = getCurrentUserId();
        log.info("Fetching recurring transaction notifications for user: {}", userId);

        List<RecurringTransactionNotification> notifications = notificationService.getRecurringTransactionNotifications(userId);

        log.info("Returning {} recurring transaction notifications", notifications.size());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get all goal notifications for the current user
     * @return List of goal notifications
     */
    @GetMapping("/goals")
    public ResponseEntity<List<GoalNotification>> getGoalNotifications() {
        String userId = getCurrentUserId();
        log.info("Fetching goal notifications for user: {}", userId);

        List<GoalNotification> notifications = notificationService.getGoalNotifications(userId);

        log.info("Returning {} goal notifications", notifications.size());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get all notifications (budgets, recurring transactions, goals) for the current user
     * @return Map containing all notification types
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllNotifications() {
        String userId = getCurrentUserId();
        log.info("Fetching all notifications for user: {}", userId);

        List<BudgetNotification> budgetNotifications = notificationService.getBudgetNotifications(userId);
        List<RecurringTransactionNotification> recurringTransactionNotifications =
                notificationService.getRecurringTransactionNotifications(userId);
        List<GoalNotification> goalNotifications = notificationService.getGoalNotifications(userId);

        Map<String, Object> allNotifications = new HashMap<>();
        allNotifications.put("budgets", budgetNotifications);
        allNotifications.put("recurringTransactions", recurringTransactionNotifications);
        allNotifications.put("goals", goalNotifications);

        int totalCount = budgetNotifications.size() + recurringTransactionNotifications.size() + goalNotifications.size();
        allNotifications.put("totalCount", totalCount);

        log.info("Returning {} total notifications", totalCount);
        return ResponseEntity.ok(allNotifications);
    }

    /**
     * Get count of all notifications for the current user
     * @return Count of notifications by type
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getNotificationCount() {
        String userId = getCurrentUserId();
        log.info("Fetching notification count for user: {}", userId);

        List<BudgetNotification> budgetNotifications = notificationService.getBudgetNotifications(userId);
        List<RecurringTransactionNotification> recurringTransactionNotifications =
                notificationService.getRecurringTransactionNotifications(userId);
        List<GoalNotification> goalNotifications = notificationService.getGoalNotifications(userId);

        Map<String, Integer> counts = new HashMap<>();
        counts.put("budgets", budgetNotifications.size());
        counts.put("recurringTransactions", recurringTransactionNotifications.size());
        counts.put("goals", goalNotifications.size());
        counts.put("total", budgetNotifications.size() + recurringTransactionNotifications.size() + goalNotifications.size());

        log.info("Returning notification counts: {}", counts);
        return ResponseEntity.ok(counts);
    }

    /**
     * Helper method to get current user ID from security context
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // Assuming the name is the user ID
    }
}