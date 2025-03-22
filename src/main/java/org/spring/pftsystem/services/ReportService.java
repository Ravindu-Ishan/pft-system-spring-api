package org.spring.pftsystem.services;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.request.ReportRequest;
import org.spring.pftsystem.entity.schema.main.Report;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.entity.schema.sub.FilteredTransaction;
import org.spring.pftsystem.entity.schema.sub.Summary;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.repository.customImp.TransactionRepositoryImpl;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;


@Log
@Service
public class ReportService {

    private final TransactionRepositoryImpl transactionRepository;
    private final UserRepository userRepository;

    public ReportService(TransactionRepositoryImpl transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public Report generateReport(ReportRequest request) throws ParseException {
        List<String> typeFilters = null;
        switch (request.getReportType().toLowerCase()) {
            case "expenditure":
                typeFilters = List.of("Expense");
                break;
            case "income":
                typeFilters = List.of("Income");
                break;
            case "savings":
                typeFilters = List.of("Savings");
                break;
            case "cashflow":
                typeFilters = List.of("Expense", "Income", "Savings");  // Allow both types
                break;
            default:
                typeFilters = List.of("Expense", "Income", "Savings");
                break;
        }

        // Using string dates for MongoDB queries instead of Date objects
        String startDate = request.getTimePeriod().getStartDate();
        String endDate = request.getTimePeriod().getEndDate();

        User user = UserUtil.getUserFromContext(userRepository);
        String userId = user.getId();

        // Fetch and filter transactions based on the request
        List<Transaction> transactions = transactionRepository.findFilteredTransactions(
                userId,
                startDate,
                endDate,
                request.getFilters().getCategories(),
                request.getFilters().getTags(),
                typeFilters
        );

        log.info("Transactions fetched: " + transactions.size());
        log.info("Generating report...");

        // Assign Report Details from request
        Report response = new Report();
        response.setReportType(request.getReportType());
        response.setTimePeriod(request.getTimePeriod());
        response.setFilters(request.getFilters());

        // Map Transaction to FilteredTransaction
        List<FilteredTransaction> filteredTransactions = transactions.stream().map(this::mapToFilteredTransaction).toList();
        response.setTransactions(filteredTransactions);

        // Calculate summary
        Summary summary = generateSummary(transactions);

        response.setSummary(summary);

        log.info("Report generation finished");
        return response;
    }

    private FilteredTransaction mapToFilteredTransaction(Transaction transaction) {
        FilteredTransaction filteredTransaction = new FilteredTransaction();
        filteredTransaction.setType(transaction.getType());

        // Since MongoDB stores dates as strings, we don't need to parse and format
        // Just make sure the format is consistent with what we expect in the API response
        String transactionDate = transaction.getTransactionDate();

        // If format conversion is needed, we can add it here
        // For now, we'll just pass the string through
        filteredTransaction.setDate(transactionDate);

        filteredTransaction.setAmount(transaction.getAmount());
        filteredTransaction.setCategory(transaction.getCategory());
        filteredTransaction.setBeneficiary(transaction.getBeneficiary());
        filteredTransaction.setTags(transaction.getTags());
        filteredTransaction.setDescription(transaction.getSenderDescription());

        return filteredTransaction;
    }

    public Summary generateSummary(List<Transaction> transactions) {
        Summary summary = new Summary();

        // Variables to store totals
        double totalIncome = 0;
        double totalExpense = 0;
        double totalSavings = 0;

        Transaction highestExpenseTransaction = null;
        Transaction highestIncomeTransaction = null;

        // Set to track unique transaction dates for expense
        Set<String> uniqueExpenseDates = new HashSet<>();

        for (Transaction transaction : transactions) {
            double amount = transaction.getAmount();
            String transactionDate = transaction.getTransactionDate();

            switch (transaction.getType().toLowerCase()) {
                case "income":
                    totalIncome += amount;
                    if (highestIncomeTransaction == null || amount > highestIncomeTransaction.getAmount()) {
                        highestIncomeTransaction = transaction;
                    }
                    break;

                case "expense":
                    totalExpense += amount;
                    // Extract just the date part if necessary (assuming format contains date information)
                    uniqueExpenseDates.add(transactionDate); // Track unique expense dates

                    if (highestExpenseTransaction == null || amount > highestExpenseTransaction.getAmount()) {
                        highestExpenseTransaction = transaction;
                    }
                    break;

                case "savings":
                    totalSavings += amount; // Track savings separately
                    break;

                default:
                    // Ignore unknown types
                    log.warning("Unknown transaction type: " + transaction.getType() + " for transaction: " + transaction.getId());
                    break;
            }
        }

        // Compute Average Daily Expense
        int numExpenseDays = uniqueExpenseDates.size();
        double averageDailyExpense = numExpenseDays > 0 ? totalExpense / numExpenseDays : 0;

        // Set values in Summary
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setNetBalance(totalIncome - totalExpense);
        summary.setTotalSavings(totalSavings);
        summary.setBalanceAfterSavings(totalIncome + totalSavings - totalExpense);
        summary.setAverageDailyExpense(averageDailyExpense);

        // Set Highest Expense
        if (highestExpenseTransaction != null) {
            Summary.HighestExpense highestExpense = new Summary.HighestExpense();
            highestExpense.setAmount(highestExpenseTransaction.getAmount());
            highestExpense.setCategory(highestExpenseTransaction.getCategory());
            highestExpense.setDate(highestExpenseTransaction.getTransactionDate());
            summary.setHighestExpense(highestExpense);
        }

        // Set Highest Income
        if (highestIncomeTransaction != null) {
            Summary.HighestIncome highestIncome = new Summary.HighestIncome();
            highestIncome.setAmount(highestIncomeTransaction.getAmount());
            highestIncome.setSource(highestIncomeTransaction.getBeneficiary());
            highestIncome.setDate(highestIncomeTransaction.getTransactionDate());
            summary.setHighestIncome(highestIncome);
        }

        return summary;
    }
}