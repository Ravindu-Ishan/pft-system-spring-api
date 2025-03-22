package org.spring.pftsystem.services;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.spring.pftsystem.entity.schema.sub.RecurrenceDetails;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.exception.AppIllegalArgument;
import org.spring.pftsystem.exception.NotFoundException;
import org.spring.pftsystem.repository.SystemSettingsRepo;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Log
@Service
public class TransactionsService {

    private final TransactionsRepo transactionsRepo;
    private final UserRepository userRepository;
    private final SystemSettingsRepo systemSettingsRepo;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final BudgetService budgetService;

    // Constructor
    public TransactionsService(TransactionsRepo transactionsRepo, UserRepository userRepository, SystemSettingsRepo systemSettingsRepo, BudgetService budgetService) {
        this.transactionsRepo = transactionsRepo;
        this.userRepository = userRepository;
        this.systemSettingsRepo = systemSettingsRepo;
        this.budgetService = budgetService;
    }

    // Method to create a transaction
    public Transaction createTransaction(Transaction transaction) {

        transaction.setId(null); // Ensure that the id is null
        User user = UserUtil.getUserFromContext(userRepository);

        long transactionCount = transactionsRepo.countByUserId(user.getId());
        SystemSettings systemSettings = systemSettingsRepo.findFirstByOrderByIdAsc();

        if(transactionCount >= systemSettings.getTotalTransactionsLimit()) {
            throw new AppIllegalArgument("Maximum transactions limit for user reached", 400);
        }

        //set default currency if currency is not specified
        if(transaction.getCurrency().isEmpty()) {
            String defaultCurrency = user.getSettings().getCurrency();
            transaction.setCurrency(defaultCurrency);
        }

        transaction.setUserId(user.getId());

        //save the transaction
        Transaction savedTransaction = transactionsRepo.save(transaction);

        // update after saving transaction budget when a new expense transaction occurs
        if (transaction.getType().equalsIgnoreCase("expense")){
            log.info("Updating user budget");
            budgetService.updateBudgetForUser(user.getId());
        }
        return savedTransaction;
    }

    // Method to get a transaction by id
    public Transaction getTransactionById(String id) {
        return transactionsRepo.findById(id).orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    // Method to get all transactions
    public List <Transaction> getAllTransactions() {

        List <Transaction> transactionList =  transactionsRepo.findAll();
        if(transactionList.isEmpty()) {
            throw new NotFoundException("No transactions found");
        }
        return transactionList;
    }

    // Method to get all transactions of a user
    public List <Transaction> getAllTransactionsOfUser() {
        User user = UserUtil.getUserFromContext(userRepository);
        List <Transaction> transactionList =  transactionsRepo.findAllByUserId(user.getId());
        if(transactionList.isEmpty()) {
            throw new NotFoundException("No transactions found");
        }
        return transactionList;
    }

    // Method to get a transaction by user id
    public  List <Transaction> getTransactionByUserId(String uid) {
        List <Transaction> transactionList =  transactionsRepo.findAllByUserId(uid);

        if(transactionList.isEmpty()) {
            throw new NotFoundException("No transactions found");
        }
        return transactionList;
    }

    // Method to update a transaction
    public Transaction updateTransaction(String id, Transaction transaction) {

        User user = UserUtil.getUserFromContext(userRepository);

        Optional<Transaction> transactionOriginal = transactionsRepo.findById(id);  // Use the `id` parameter from the URL
        if(transactionOriginal.isEmpty()) {
            throw new NotFoundException("Transaction not found");
        }

        // Now update the fields of the transaction
        Transaction updatedTransaction = transactionOriginal.get();  // Get the original transaction for updating
        updatedTransaction.setType(transaction.getType());
        updatedTransaction.setCategory(transaction.getCategory());
        updatedTransaction.setTags(transaction.getTags());
        updatedTransaction.setBeneficiary(transaction.getBeneficiary());
        updatedTransaction.setSenderDescription(transaction.getSenderDescription());
        updatedTransaction.setAmount(transaction.getAmount());
        updatedTransaction.setNotify(transaction.isNotify());

        //set default currency if currency is not specified
        if(transaction.getCurrency().isEmpty()) {
            String defaultCurrency = user.getSettings().getCurrency();
            updatedTransaction.setCurrency(defaultCurrency);
        }else{
            updatedTransaction.setCurrency(transaction.getCurrency());
        }


        if (transaction.getIsRecurring()) {
            updatedTransaction.setIsRecurring(true);
            if (transaction.getRecurrence() != null) {
                RecurrenceDetails recurrenceDetails = new RecurrenceDetails(
                        transaction.getRecurrence().getPattern(),
                        transaction.getRecurrence().getStartDate(),
                        transaction.getRecurrence().getEndDate(),
                        transaction.getRecurrence().getExecuteOnDay(),
                        transaction.getRecurrence().getNextExecutionDate()
                );
                updatedTransaction.setRecurrence(recurrenceDetails);
            }else{
                throw new AppIllegalArgument("Recurrence details are required for recurring transactions", 400);
            }
        } else {
            updatedTransaction.setIsRecurring(false);
            updatedTransaction.setRecurrence(null);
        }


        updatedTransaction.setTransactionDate(transactionOriginal.get().getTransactionDate());
        updatedTransaction.setLastUpdatedAt(java.time.LocalDateTime.now().toString());
        return transactionsRepo.save(updatedTransaction);  // Save and return the updated transaction
    }


    // Method to delete a transaction
    public String deleteTransaction(String id) {
        try {
            transactionsRepo.deleteById(id);
            return "Transaction deleted successfully";
        }catch (Exception e) {
            throw new NotFoundException("Transaction not found");
        }
    }


    public void processRecurringTransactions() {
        log.info("Processing recurring transactions");

        // Fetch all recurring transactions
        List<Transaction> recurringTransactions = transactionsRepo.findByIsRecurringTrue();

        LocalDate today = LocalDate.now();
        int processedCount = 0;

        for (Transaction transaction : recurringTransactions) {
            if (transaction.getRecurrence() != null) {
                LocalDate nextExecution = LocalDate.parse(
                        transaction.getRecurrence().getNextExecutionDate(),
                        DATE_FORMATTER
                );

                // If the next execution date is today or has passed
                if (!nextExecution.isAfter(today)) {
                    log.info("Processing recurring transaction: " + transaction.getId());

                    // Create a new transaction
                    Transaction newTransaction = createTransactionFromRecurring(transaction);
                    transactionsRepo.save(newTransaction);

                    // Update the next execution date
                    updateNextExecutionDate(transaction);
                    transactionsRepo.save(transaction);

                    processedCount++;
                }
            }
        }

        log.info("Processed {} recurring transactions "+ processedCount);
    }

    /**
     * Create a new transaction from a recurring transaction template
     */
    private Transaction createTransactionFromRecurring(Transaction recurring) {
        Transaction newTransaction = new Transaction();

        // Copy relevant fields
        newTransaction.setUserId(recurring.getUserId());
        newTransaction.setType(recurring.getType());
        newTransaction.setCategory(recurring.getCategory());
        newTransaction.setTags(recurring.getTags());
        newTransaction.setBeneficiary(recurring.getBeneficiary());
        newTransaction.setSenderDescription(recurring.getSenderDescription());
        newTransaction.setAmount(recurring.getAmount());
        newTransaction.setCurrency(recurring.getCurrency());

        // Not a recurring transaction itself
        newTransaction.setIsRecurring(false);
        newTransaction.setRecurrence(null);

        // Set current timestamp
        String now = java.time.LocalDateTime.now().toString();
        newTransaction.setTransactionDate(now);
        newTransaction.setLastUpdatedAt(now);

        return newTransaction;
    }

    /**
     * Update the next execution date based on recurrence pattern
     */
    private void updateNextExecutionDate(Transaction transaction) {
        RecurrenceDetails recurrence = transaction.getRecurrence();
        LocalDate currentNextDate = LocalDate.parse(recurrence.getNextExecutionDate(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(recurrence.getEndDate(), DATE_FORMATTER);

        // Calculate next execution date based on pattern
        LocalDate nextDate;
        switch (recurrence.getPattern()) {
            case "Daily":
                nextDate = currentNextDate.plusDays(1);
                break;
            case "Weekly":
                nextDate = currentNextDate.plusWeeks(1);
                break;
            case "Monthly":
                nextDate = currentNextDate.plusMonths(1);
                // Adjust for month length if needed
                int day = recurrence.getExecuteOnDay();
                if (day > nextDate.lengthOfMonth()) {
                    nextDate = nextDate.withDayOfMonth(nextDate.lengthOfMonth());
                } else {
                    nextDate = nextDate.withDayOfMonth(day);
                }
                break;
            default:
                throw new IllegalStateException("Unknown recurrence pattern: " + recurrence.getPattern());
        }

        // Check if next date is beyond end date
        if (nextDate.isAfter(endDate)) {
            // This was the last recurrence
            transaction.setIsRecurring(false);
            transaction.setRecurrence(null);
        } else {
            // Update next execution date
            recurrence.setNextExecutionDate(nextDate.format(DATE_FORMATTER));
        }
    }



}
