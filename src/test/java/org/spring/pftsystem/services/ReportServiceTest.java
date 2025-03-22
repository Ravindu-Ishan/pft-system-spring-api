package org.spring.pftsystem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.request.ReportRequest;
import org.spring.pftsystem.entity.schema.main.Report;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.entity.schema.sub.*;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.repository.customImp.TransactionRepositoryImpl;
import org.spring.pftsystem.utility.UserUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private TransactionRepositoryImpl transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    private static final String TEST_USER_ID = "testUserId";
    private Transaction expenseTransaction;
    private Transaction incomeTransaction;
    private Transaction savingsTransaction;
    private ReportRequest reportRequest;
    private User testUser;

    @BeforeEach
    void setup() {
        // Setup test user
        testUser = new User();
        testUser.setId(TEST_USER_ID);

        // Setup test data with date as a String (simplified ISO format)
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        expenseTransaction = new Transaction();
        expenseTransaction.setId("expense1");
        expenseTransaction.setType("Expense");
        expenseTransaction.setCategory("Groceries");
        expenseTransaction.setTags(List.of("Food", "Essentials"));
        expenseTransaction.setBeneficiary("Supermarket");
        expenseTransaction.setSenderDescription("Weekly groceries");
        expenseTransaction.setAmount(100.0);
        expenseTransaction.setCurrency("USD");
        expenseTransaction.setTransactionDate(currentDate);

        incomeTransaction = new Transaction();
        incomeTransaction.setId("income1");
        incomeTransaction.setType("Income");
        incomeTransaction.setCategory("Salary");
        incomeTransaction.setTags(List.of("Monthly", "Work"));
        incomeTransaction.setBeneficiary("Company XYZ");
        incomeTransaction.setSenderDescription("Monthly salary");
        incomeTransaction.setAmount(3000.0);
        incomeTransaction.setCurrency("USD");
        incomeTransaction.setTransactionDate(currentDate);

        savingsTransaction = new Transaction();
        savingsTransaction.setId("savings1");
        savingsTransaction.setType("Savings");
        savingsTransaction.setCategory("Investment");
        savingsTransaction.setTags(List.of("Future", "Retirement"));
        savingsTransaction.setBeneficiary("Investment Fund");
        savingsTransaction.setSenderDescription("Monthly investment");
        savingsTransaction.setAmount(500.0);
        savingsTransaction.setCurrency("USD");
        savingsTransaction.setTransactionDate(currentDate);

        // Setup report request
        reportRequest = new ReportRequest();
        reportRequest.setReportType("cashflow");

        TimePeriod timePeriod = new TimePeriod();
        timePeriod.setStartDate("2023-01-01");
        timePeriod.setEndDate("2023-12-31");
        reportRequest.setTimePeriod(timePeriod);

        Filters filters = new Filters();
        filters.setCategories(new ArrayList<>());
        filters.setTags(new ArrayList<>());
        reportRequest.setFilters(filters);
    }

    @Test
    void testGenerateReport_CashflowType() throws ParseException {
        // Arrange
        List<Transaction> transactions = Arrays.asList(expenseTransaction, incomeTransaction, savingsTransaction);

        // Use string dates instead of Date objects
        String startDate = "2023-01-01";
        String endDate = "2023-12-31";

        try (MockedStatic<UserUtil> userUtilMock = Mockito.mockStatic(UserUtil.class)) {
            userUtilMock.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(testUser);

            when(transactionRepository.findFilteredTransactions(
                    eq(TEST_USER_ID),
                    eq(startDate),
                    eq(endDate),
                    anyList(),
                    anyList(),
                    eq(List.of("Expense", "Income", "Savings"))
            )).thenReturn(transactions);

            // Act
            Report report = reportService.generateReport(reportRequest);

            // Assert
            assertNotNull(report);
            assertEquals("cashflow", report.getReportType());
            assertEquals(3, report.getTransactions().size());

            Summary summary = report.getSummary();
            assertNotNull(summary);
            assertEquals(3000.0, summary.getTotalIncome());
            assertEquals(100.0, summary.getTotalExpense());
            assertEquals(2900.0, summary.getNetBalance());
            assertEquals(500.0, summary.getTotalSavings());
            assertEquals(3400.0, summary.getBalanceAfterSavings());

            // Check highest expense
            assertNotNull(summary.getHighestExpense());
            assertEquals(100.0, summary.getHighestExpense().getAmount());
            assertEquals("Groceries", summary.getHighestExpense().getCategory());

            // Check highest income
            assertNotNull(summary.getHighestIncome());
            assertEquals(3000.0, summary.getHighestIncome().getAmount());
            assertEquals("Company XYZ", summary.getHighestIncome().getSource());
        }
    }

    @Test
    void testGenerateReport_ExpenditureType() throws ParseException {
        // Arrange
        reportRequest.setReportType("expenditure");
        List<Transaction> transactions = List.of(expenseTransaction);

        // Use string dates instead of Date objects
        String startDate = "2023-01-01";
        String endDate = "2023-12-31";

        try (MockedStatic<UserUtil> userUtilMock = Mockito.mockStatic(UserUtil.class)) {
            userUtilMock.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(testUser);

            when(transactionRepository.findFilteredTransactions(
                    eq(TEST_USER_ID),
                    eq(startDate),
                    eq(endDate),
                    anyList(),
                    anyList(),
                    eq(List.of("Expense"))
            )).thenReturn(transactions);

            // Act
            Report report = reportService.generateReport(reportRequest);

            // Assert
            assertNotNull(report);
            assertEquals("expenditure", report.getReportType());
            assertEquals(1, report.getTransactions().size());

            Summary summary = report.getSummary();
            assertNotNull(summary);
            assertEquals(0.0, summary.getTotalIncome());
            assertEquals(100.0, summary.getTotalExpense());
            assertEquals(-100.0, summary.getNetBalance());

            // Check highest expense
            assertNotNull(summary.getHighestExpense());
            assertEquals(100.0, summary.getHighestExpense().getAmount());
            assertEquals("Groceries", summary.getHighestExpense().getCategory());

            // Highest income should be null as no income transactions
            assertNull(summary.getHighestIncome());
        }
    }

    @Test
    void testGenerateReport_IncomeType() throws ParseException {
        // Arrange
        reportRequest.setReportType("income");
        List<Transaction> transactions = List.of(incomeTransaction);

        // Use string dates instead of Date objects
        String startDate = "2023-01-01";
        String endDate = "2023-12-31";

        try (MockedStatic<UserUtil> userUtilMock = Mockito.mockStatic(UserUtil.class)) {
            userUtilMock.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(testUser);

            when(transactionRepository.findFilteredTransactions(
                    eq(TEST_USER_ID),
                    eq(startDate),
                    eq(endDate),
                    anyList(),
                    anyList(),
                    eq(List.of("Income"))
            )).thenReturn(transactions);

            // Act
            Report report = reportService.generateReport(reportRequest);

            // Assert
            assertNotNull(report);
            assertEquals("income", report.getReportType());
            assertEquals(1, report.getTransactions().size());

            Summary summary = report.getSummary();
            assertNotNull(summary);
            assertEquals(3000.0, summary.getTotalIncome());
            assertEquals(0.0, summary.getTotalExpense());
            assertEquals(3000.0, summary.getNetBalance());

            // Highest expense should be null as no expense transactions
            assertNull(summary.getHighestExpense());

            // Check highest income
            assertNotNull(summary.getHighestIncome());
            assertEquals(3000.0, summary.getHighestIncome().getAmount());
            assertEquals("Company XYZ", summary.getHighestIncome().getSource());
        }
    }

    @Test
    void testGenerateReport_SavingsType() throws ParseException {
        // Arrange
        reportRequest.setReportType("savings");
        List<Transaction> transactions = List.of(savingsTransaction);

        // Use string dates instead of Date objects
        String startDate = "2023-01-01";
        String endDate = "2023-12-31";

        try (MockedStatic<UserUtil> userUtilMock = Mockito.mockStatic(UserUtil.class)) {
            userUtilMock.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(testUser);

            when(transactionRepository.findFilteredTransactions(
                    eq(TEST_USER_ID),
                    eq(startDate),
                    eq(endDate),
                    anyList(),
                    anyList(),
                    eq(List.of("Savings"))
            )).thenReturn(transactions);

            // Act
            Report report = reportService.generateReport(reportRequest);

            // Assert
            assertNotNull(report);
            assertEquals("savings", report.getReportType());
            assertEquals(1, report.getTransactions().size());

            Summary summary = report.getSummary();
            assertNotNull(summary);
            assertEquals(0.0, summary.getTotalIncome());
            assertEquals(0.0, summary.getTotalExpense());
            assertEquals(0.0, summary.getNetBalance());
            assertEquals(500.0, summary.getTotalSavings());
            assertEquals(500.0, summary.getBalanceAfterSavings());
        }
    }

    @Test
    void testGenerateSummary() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(expenseTransaction, incomeTransaction, savingsTransaction);

        // Act
        Summary summary = reportService.generateSummary(transactions);

        // Assert
        assertNotNull(summary);
        assertEquals(3000.0, summary.getTotalIncome());
        assertEquals(100.0, summary.getTotalExpense());
        assertEquals(2900.0, summary.getNetBalance());
        assertEquals(500.0, summary.getTotalSavings());
        assertEquals(3400.0, summary.getBalanceAfterSavings());

        // Check average daily expense - since all transactions are on the same day
        assertEquals(100.0, summary.getAverageDailyExpense());

        // Check highest expense
        assertNotNull(summary.getHighestExpense());
        assertEquals(100.0, summary.getHighestExpense().getAmount());
        assertEquals("Groceries", summary.getHighestExpense().getCategory());

        // Check highest income
        assertNotNull(summary.getHighestIncome());
        assertEquals(3000.0, summary.getHighestIncome().getAmount());
        assertEquals("Company XYZ", summary.getHighestIncome().getSource());
    }

    @Test
    void testMapToFilteredTransaction() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setType("Expense");

        // Simply use a string date directly
        String dateString = "2023-05-15";
        transaction.setTransactionDate(dateString);

        transaction.setAmount(150.0);
        transaction.setCategory("Dining");
        transaction.setBeneficiary("Restaurant");
        transaction.setTags(List.of("Food", "Entertainment"));
        transaction.setSenderDescription("Dinner with friends");

        // This will trigger the private method, we need to use reflection to test it
        java.lang.reflect.Method method = ReportService.class.getDeclaredMethod("mapToFilteredTransaction", Transaction.class);
        method.setAccessible(true);

        // Act
        FilteredTransaction result = (FilteredTransaction) method.invoke(reportService, transaction);

        // Assert
        assertNotNull(result);
        assertEquals("Expense", result.getType());
        assertEquals(dateString, result.getDate()); // Date should remain the same string
        assertEquals(150.0, result.getAmount());
        assertEquals("Dining", result.getCategory());
        assertEquals("Restaurant", result.getBeneficiary());
        assertEquals(List.of("Food", "Entertainment"), result.getTags());
        assertEquals("Dinner with friends", result.getDescription());
    }

    // This test is no longer needed since we're not parsing dates anymore
    // But keeping a simplified version for robustness testing
    @Test
    void testMapToFilteredTransaction_InvalidDateFormat() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setType("Expense");
        transaction.setTransactionDate("Invalid date format");
        transaction.setAmount(150.0);
        transaction.setCategory("Dining");
        transaction.setBeneficiary("Restaurant");
        transaction.setTags(List.of("Food"));
        transaction.setSenderDescription("Dinner");

        // This will trigger the private method, we need to use reflection to test it
        java.lang.reflect.Method method = ReportService.class.getDeclaredMethod("mapToFilteredTransaction", Transaction.class);
        method.setAccessible(true);

        // Act
        FilteredTransaction result = (FilteredTransaction) method.invoke(reportService, transaction);

        // Assert
        assertNotNull(result);
        assertEquals("Invalid date format", result.getDate()); // Date should remain the same string, even if invalid
    }

    @Test
    void testGenerateReport_NoTransactions() throws ParseException {
        // Arrange
        // Use string dates instead of Date objects
        String startDate = "2023-01-01";
        String endDate = "2023-12-31";

        try (MockedStatic<UserUtil> userUtilMock = Mockito.mockStatic(UserUtil.class)) {
            userUtilMock.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(testUser);

            when(transactionRepository.findFilteredTransactions(
                    eq(TEST_USER_ID),
                    eq(startDate),
                    eq(endDate),
                    anyList(),
                    anyList(),
                    anyList()
            )).thenReturn(new ArrayList<>());

            // Act
            Report report = reportService.generateReport(reportRequest);

            // Assert
            assertNotNull(report);
            assertEquals("cashflow", report.getReportType());
            assertTrue(report.getTransactions().isEmpty());

            Summary summary = report.getSummary();
            assertNotNull(summary);
            assertEquals(0.0, summary.getTotalIncome());
            assertEquals(0.0, summary.getTotalExpense());
            assertEquals(0.0, summary.getNetBalance());
            assertEquals(0.0, summary.getTotalSavings());
            assertEquals(0.0, summary.getBalanceAfterSavings());
            assertEquals(0.0, summary.getAverageDailyExpense());
            assertNull(summary.getHighestExpense());
            assertNull(summary.getHighestIncome());
        }
    }
}