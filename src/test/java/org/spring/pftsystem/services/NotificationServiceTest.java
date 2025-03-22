package org.spring.pftsystem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.response.BudgetNotification;
import org.spring.pftsystem.entity.response.GoalNotification;
import org.spring.pftsystem.entity.response.RecurringTransactionNotification;
import org.spring.pftsystem.entity.schema.main.Budget;
import org.spring.pftsystem.entity.schema.main.Goal;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.sub.RecurrenceDetails;
import org.spring.pftsystem.repository.BudgetRepository;
import org.spring.pftsystem.repository.GoalRepository;
import org.spring.pftsystem.repository.TransactionsRepo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionsRepo transactionRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalService goalService;

    @InjectMocks
    private NotificationService notificationService;

    private final String TEST_USER_ID = "test-user-123";
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    public void testGetBudgetNotifications_WhenBudgetApproachingLimit() {
        // Arrange
        Budget approachingBudget = new Budget();
        approachingBudget.setId("budget-456");
        approachingBudget.setUserID(TEST_USER_ID);
        approachingBudget.setMonthlyLimit(1000.0f);
        approachingBudget.setCurrentExpenditure(800.0f);
        approachingBudget.setCurrency("EUR");
        approachingBudget.setWarning(true);

        when(budgetRepository.findByUserIDAndWarningTrue(TEST_USER_ID))
                .thenReturn(Collections.singletonList(approachingBudget));

        // Act
        List<BudgetNotification> notifications = notificationService.getBudgetNotifications(TEST_USER_ID);

        // Assert
        assertEquals(1, notifications.size());
        BudgetNotification notification = notifications.get(0);
        assertEquals("budget-456", notification.getBudgetId());
        assertEquals(1000.0f, notification.getMonthlyLimit());
        assertEquals(800.0f, notification.getCurrentExpenditure());
        assertEquals(200.0f, notification.getRemainingAmount());
        assertEquals(80.0f, notification.getPercentageUsed());
        assertEquals("EUR", notification.getCurrency());
        assertFalse(notification.isExceeded());
        assertTrue(notification.getMessage().contains("80.0%"));
    }

    @Test
    public void testGetBudgetNotifications_WhenNoBudgetsWithWarning() {
        // Arrange
        when(budgetRepository.findByUserIDAndWarningTrue(TEST_USER_ID))
                .thenReturn(Collections.emptyList());

        // Act
        List<BudgetNotification> notifications = notificationService.getBudgetNotifications(TEST_USER_ID);

        // Assert
        assertEquals(0, notifications.size());
    }

    @Test
    public void testGetRecurringTransactionNotifications_WhenTransactionsExistWithinNotificationWindow() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Transaction scheduled for today
        Transaction todayTransaction = createRecurringTransaction(
                "tx-today", TEST_USER_ID, "Rent", 1500.0, "USD", "PAYMENT",
                today.format(DATE_FORMATTER)
        );

        // Transaction scheduled for tomorrow
        Transaction tomorrowTransaction = createRecurringTransaction(
                "tx-tomorrow", TEST_USER_ID, "Netflix", 15.99, "USD", "SUBSCRIPTION",
                today.plusDays(1).format(DATE_FORMATTER)
        );

        // Transaction scheduled in 3 days
        Transaction threeDaysTransaction = createRecurringTransaction(
                "tx-3days", TEST_USER_ID, "Gym", 50.0, "USD", "SUBSCRIPTION",
                today.plusDays(3).format(DATE_FORMATTER)
        );

        // Transaction scheduled in 5 days (outside notification window)
        Transaction fiveDaysTransaction = createRecurringTransaction(
                "tx-5days", TEST_USER_ID, "Insurance", 100.0, "USD", "PAYMENT",
                today.plusDays(5).format(DATE_FORMATTER)
        );

        when(transactionRepository.findByUserIdAndIsRecurringTrue(TEST_USER_ID))
                .thenReturn(Arrays.asList(
                        todayTransaction,
                        tomorrowTransaction,
                        threeDaysTransaction,
                        fiveDaysTransaction
                ));

        // Act
        List<RecurringTransactionNotification> notifications =
                notificationService.getRecurringTransactionNotifications(TEST_USER_ID);

        // Assert
        assertEquals(3, notifications.size());

        // Verify today's transaction notification
        RecurringTransactionNotification todayNotification = findNotificationById(notifications, "tx-today");
        assertNotNull(todayNotification);
        assertEquals(0, todayNotification.getDaysRemaining());
        assertTrue(todayNotification.getMessage().contains("today"));

        // Verify tomorrow's transaction notification
        RecurringTransactionNotification tomorrowNotification = findNotificationById(notifications, "tx-tomorrow");
        assertNotNull(tomorrowNotification);
        assertEquals(1, tomorrowNotification.getDaysRemaining());
        assertTrue(tomorrowNotification.getMessage().contains("tomorrow"));

        // Verify 3-day transaction notification
        RecurringTransactionNotification threeDayNotification = findNotificationById(notifications, "tx-3days");
        assertNotNull(threeDayNotification);
        assertEquals(3, threeDayNotification.getDaysRemaining());
        assertTrue(threeDayNotification.getMessage().contains("in 3 days"));

        // The 5-day transaction should not be included
        assertNull(findNotificationById(notifications, "tx-5days"));
    }

    @Test
    public void testGetRecurringTransactionNotifications_WhenNoTransactionsInNotificationWindow() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Transaction scheduled in 5 days (outside notification window)
        Transaction fiveDaysTransaction = createRecurringTransaction(
                "tx-5days", TEST_USER_ID, "Insurance", 100.0, "USD", "PAYMENT",
                today.plusDays(5).format(DATE_FORMATTER)
        );

        when(transactionRepository.findByUserIdAndIsRecurringTrue(TEST_USER_ID))
                .thenReturn(Collections.singletonList(fiveDaysTransaction));

        // Act
        List<RecurringTransactionNotification> notifications =
                notificationService.getRecurringTransactionNotifications(TEST_USER_ID);

        // Assert
        assertEquals(0, notifications.size());
    }

    @Test
    public void testGetGoalNotifications_WhenUpcomingAutoCollection() {
        // Arrange
        LocalDate today = LocalDate.now();
        int collectionDay = getDayForNextThreeDays(today);

        Goal goal = new Goal();
        goal.setId("goal-123");
        goal.setUserID(TEST_USER_ID);
        goal.setGoalName("Vacation");
        goal.setAmountRequired(5000.0);
        goal.setMonthlyCommitment(500.0);
        goal.setCollectionDayOfMonth(collectionDay);
        goal.setEnableAutoCollect(true);
        goal.setNotify(true);

        when(goalRepository.findByUserIDAndNotifyTrue(TEST_USER_ID))
                .thenReturn(Collections.singletonList(goal));
        when(goalService.getCurrentAmountForGoal("goal-123"))
                .thenReturn(2500.0); // 50% complete

        // Act
        List<GoalNotification> notifications = notificationService.getGoalNotifications(TEST_USER_ID);

        // Assert
        assertEquals(1, notifications.size());
        GoalNotification notification = notifications.get(0);
        assertEquals("goal-123", notification.getGoalId());
        assertEquals(TEST_USER_ID, notification.getUserID());
        assertEquals("Vacation", notification.getGoalName());
        assertEquals(5000.0, notification.getAmountRequired());
        assertEquals(2500.0, notification.getCurrentAmount());
        assertEquals(2500.0, notification.getRemainingAmount());
        assertEquals(50.0, notification.getPercentageComplete());

        // Should mention auto-collection
        assertTrue(notification.getMessage().contains("Auto-collection"));
        assertTrue(notification.getMessage().contains("500.0"));
    }

    @Test
    public void testGetGoalNotifications_WhenNearingCompletion() {
        // Arrange
        LocalDate today = LocalDate.now();

        Goal goal = new Goal();
        goal.setId("goal-456");
        goal.setUserID(TEST_USER_ID);
        goal.setGoalName("New Laptop");
        goal.setAmountRequired(1000.0);
        goal.setMonthlyCommitment(100.0);
        goal.setCollectionDayOfMonth(28); // Far from today
        goal.setEnableAutoCollect(true);
        goal.setNotify(true);

        when(goalRepository.findByUserIDAndNotifyTrue(TEST_USER_ID))
                .thenReturn(Collections.singletonList(goal));
        when(goalService.getCurrentAmountForGoal("goal-456"))
                .thenReturn(950.0); // 95% complete

        // Act
        List<GoalNotification> notifications = notificationService.getGoalNotifications(TEST_USER_ID);

        // Assert
        assertEquals(1, notifications.size());
        GoalNotification notification = notifications.get(0);
        assertEquals("goal-456", notification.getGoalId());
        assertEquals("New Laptop", notification.getGoalName());
        assertEquals(1000.0, notification.getAmountRequired());
        assertEquals(950.0, notification.getCurrentAmount());
        assertEquals(50.0, notification.getRemainingAmount());
        assertEquals(95.0, notification.getPercentageComplete());

        // Should mention percentage complete
        assertTrue(notification.getMessage().contains("95.0%"));
        assertTrue(notification.getMessage().contains("50.0"));
    }

    @Test
    public void testGetGoalNotifications_WhenNoNotificationNeeded() {
        // Arrange
        LocalDate today = LocalDate.now();

        Goal goal = new Goal();
        goal.setId("goal-789");
        goal.setUserID(TEST_USER_ID);
        goal.setGoalName("Emergency Fund");
        goal.setAmountRequired(10000.0);
        goal.setMonthlyCommitment(200.0);
        goal.setCollectionDayOfMonth(28); // Far from today
        goal.setEnableAutoCollect(true);
        goal.setNotify(true);

        when(goalRepository.findByUserIDAndNotifyTrue(TEST_USER_ID))
                .thenReturn(Collections.singletonList(goal));
        when(goalService.getCurrentAmountForGoal("goal-789"))
                .thenReturn(5000.0); // 50% complete, not near completion

        // Act
        List<GoalNotification> notifications = notificationService.getGoalNotifications(TEST_USER_ID);

        // Assert
        assertEquals(0, notifications.size());
    }

    // Helper methods
    private Transaction createRecurringTransaction(String id, String userId, String beneficiary,
                                                   Double amount, String currency, String type,
                                                   String nextExecutionDate) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setUserId(userId);
        transaction.setBeneficiary(beneficiary);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setType(type);
        transaction.setIsRecurring(true);
        transaction.setNotify(true);

        // Create RecurrenceDetails with all required fields
        RecurrenceDetails recurrence = new RecurrenceDetails(
                "Monthly",                         // pattern
                LocalDate.now().format(DATE_FORMATTER), // startDate
                LocalDate.now().plusYears(1).format(DATE_FORMATTER), // endDate
                15,                                // executeOnDay
                nextExecutionDate                  // nextExecutionDate
        );

        transaction.setRecurrence(recurrence);

        return transaction;
    }

    private RecurringTransactionNotification findNotificationById(
            List<RecurringTransactionNotification> notifications, String id) {
        return notifications.stream()
                .filter(n -> n.getTransactionId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private int getDayForNextThreeDays(LocalDate today) {
        // Return a day of month that's within the next 3 days
        return today.getDayOfMonth() + 2;
    }
}