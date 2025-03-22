package org.spring.pftsystem.services;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.schema.main.Goal;
import org.spring.pftsystem.entity.schema.main.GoalContribution;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.exception.NotFoundException;
import org.spring.pftsystem.repository.GoalContributionRepository;
import org.spring.pftsystem.repository.GoalRepository;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Log
@Service
public class GoalService {
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final TransactionsRepo transactionsRepo;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository, GoalContributionRepository goalContributionRepository, TransactionsRepo transactionsRepo) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.goalContributionRepository = goalContributionRepository;
        this.transactionsRepo = transactionsRepo;
    }

    public Goal createGoal(Goal goal) {
        User user = UserUtil.getUserFromContext(userRepository);
        goal.setId(null);
        goal.setUserID(user.getId());
        return goalRepository.save(goal);
    }

    public List<Goal> getAllGoals() {
        return goalRepository.findAll();
    }

    public List<Goal> getGoalsByUserID(String userID) {
        return goalRepository.findByUserID(userID);
    }

    public List<Goal> getGoalsOfUser() {
        User user = UserUtil.getUserFromContext(userRepository);
        return goalRepository.findByUserID(user.getId());
    }

    public Goal getGoalById(String id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Goal not found with ID: " + id));
    }

    public Goal updateGoal(String id, Goal updatedGoal) {
        Goal existingGoal = getGoalById(id);
        updatedGoal.setId(existingGoal.getId());
        updatedGoal.setUserID(existingGoal.getUserID());
        return goalRepository.save(updatedGoal);
    }

    public void deleteGoal(String id) {
        if (!goalRepository.existsById(id)) {
            throw new NotFoundException("Goal not found with ID: " + id);
        }
        goalRepository.deleteById(id);
    }

    /**
     * Update all goals and process auto-collections if needed
     */
    public void updateAllGoals() {
        log.info("Updating all goals");

        // Get all goals with auto-collect enabled
        List<Goal> autoCollectGoals = goalRepository.findByEnableAutoCollectTrue();

        // Process auto-collections
        int collectionsProcessed = 0;
        LocalDate today = LocalDate.now();

        for (Goal goal : autoCollectGoals) {
            // Check if today is collection day
            if (today.getDayOfMonth() == goal.getCollectionDayOfMonth()) {
                log.info("Processing auto-collection for goal: " +  goal.getId());

                // Create contribution
                GoalContribution contribution = new GoalContribution();
                contribution.setGoalId(goal.getId());
                contribution.setUserId(goal.getUserID());
                contribution.setAmount(goal.getMonthlyCommitment());
                goalContributionRepository.save(contribution);

                // Create transaction record (categorized as Savings)
                createSavingsTransaction(goal);

                collectionsProcessed++;
            }
        }

        log.info("Processed auto-collections : " + collectionsProcessed);
    }

    /**
     * Create a savings transaction for a goal's auto-collection
     */
    private void createSavingsTransaction(Goal goal) {
        Transaction transaction = new Transaction();

        transaction.setUserId(goal.getUserID());
        transaction.setType("Savings");
        transaction.setCategory("Goal Contribution");
        transaction.setBeneficiary("Self");
        transaction.setSenderDescription("Auto-collection for " + goal.getGoalName());
        transaction.setAmount(goal.getMonthlyCommitment());
        transaction.setCurrency("LKR"); // Assuming USD, adjust as needed
        transaction.setIsRecurring(false);

        // Set current timestamp
        String now = java.time.LocalDateTime.now().toString();
        transaction.setTransactionDate(now);
        transaction.setLastUpdatedAt(now);

        transactionsRepo.save(transaction);
    }

    /**
     * Get current amount saved for a specific goal
     */
    public double getCurrentAmountForGoal(String goalId) {
        List<GoalContribution> contributions = goalContributionRepository.findByGoalId(goalId);

        return contributions.stream()
                .mapToDouble(GoalContribution::getAmount)
                .sum();
    }


}