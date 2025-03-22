package org.spring.pftsystem.services;

import lombok.extern.java.Log;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Log
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final TransactionsRepo transactionsRepo;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final SystemUsageTracker systemUsageTracker;

    public DashboardService(UserRepository userRepository, TransactionsRepo transactionsRepo, BudgetRepository budgetRepository, GoalRepository goalRepository, SystemUsageTracker systemUsageTracker) {
        this.userRepository = userRepository;
        this.transactionsRepo = transactionsRepo;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.systemUsageTracker = systemUsageTracker;
    }

    public DashboardUser getDashboardUserData() {
        User user = UserUtil.getUserFromContext(userRepository);
        log.info("User: " + user.toString());

        long transactionCount = transactionsRepo.countByUserId(user.getId());
        String username = user.getFirstName() + " " + user.getLastName();

        // Replace your current date handling code with this:
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        String startInstant = startOfMonth.atZone(ZoneId.systemDefault()).toInstant().toString();

        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
        String endInstant = startOfNextMonth.atZone(ZoneId.systemDefault()).toInstant().toString();

        log.info("Start date: " + startInstant);
        log.info("End date: " + endInstant);

        long transactionsThisMonth = transactionsRepo.countTransactionsByUserIdAndTransactionDateBetween(user.getId(), startInstant, endInstant);
        List<Transaction> savingsTransactionsThisMonth = transactionsRepo.findSavingsTransactionsByUserIdAndTransactionDateBetween(user.getId(), startInstant, endInstant);
        double totalSavingsThisMonth = savingsTransactionsThisMonth.stream().mapToDouble(Transaction::getAmount).sum();
        List<Transaction> expenseTransactionsThisMonth = transactionsRepo.findExpenseTransactionsByUserIdAndTransactionDateBetween(user.getId(), startInstant, endInstant);
        double totalExpensesThisMonth = expenseTransactionsThisMonth.stream().mapToDouble(Transaction::getAmount).sum();
        List<Transaction> incomeTransactionsThisMonth = transactionsRepo.findIncomeTransactionsByUserIdAndTransactionDateBetween(user.getId(), startInstant, endInstant);
        double totalIncomeThisMonth = incomeTransactionsThisMonth.stream().mapToDouble(Transaction::getAmount).sum();


        TransactionsSummary transactionsSummary = new TransactionsSummary();
        transactionsSummary.setTotalExpensesThisMonth(totalExpensesThisMonth);
        transactionsSummary.setTotalTransactionsToDate(transactionCount);
        transactionsSummary.setTotalTransactionsThisMonth(transactionsThisMonth);
        transactionsSummary.setTotalSavingsThisMonth(totalSavingsThisMonth);
        transactionsSummary.setTotalExpensesThisMonth(totalExpensesThisMonth);
        transactionsSummary.setTotalIncomeThisMonth(totalIncomeThisMonth);

        Optional<Budget> existingBudget = budgetRepository.findByUserID(user.getId());
        Budget budget = existingBudget.orElse(new Budget());

        long goalCount = goalRepository.countByUserID(user.getId());
        List <Goal> ongoingGoals = goalRepository.findByUserID(user.getId());

        DashboardUser dashboardUser = new DashboardUser();
        dashboardUser.setUsername(username);
        dashboardUser.setTransactionsSummary(transactionsSummary);
        dashboardUser.setBudget(budget);
        dashboardUser.setOngoingGoalsCount(goalCount);
        dashboardUser.setOngoingGoals(ongoingGoals);

        return dashboardUser;
    }

    public DashboardAdmin getAdminDashboardData() {
        // Get the admin user from the security context
        User adminUser = UserUtil.getUserFromContext(userRepository);
        String username = adminUser.getFirstName() + " " + adminUser.getLastName();

        // Count all transactions in the system
        long totalTransactionsToDate = transactionsRepo.count();

        // Get date range for "this month"
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        // Format dates as strings to match your database format - adjust format pattern if needed
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
        String startDate = startOfMonth.format(formatter);
        String endDate = startOfNextMonth.format(formatter);

        // Count transactions for this month
        long totalTransactionsThisMonth = transactionsRepo.countTransactionsByDateBetween(startDate, endDate);

        // Count total users
        int totalUsers = (int) userRepository.count();

        // Get system usage metric
        long systemUsage = systemUsageTracker.getTotalRequestCount();

        // Build the response
        DashboardAdmin dashboardAdmin = new DashboardAdmin();
        dashboardAdmin.setUsername(username);
        dashboardAdmin.setTotalTransactionsToDate(totalTransactionsToDate);
        dashboardAdmin.setTotalTransactionsThisMonth(totalTransactionsThisMonth);
        dashboardAdmin.setTotalUsers(totalUsers);
        dashboardAdmin.setSystemUsage(systemUsage);

        return dashboardAdmin;
    }

}
