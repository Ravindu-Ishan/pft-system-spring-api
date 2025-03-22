package org.spring.pftsystem.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.entity.schema.sub.RecurrenceDetails;
import org.spring.pftsystem.entity.schema.sub.UserSettings;
import org.spring.pftsystem.exception.AppIllegalArgument;
import org.spring.pftsystem.exception.NotFoundException;
import org.spring.pftsystem.repository.SystemSettingsRepo;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionsServiceTest {

    @Mock
    private TransactionsRepo transactionsRepo;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SystemSettingsRepo systemSettingsRepo;

    @InjectMocks
    private TransactionsService transactionsService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private Transaction transaction;
    private User user;
    private RecurrenceDetails recurrenceDetails;
    private SystemSettings systemSettings;
    private MockedStatic<UserUtil> userUtilMockedStatic;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @BeforeEach
    void setUp() {
        // Setup mock user
        user = new User();
        user.setId("user123");

        UserSettings userSettings = new UserSettings();
        userSettings.setCurrency("USD");
        user.setSettings(userSettings);

        // Setup transaction
        transaction = new Transaction();
        transaction.setId("trans123");
        transaction.setUserId("user123");
        transaction.setType("Expense");
        transaction.setCategory("Groceries");
        transaction.setTags(List.of("Food", "Essentials"));
        transaction.setBeneficiary("Supermarket");
        transaction.setSenderDescription("Weekly shopping");
        transaction.setAmount(100.0);
        transaction.setCurrency("USD");
        transaction.setIsRecurring(false);
        transaction.setTransactionDate(LocalDateTime.now().toString());
        transaction.setLastUpdatedAt(LocalDateTime.now().toString());

        // Setup recurrence details
        LocalDate today = LocalDate.now();
        recurrenceDetails = new RecurrenceDetails(
                "Monthly",
                today.minusMonths(1).format(DATE_FORMATTER),
                today.plusMonths(6).format(DATE_FORMATTER),
                15,
                today.format(DATE_FORMATTER)
        );

        // Setup system settings
        systemSettings = new SystemSettings(
                "settings123",
                100, // TotalTransactionsLimit
                20,  // RecurringTransactionsLimit
                List.of("Groceries", "Utilities", "Entertainment"),
                3600 // JWTExpirationTime
        );

        // Mock static method in UserUtil
        userUtilMockedStatic = Mockito.mockStatic(UserUtil.class);
        userUtilMockedStatic.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        if (userUtilMockedStatic != null) {
            userUtilMockedStatic.close();
        }
    }

    @Test
    void testCreateTransaction_Success() {
        // Arrange
        Transaction newTransaction = new Transaction();
        newTransaction.setType("Expense");
        newTransaction.setCategory("Groceries");
        newTransaction.setTags(List.of("Food"));
        newTransaction.setBeneficiary("Store");
        newTransaction.setSenderDescription("Food purchase");
        newTransaction.setAmount(50.0);
        newTransaction.setCurrency(""); // Empty currency to test default currency assignment

        // Mock transaction count
        when(transactionsRepo.countByUserId("user123")).thenReturn(50L); // Below the limit

        // Mock system settings
        when(systemSettingsRepo.findFirstByOrderByIdAsc()).thenReturn(systemSettings);

        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId("newTrans123");
            return saved;
        });

        // Mock the budgetService.updateBudgetForUser method
        BudgetService budgetService = mock(BudgetService.class);
        ReflectionTestUtils.setField(transactionsService, "budgetService", budgetService);
        doNothing().when(budgetService).updateBudgetForUser(anyString());

        // Act
        Transaction result = transactionsService.createTransaction(newTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals("USD", result.getCurrency()); // Should use default from user settings
        verify(transactionsRepo, times(1)).countByUserId("user123");
        verify(systemSettingsRepo, times(1)).findFirstByOrderByIdAsc();
        verify(transactionsRepo, times(1)).save(any(Transaction.class));
        // Verify budget was updated since this is an expense transaction
        verify(budgetService, times(1)).updateBudgetForUser("user123");
    }

    @Test
    void testCreateTransaction_ExceedsLimit() {
        // Arrange
        Transaction newTransaction = new Transaction();
        newTransaction.setType("Expense");
        newTransaction.setCategory("Groceries");
        newTransaction.setTags(List.of("Food"));
        newTransaction.setAmount(50.0);
        newTransaction.setCurrency("");

        // Mock transaction count - at the limit
        when(transactionsRepo.countByUserId("user123")).thenReturn(100L);

        // Mock system settings
        when(systemSettingsRepo.findFirstByOrderByIdAsc()).thenReturn(systemSettings);

        // Act & Assert
        assertThrows(AppIllegalArgument.class, () ->
                transactionsService.createTransaction(newTransaction)
        );
        verify(transactionsRepo, times(1)).countByUserId("user123");
        verify(systemSettingsRepo, times(1)).findFirstByOrderByIdAsc();
        verify(transactionsRepo, never()).save(any(Transaction.class));
    }

    @Test
    void testGetTransactionById_Success() {
        // Arrange
        when(transactionsRepo.findById("trans123")).thenReturn(Optional.of(transaction));

        // Act
        Transaction result = transactionsService.getTransactionById("trans123");

        // Assert
        assertNotNull(result);
        assertEquals("trans123", result.getId());
        verify(transactionsRepo, times(1)).findById("trans123");
    }

    @Test
    void testGetTransactionById_NotFound() {
        // Arrange
        when(transactionsRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> transactionsService.getTransactionById("nonexistent"));
        verify(transactionsRepo, times(1)).findById("nonexistent");
    }

    @Test
    void testGetAllTransactions_Success() {
        // Arrange
        List<Transaction> transactions = List.of(transaction);
        when(transactionsRepo.findAll()).thenReturn(transactions);

        // Act
        List<Transaction> result = transactionsService.getAllTransactions();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionsRepo, times(1)).findAll();
    }

    @Test
    void testGetAllTransactions_NotFound() {
        // Arrange
        when(transactionsRepo.findAll()).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> transactionsService.getAllTransactions());
        verify(transactionsRepo, times(1)).findAll();
    }

    @Test
    void testGetAllTransactionsOfUser_Success() {
        // Arrange
        List<Transaction> transactions = List.of(transaction);
        when(transactionsRepo.findAllByUserId("user123")).thenReturn(transactions);

        // Act
        List<Transaction> result = transactionsService.getAllTransactionsOfUser();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionsRepo, times(1)).findAllByUserId("user123");
    }

    @Test
    void testGetAllTransactionsOfUser_NotFound() {
        // Arrange
        when(transactionsRepo.findAllByUserId("user123")).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> transactionsService.getAllTransactionsOfUser());
        verify(transactionsRepo, times(1)).findAllByUserId("user123");
    }

    @Test
    void testGetTransactionByUserId_Success() {
        // Arrange
        List<Transaction> transactions = List.of(transaction);
        when(transactionsRepo.findAllByUserId("user123")).thenReturn(transactions);

        // Act
        List<Transaction> result = transactionsService.getTransactionByUserId("user123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(transactionsRepo, times(1)).findAllByUserId("user123");
    }

    @Test
    void testGetTransactionByUserId_NotFound() {
        // Arrange
        when(transactionsRepo.findAllByUserId("user123")).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> transactionsService.getTransactionByUserId("user123"));
        verify(transactionsRepo, times(1)).findAllByUserId("user123");
    }

    @Test
    void testUpdateTransaction_Success() {
        // Arrange
        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setType("Income");
        updatedTransaction.setCategory("Salary");
        updatedTransaction.setTags(List.of("Monthly", "Work"));
        updatedTransaction.setBeneficiary("Employer");
        updatedTransaction.setSenderDescription("Monthly salary");
        updatedTransaction.setAmount(3000.0);
        updatedTransaction.setCurrency("EUR");
        updatedTransaction.setIsRecurring(false);

        when(transactionsRepo.findById("trans123")).thenReturn(Optional.of(transaction));
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = transactionsService.updateTransaction("trans123", updatedTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("Income", result.getType());
        assertEquals("Salary", result.getCategory());
        assertEquals("EUR", result.getCurrency());
        assertEquals(3000.0, result.getAmount());
        verify(transactionsRepo, times(1)).findById("trans123");
        verify(transactionsRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUpdateTransaction_EmptyCurrency() {
        // Arrange
        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setType("Income");
        updatedTransaction.setCategory("Salary");
        updatedTransaction.setTags(List.of("Monthly", "Work"));
        updatedTransaction.setBeneficiary("Employer");
        updatedTransaction.setSenderDescription("Monthly salary");
        updatedTransaction.setAmount(3000.0);
        updatedTransaction.setCurrency(""); // Empty currency to test default currency assignment
        updatedTransaction.setIsRecurring(false);

        when(transactionsRepo.findById("trans123")).thenReturn(Optional.of(transaction));
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = transactionsService.updateTransaction("trans123", updatedTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("USD", result.getCurrency()); // Should use default from user settings
        verify(transactionsRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUpdateTransaction_WithRecurrenceDetails() {
        // Arrange
        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setType("Expense");
        updatedTransaction.setCategory("Rent");
        updatedTransaction.setTags(List.of("Housing", "Monthly"));
        updatedTransaction.setBeneficiary("Landlord");
        updatedTransaction.setSenderDescription("Monthly rent");
        updatedTransaction.setAmount(1500.0);
        updatedTransaction.setCurrency("USD");
        updatedTransaction.setIsRecurring(true);
        updatedTransaction.setRecurrence(recurrenceDetails);

        when(transactionsRepo.findById("trans123")).thenReturn(Optional.of(transaction));
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = transactionsService.updateTransaction("trans123", updatedTransaction);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsRecurring());
        assertNotNull(result.getRecurrence());
        assertEquals("Monthly", result.getRecurrence().getPattern());
        assertEquals(15, result.getRecurrence().getExecuteOnDay());
        verify(transactionsRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUpdateTransaction_MissingRecurrenceDetails() {
        // Arrange
        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setType("Expense");
        updatedTransaction.setCategory("Rent");
        updatedTransaction.setAmount(1500.0);
        updatedTransaction.setCurrency("USD");
        updatedTransaction.setIsRecurring(true);
        updatedTransaction.setRecurrence(null); // Missing recurrence details

        when(transactionsRepo.findById("trans123")).thenReturn(Optional.of(transaction));

        // Act & Assert
        assertThrows(AppIllegalArgument.class, () ->
                transactionsService.updateTransaction("trans123", updatedTransaction)
        );
        verify(transactionsRepo, never()).save(any(Transaction.class));
    }

    @Test
    void testUpdateTransaction_NotFound() {
        // Arrange
        when(transactionsRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
                transactionsService.updateTransaction("nonexistent", transaction));
        verify(transactionsRepo, never()).save(any(Transaction.class));
    }

    @Test
    void testDeleteTransaction_Success() {
        // Arrange
        doNothing().when(transactionsRepo).deleteById("trans123");

        // Act
        String result = transactionsService.deleteTransaction("trans123");

        // Assert
        assertEquals("Transaction deleted successfully", result);
        verify(transactionsRepo, times(1)).deleteById("trans123");
    }

    @Test
    void testDeleteTransaction_NotFound() {
        // Arrange
        doThrow(new RuntimeException()).when(transactionsRepo).deleteById("nonexistent");

        // Act & Assert
        assertThrows(NotFoundException.class, () -> transactionsService.deleteTransaction("nonexistent"));
        verify(transactionsRepo, times(1)).deleteById("nonexistent");
    }

    @Test
    void testProcessRecurringTransactions_NoTransactionsDue() {
        // Arrange
        Transaction recurringTransaction = new Transaction();
        recurringTransaction.setId("recur123");
        recurringTransaction.setUserId("user123");
        recurringTransaction.setIsRecurring(true);

        // Set next execution date to tomorrow (not due yet)
        RecurrenceDetails futureDetails = new RecurrenceDetails(
                "Monthly",
                LocalDate.now().minusMonths(1).format(DATE_FORMATTER),
                LocalDate.now().plusMonths(6).format(DATE_FORMATTER),
                15,
                LocalDate.now().plusDays(1).format(DATE_FORMATTER) // Future date
        );
        recurringTransaction.setRecurrence(futureDetails);

        List<Transaction> recurringTransactions = List.of(recurringTransaction);
        when(transactionsRepo.findByIsRecurringTrue()).thenReturn(recurringTransactions);

        // Act
        transactionsService.processRecurringTransactions();

        // Assert
        verify(transactionsRepo, never()).save(transactionCaptor.capture());
    }

    @Test
    void testProcessRecurringTransactions_TransactionDue() {
        // Arrange
        Transaction recurringTransaction = createRecurringTransaction();
        List<Transaction> recurringTransactions = List.of(recurringTransaction);

        when(transactionsRepo.findByIsRecurringTrue()).thenReturn(recurringTransactions);
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionsService.processRecurringTransactions();

        // Assert
        verify(transactionsRepo, times(2)).save(transactionCaptor.capture());

        List<Transaction> captured = transactionCaptor.getAllValues();

        // Check that we have two transactions
        assertEquals(2, captured.size());

        // Find the non-recurring transaction (the new one)
        Transaction newTransaction = captured.stream()
                .filter(t -> !t.getIsRecurring())
                .findFirst()
                .orElseThrow();

        // Find the recurring transaction (the updated one)
        Transaction updatedRecurring = captured.stream()
                .filter(Transaction::getIsRecurring)
                .findFirst()
                .orElseThrow();

        // Now verify the new transaction
        assertEquals("user123", newTransaction.getUserId());
        assertEquals("Expense", newTransaction.getType());
        assertEquals("Subscription", newTransaction.getCategory());
        assertEquals(15.0, newTransaction.getAmount());
        assertEquals("USD", newTransaction.getCurrency());
        assertFalse(newTransaction.getIsRecurring());
        assertNull(newTransaction.getRecurrence());

        // Verify the updated recurring transaction
        assertNotNull(updatedRecurring.getRecurrence());

        // Don't rely on exact date formatting; parse and compare actual dates
        LocalDate expectedNextDate = LocalDate.now().plusMonths(1);
        LocalDate actualNextDate = LocalDate.parse(updatedRecurring.getRecurrence().getNextExecutionDate(), DATE_FORMATTER);

        // Check that dates are the same; you might need to ignore time components
        assertEquals(expectedNextDate.getYear(), actualNextDate.getYear());
        assertEquals(expectedNextDate.getMonth(), actualNextDate.getMonth());

        // If you expect the day to be adjusted based on executeOnDay, use this instead of comparing day directly
        assertEquals(Math.min(expectedNextDate.lengthOfMonth(),
                        updatedRecurring.getRecurrence().getExecuteOnDay()),
                actualNextDate.getDayOfMonth());
    }
    @Test
    void testProcessRecurringTransactions_LastRecurrence() {
        // Arrange
        Transaction recurringTransaction = new Transaction();
        recurringTransaction.setId("recur123");
        recurringTransaction.setUserId("user123");
        recurringTransaction.setType("Expense");
        recurringTransaction.setCategory("Subscription");
        recurringTransaction.setAmount(15.0);
        recurringTransaction.setCurrency("USD");
        recurringTransaction.setIsRecurring(true);

        // Set next execution date to today with end date today as well
        RecurrenceDetails finalRecurrence = new RecurrenceDetails(
                "Monthly",
                LocalDate.now().minusMonths(6).format(DATE_FORMATTER),
                LocalDate.now().format(DATE_FORMATTER), // End date is today
                15,
                LocalDate.now().format(DATE_FORMATTER) // Due today
        );
        recurringTransaction.setRecurrence(finalRecurrence);

        List<Transaction> recurringTransactions = List.of(recurringTransaction);

        when(transactionsRepo.findByIsRecurringTrue()).thenReturn(recurringTransactions);
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionsService.processRecurringTransactions();

        // Assert
        verify(transactionsRepo, times(2)).save(transactionCaptor.capture());

        // Verify recurring transaction was updated correctly to no longer be recurring
        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        Transaction updatedRecurring = capturedTransactions.get(1);

        assertFalse(updatedRecurring.getIsRecurring());
        assertNull(updatedRecurring.getRecurrence());
    }

    @Test
    void testProcessRecurringTransactions_DailyPattern() {
        // Arrange
        Transaction recurringTransaction = new Transaction();
        recurringTransaction.setId("recur123");
        recurringTransaction.setUserId("user123");
        recurringTransaction.setType("Expense");
        recurringTransaction.setCategory("Coffee");
        recurringTransaction.setAmount(5.0);
        recurringTransaction.setCurrency("USD");
        recurringTransaction.setIsRecurring(true);

        // Set as a daily recurring transaction
        RecurrenceDetails dailyRecurrence = new RecurrenceDetails(
                "Daily",
                LocalDate.now().minusDays(5).format(DATE_FORMATTER),
                LocalDate.now().plusDays(10).format(DATE_FORMATTER),
                1, // Not used for daily
                LocalDate.now().format(DATE_FORMATTER) // Due today
        );
        recurringTransaction.setRecurrence(dailyRecurrence);

        List<Transaction> recurringTransactions = List.of(recurringTransaction);

        when(transactionsRepo.findByIsRecurringTrue()).thenReturn(recurringTransactions);
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionsService.processRecurringTransactions();

        // Assert
        verify(transactionsRepo, times(2)).save(transactionCaptor.capture());

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        Transaction updatedRecurring = capturedTransactions.get(1);

        assertTrue(updatedRecurring.getIsRecurring());
        assertNotNull(updatedRecurring.getRecurrence());
        assertEquals("Daily", updatedRecurring.getRecurrence().getPattern());
        assertEquals(LocalDate.now().plusDays(1).format(DATE_FORMATTER),
                updatedRecurring.getRecurrence().getNextExecutionDate());
    }

    @Test
    void testProcessRecurringTransactions_WeeklyPattern() {
        // Arrange
        Transaction recurringTransaction = new Transaction();
        recurringTransaction.setId("recur123");
        recurringTransaction.setUserId("user123");
        recurringTransaction.setType("Expense");
        recurringTransaction.setCategory("Gym");
        recurringTransaction.setAmount(20.0);
        recurringTransaction.setCurrency("USD");
        recurringTransaction.setIsRecurring(true);

        // Set as a weekly recurring transaction
        RecurrenceDetails weeklyRecurrence = new RecurrenceDetails(
                "Weekly",
                LocalDate.now().minusWeeks(2).format(DATE_FORMATTER),
                LocalDate.now().plusWeeks(8).format(DATE_FORMATTER),
                1, // Not used for weekly
                LocalDate.now().format(DATE_FORMATTER) // Due today
        );
        recurringTransaction.setRecurrence(weeklyRecurrence);

        List<Transaction> recurringTransactions = List.of(recurringTransaction);

        when(transactionsRepo.findByIsRecurringTrue()).thenReturn(recurringTransactions);
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionsService.processRecurringTransactions();

        // Assert
        verify(transactionsRepo, times(2)).save(transactionCaptor.capture());

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        Transaction updatedRecurring = capturedTransactions.get(1);

        assertTrue(updatedRecurring.getIsRecurring());
        assertNotNull(updatedRecurring.getRecurrence());
        assertEquals("Weekly", updatedRecurring.getRecurrence().getPattern());
        assertEquals(LocalDate.now().plusWeeks(1).format(DATE_FORMATTER),
                updatedRecurring.getRecurrence().getNextExecutionDate());
    }

    private Transaction createRecurringTransaction() {
        Transaction recurringTransaction = new Transaction();
        recurringTransaction.setId("recur123");
        recurringTransaction.setUserId("user123");
        recurringTransaction.setType("Expense");
        recurringTransaction.setCategory("Subscription");
        recurringTransaction.setTags(List.of("Entertainment", "Monthly"));
        recurringTransaction.setBeneficiary("Netflix");
        recurringTransaction.setSenderDescription("Monthly subscription");
        recurringTransaction.setAmount(15.0);
        recurringTransaction.setCurrency("USD");
        recurringTransaction.setIsRecurring(true);

        // Set next execution date to today (due for processing)
        RecurrenceDetails dueDetails = new RecurrenceDetails(
                "Monthly",
                LocalDate.now().minusMonths(1).format(DATE_FORMATTER),
                LocalDate.now().plusMonths(6).format(DATE_FORMATTER),
                15,
                LocalDate.now().format(DATE_FORMATTER) // Due today
        );
        recurringTransaction.setRecurrence(dueDetails);
        return recurringTransaction;
    }
}