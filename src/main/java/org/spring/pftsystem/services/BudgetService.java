package org.spring.pftsystem.services;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.schema.main.Budget;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.exception.AppIllegalArgument;
import org.spring.pftsystem.exception.NotFoundException;
import org.spring.pftsystem.repository.BudgetRepository;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
@Log
@Service
public class BudgetService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final float BUDGET_WARNING_THRESHOLD = 0.8f; // 80% of budget - to do : move to system settings or user settings

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final TransactionsRepo transactionsRepo;

    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository, TransactionsRepo transactionsRepo) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.transactionsRepo = transactionsRepo;
    }

    public Budget createBudget(Budget budget) {
        User user = UserUtil.getUserFromContext(userRepository);

        // Check if the user already has a budget
        Optional<Budget> existingBudget = budgetRepository.findByUserID(user.getId());
        if (existingBudget.isPresent()) {
            throw new AppIllegalArgument("User already has a budget. You can only have one budget.", 400);
        }

        // Set user ID and validate
        budget.setUserID(user.getId());
        validateBudget(budget, user);

        // Save and return the new budget
        return budgetRepository.save(budget);
    }

    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public Budget getBudgetForUser() {
        User user = UserUtil.getUserFromContext(userRepository);
        Optional<Budget> existingBudget = budgetRepository.findByUserID(user.getId());
        if (existingBudget.isEmpty()) {
            throw new NotFoundException("No budget found for the current user");
        }
        return existingBudget.get();
    }

    public Budget updateBudget(String id, Budget updatedBudget) {

        Optional<Budget> originalBudget = budgetRepository.findById(id);
        if (!budgetRepository.existsById(id)) {
            throw new NotFoundException("Budget not found with ID: " + id);
        }
        // Check for budget update permission
        User user = UserUtil.getUserFromContext(userRepository);
        String role = user.getRole();
        // Ensure the budget belongs to the current user
        if(role.equalsIgnoreCase("user")){
            if (!originalBudget.get().getUserID().equals(user.getId())) {
                throw new AppIllegalArgument("You do not have permission to update this budget", 403);
            }
        }
        updatedBudget.setId(originalBudget.get().getId());
        updatedBudget.setUserID(user.getId());
        validateBudget(updatedBudget, user);

        return budgetRepository.save(updatedBudget);
    }

    public void deleteBudget(String id) {
        if (!budgetRepository.existsById(id)) {
            throw new NotFoundException("Budget not found with ID: " + id);
        }
        budgetRepository.deleteById(id);
    }

    public Optional<Budget> getBudgetById(String id) {
        return budgetRepository.findById(id);
    }

    private void validateBudget(Budget budget, User user) {
        // Set default currency if not specified
        if (budget.getCurrency() == null || budget.getCurrency().isEmpty()) {
            String defaultCurrency = user.getSettings().getCurrency();
            budget.setCurrency(defaultCurrency);
        }

        // Validate category limits if the flag is on
        if (budget.isCategoryLimitsOn() && (budget.getCategoryLimits() == null || budget.getCategoryLimits().isEmpty())) {
            throw new AppIllegalArgument("Category limits flag is on but no category limits are specified", 400);
        }
    }

    /**
     * Update all budgets with current expenditure and set warning flags
     */
    public void updateAllBudgets() {
        log.info("Updating all budgets");

        List<Budget> budgets = budgetRepository.findAll();
        int updatedCount = 0;

        for (Budget budget : budgets) {
            try {
                float currentExpenditure = calculateCurrentMonthExpenditure(budget.getUserID(), budget.getCurrency());
                budget.setCurrentExpenditure(currentExpenditure);

                // Set warning flag if over threshold
                boolean shouldWarn = budget.getMonthlyLimit() > 0 &&
                        currentExpenditure >= BUDGET_WARNING_THRESHOLD * budget.getMonthlyLimit();
                budget.setWarning(shouldWarn);

                budgetRepository.save(budget);
                updatedCount++;
            } catch (Exception e) {
                log.severe("Error updating budget {}: {}" + budget.getId() + " " + e.getMessage());
            }
        }

        log.info("Updated budgets : " +  updatedCount);
    }

    /**
     * Calculate current month's expenditure for a user in specified currency
     */
    public void updateBudgetForUser(String userId) {
        Optional<Budget> budgetOpt = budgetRepository.findByUserID(userId);
        if (budgetOpt.isPresent()) {
            Budget budget = budgetOpt.get();
            float currentExpenditure = calculateCurrentMonthExpenditure(userId, budget.getCurrency());
            budget.setCurrentExpenditure(currentExpenditure);

            boolean shouldWarn = budget.getMonthlyLimit() > 0 &&
                    currentExpenditure >= BUDGET_WARNING_THRESHOLD * budget.getMonthlyLimit();
            budget.setWarning(shouldWarn);
            log.info("User Budget updated");
            budgetRepository.save(budget);
        }
    }

    private float calculateCurrentMonthExpenditure(String userId, String currency) {
        // Get current month's start and end dates
        YearMonth currentMonth = YearMonth.now();
        LocalDate firstDay = currentMonth.atDay(1);
        LocalDate lastDay = currentMonth.atEndOfMonth();

        String startDate = firstDay.atStartOfDay().format(DATE_FORMATTER);
        String endDate = lastDay.atTime(23, 59, 59).format(DATE_FORMATTER);

        // Get all transactions for this month
        List<Transaction> transactions = transactionsRepo.findByUserIdAndTransactionDateBetween(
                userId, startDate, endDate);

        // Sum up all expenses (filter by type and currency)
        return (float) transactions.stream()
                .filter(t -> "Expense".equals(t.getType()) && currency.equals(t.getCurrency()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

}
