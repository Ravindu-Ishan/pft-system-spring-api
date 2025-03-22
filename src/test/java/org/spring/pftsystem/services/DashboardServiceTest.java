package org.spring.pftsystem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.response.DashboardAdmin;
import org.spring.pftsystem.entity.response.DashboardUser;
import org.spring.pftsystem.entity.response.TransactionsSummary;
import org.spring.pftsystem.entity.schema.main.Budget;
import org.spring.pftsystem.entity.schema.main.Goal;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.repository.BudgetRepository;
import org.spring.pftsystem.repository.GoalRepository;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionsRepo transactionsRepo;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private SystemUsageTracker systemUsageTracker;

    @InjectMocks
    private DashboardService dashboardService;

    private User mockUser;
    private List<Transaction> mockIncomeTransactions;
    private List<Transaction> mockExpenseTransactions;
    private List<Transaction> mockSavingsTransactions;
    private List<Goal> mockGoals;
    private Budget mockBudget;

    @BeforeEach
    void setUp() {
        // Set up a mock user
        mockUser = new User();
        mockUser.setId("user123");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");

        // Set up mock transactions
        mockIncomeTransactions = new ArrayList<>();
        Transaction incomeTransaction1 = new Transaction();
        incomeTransaction1.setAmount(1000.0);
        Transaction incomeTransaction2 = new Transaction();
        incomeTransaction2.setAmount(500.0);
        mockIncomeTransactions.add(incomeTransaction1);
        mockIncomeTransactions.add(incomeTransaction2);

        mockExpenseTransactions = new ArrayList<>();
        Transaction expenseTransaction1 = new Transaction();
        expenseTransaction1.setAmount(300.0);
        Transaction expenseTransaction2 = new Transaction();
        expenseTransaction2.setAmount(200.0);
        mockExpenseTransactions.add(expenseTransaction1);
        mockExpenseTransactions.add(expenseTransaction2);

        mockSavingsTransactions = new ArrayList<>();
        Transaction savingsTransaction1 = new Transaction();
        savingsTransaction1.setAmount(100.0);
        mockSavingsTransactions.add(savingsTransaction1);

        // Set up mock goals - fixed to match your schema
        mockGoals = new ArrayList<>();
        Goal goal1 = new Goal();
        goal1.setId("goal1");
        goal1.setUserID("user123");
        goal1.setGoalName("Vacation");
        goal1.setAmountRequired(5000.0);
        goal1.setMonthlyCommitment(200.0);
        goal1.setEnableAutoCollect(true);
        goal1.setCollectionDayOfMonth(15);
        goal1.setNotify(true);

        Goal goal2 = new Goal();
        goal2.setId("goal2");
        goal2.setUserID("user123");
        goal2.setGoalName("New Car");
        goal2.setAmountRequired(20000.0);
        goal2.setMonthlyCommitment(500.0);
        goal2.setEnableAutoCollect(true);
        goal2.setCollectionDayOfMonth(1);
        goal2.setNotify(false);

        mockGoals.add(goal1);
        mockGoals.add(goal2);

        // Set up mock budget
        mockBudget = new Budget();
        mockBudget.setId("budget1");
        mockBudget.setUserID("user123");
        mockBudget.setMonthlyLimit(2000.00F);
    }

    @Test
    void getDashboardUserData_ReturnsCorrectData() {
        // Arrange
        try (MockedStatic<UserUtil> mockedUserUtil = mockStatic(UserUtil.class)) {
            mockedUserUtil.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(mockUser);

            when(transactionsRepo.countByUserId("user123")).thenReturn(50L);
            when(transactionsRepo.countTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(10L);
            when(transactionsRepo.findSavingsTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(mockSavingsTransactions);
            when(transactionsRepo.findExpenseTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(mockExpenseTransactions);
            when(transactionsRepo.findIncomeTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(mockIncomeTransactions);
            when(budgetRepository.findByUserID("user123")).thenReturn(Optional.of(mockBudget));
            when(goalRepository.countByUserID("user123")).thenReturn(2L);
            when(goalRepository.findByUserID("user123")).thenReturn(mockGoals);

            // Act
            DashboardUser result = dashboardService.getDashboardUserData();

            // Assert
            assertNotNull(result);
            assertEquals("John Doe", result.getUsername());

            TransactionsSummary summary = result.getTransactionsSummary();
            assertNotNull(summary);
            assertEquals(50L, summary.getTotalTransactionsToDate());
            assertEquals(10L, summary.getTotalTransactionsThisMonth());
            assertEquals(100.0, summary.getTotalSavingsThisMonth());
            assertEquals(500.0, summary.getTotalExpensesThisMonth()); // 300 + 200
            assertEquals(1500.0, summary.getTotalIncomeThisMonth()); // 1000 + 500

            assertEquals(mockBudget, result.getBudget());
            assertEquals(2L, result.getOngoingGoalsCount());
            assertEquals(mockGoals, result.getOngoingGoals());

            // Verify interactions
            mockedUserUtil.verify(() -> UserUtil.getUserFromContext(userRepository));
            verify(transactionsRepo).countByUserId("user123");
            verify(transactionsRepo).countTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString());
            verify(transactionsRepo).findSavingsTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString());
            verify(transactionsRepo).findExpenseTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString());
            verify(transactionsRepo).findIncomeTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString());
            verify(budgetRepository).findByUserID("user123");
            verify(goalRepository).countByUserID("user123");
            verify(goalRepository).findByUserID("user123");
        }
    }

    @Test
    void getDashboardUserData_WithNoBudgetFound_CreatesNewBudget() {
        // Arrange
        try (MockedStatic<UserUtil> mockedUserUtil = mockStatic(UserUtil.class)) {
            mockedUserUtil.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(mockUser);

            when(budgetRepository.findByUserID("user123")).thenReturn(Optional.empty());
            // Other mocks remain the same as previous test...
            when(transactionsRepo.countByUserId("user123")).thenReturn(50L);
            when(transactionsRepo.countTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(10L);
            when(transactionsRepo.findSavingsTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(mockSavingsTransactions);
            when(transactionsRepo.findExpenseTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(mockExpenseTransactions);
            when(transactionsRepo.findIncomeTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(mockIncomeTransactions);
            when(goalRepository.countByUserID("user123")).thenReturn(2L);
            when(goalRepository.findByUserID("user123")).thenReturn(mockGoals);

            // Act
            DashboardUser result = dashboardService.getDashboardUserData();

            // Assert
            assertNotNull(result);
            assertNotNull(result.getBudget());
            assertTrue(result.getBudget() instanceof Budget);

            // Verify that findByUserID was called but no other interactions with budgetRepository
            verify(budgetRepository).findByUserID("user123");
            verifyNoMoreInteractions(budgetRepository);
        }
    }

    @Test
    void getAdminDashboardData_ReturnsCorrectData() {
        // Arrange
        try (MockedStatic<UserUtil> mockedUserUtil = mockStatic(UserUtil.class)) {
            mockedUserUtil.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(mockUser);

            when(transactionsRepo.count()).thenReturn(1000L);
            when(transactionsRepo.countTransactionsByDateBetween(anyString(), anyString())).thenReturn(200L);
            when(userRepository.count()).thenReturn(50L);
            when(systemUsageTracker.getTotalRequestCount()).thenReturn(5000L);

            // Act
            DashboardAdmin result = dashboardService.getAdminDashboardData();

            // Assert
            assertNotNull(result);
            assertEquals("John Doe", result.getUsername());
            assertEquals(1000L, result.getTotalTransactionsToDate());
            assertEquals(200L, result.getTotalTransactionsThisMonth());
            assertEquals(50, result.getTotalUsers());
            assertEquals(5000L, result.getSystemUsage());

            // Verify interactions
            mockedUserUtil.verify(() -> UserUtil.getUserFromContext(userRepository));
            verify(transactionsRepo).count();
            verify(transactionsRepo).countTransactionsByDateBetween(anyString(), anyString());
            verify(userRepository).count();
            verify(systemUsageTracker).getTotalRequestCount();
        }
    }

    @Test
    void getDashboardUserData_WithEmptyTransactions_ReturnsZeroSums() {
        // Arrange
        try (MockedStatic<UserUtil> mockedUserUtil = mockStatic(UserUtil.class)) {
            mockedUserUtil.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(mockUser);

            when(transactionsRepo.countByUserId("user123")).thenReturn(0L);
            when(transactionsRepo.countTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(0L);
            when(transactionsRepo.findSavingsTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(new ArrayList<>());
            when(transactionsRepo.findExpenseTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(new ArrayList<>());
            when(transactionsRepo.findIncomeTransactionsByUserIdAndTransactionDateBetween(
                    eq("user123"), anyString(), anyString())).thenReturn(new ArrayList<>());
            when(budgetRepository.findByUserID("user123")).thenReturn(Optional.of(mockBudget));
            when(goalRepository.countByUserID("user123")).thenReturn(0L);
            when(goalRepository.findByUserID("user123")).thenReturn(new ArrayList<>());

            // Act
            DashboardUser result = dashboardService.getDashboardUserData();

            // Assert
            assertNotNull(result);
            TransactionsSummary summary = result.getTransactionsSummary();
            assertEquals(0L, summary.getTotalTransactionsToDate());
            assertEquals(0L, summary.getTotalTransactionsThisMonth());
            assertEquals(0.0, summary.getTotalSavingsThisMonth());
            assertEquals(0.0, summary.getTotalExpensesThisMonth());
            assertEquals(0.0, summary.getTotalIncomeThisMonth());
            assertEquals(0L, result.getOngoingGoalsCount());
            assertTrue(result.getOngoingGoals().isEmpty());
        }
    }

    @Test
    void getAdminDashboardData_WithNoActivity_ReturnsZeroCounts() {
        // Arrange
        try (MockedStatic<UserUtil> mockedUserUtil = mockStatic(UserUtil.class)) {
            mockedUserUtil.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(mockUser);

            when(transactionsRepo.count()).thenReturn(0L);
            when(transactionsRepo.countTransactionsByDateBetween(anyString(), anyString())).thenReturn(0L);
            when(userRepository.count()).thenReturn(0L);
            when(systemUsageTracker.getTotalRequestCount()).thenReturn(0L);

            // Act
            DashboardAdmin result = dashboardService.getAdminDashboardData();

            // Assert
            assertNotNull(result);
            assertEquals(0L, result.getTotalTransactionsToDate());
            assertEquals(0L, result.getTotalTransactionsThisMonth());
            assertEquals(0, result.getTotalUsers());
            assertEquals(0L, result.getSystemUsage());
        }
    }
}