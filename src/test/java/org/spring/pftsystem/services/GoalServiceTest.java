package org.spring.pftsystem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoalContributionRepository goalContributionRepository;

    @Mock
    private TransactionsRepo transactionsRepo;

    @InjectMocks
    private GoalService goalService;

    private User testUser;
    private Goal testGoal;
    private List<Goal> goalList;
    private GoalContribution testContribution;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId("user123");

        // Create test goal
        testGoal = new Goal();
        testGoal.setId("goal123");
        testGoal.setUserID("user123");
        testGoal.setGoalName("Test Goal");
        testGoal.setMonthlyCommitment(100.0);
        testGoal.setAmountRequired(1000.0);
        testGoal.setEnableAutoCollect(true);
        testGoal.setCollectionDayOfMonth(LocalDate.now().getDayOfMonth());

        // Create list of goals
        goalList = new ArrayList<>();
        goalList.add(testGoal);

        // Create test contribution
        testContribution = new GoalContribution();
        testContribution.setGoalId("goal123");
        testContribution.setUserId("user123");
        testContribution.setAmount(100.0);

        // Mock static method in UserUtil
        try (var utilities = mockStatic(UserUtil.class)) {
            utilities.when(() -> UserUtil.getUserFromContext(any(UserRepository.class))).thenReturn(testUser);
        }
    }

    @Test
    void createGoal_ShouldCreateAndReturnGoal() {
        // Arrange
        Goal inputGoal = new Goal();
        inputGoal.setGoalName("New Goal");

        try (var utilities = mockStatic(UserUtil.class)) {
            utilities.when(() -> UserUtil.getUserFromContext(any(UserRepository.class))).thenReturn(testUser);

            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
                Goal savedGoal = invocation.getArgument(0);
                savedGoal.setId("generatedId123");
                return savedGoal;
            });

            // Act
            Goal result = goalService.createGoal(inputGoal);

            // Assert
            assertNotNull(result);
            assertEquals("generatedId123", result.getId());
            assertEquals("user123", result.getUserID());
            assertEquals("New Goal", result.getGoalName());

            verify(goalRepository, times(1)).save(any(Goal.class));
        }
    }

    @Test
    void getAllGoals_ShouldReturnAllGoals() {
        // Arrange
        when(goalRepository.findAll()).thenReturn(goalList);

        // Act
        List<Goal> result = goalService.getAllGoals();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("goal123", result.get(0).getId());

        verify(goalRepository, times(1)).findAll();
    }

    @Test
    void getGoalsByUserID_ShouldReturnUserGoals() {
        // Arrange
        when(goalRepository.findByUserID(anyString())).thenReturn(goalList);

        // Act
        List<Goal> result = goalService.getGoalsByUserID("user123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user123", result.get(0).getUserID());

        verify(goalRepository, times(1)).findByUserID("user123");
    }

    @Test
    void getGoalsOfUser_ShouldReturnCurrentUserGoals() {
        // Arrange
        try (var utilities = mockStatic(UserUtil.class)) {
            utilities.when(() -> UserUtil.getUserFromContext(any(UserRepository.class))).thenReturn(testUser);

            when(goalRepository.findByUserID(anyString())).thenReturn(goalList);

            // Act
            List<Goal> result = goalService.getGoalsOfUser();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("user123", result.get(0).getUserID());

            verify(goalRepository, times(1)).findByUserID("user123");
        }
    }

    @Test
    void getGoalById_WhenGoalExists_ShouldReturnGoal() {
        // Arrange
        when(goalRepository.findById(anyString())).thenReturn(Optional.of(testGoal));

        // Act
        Goal result = goalService.getGoalById("goal123");

        // Assert
        assertNotNull(result);
        assertEquals("goal123", result.getId());

        verify(goalRepository, times(1)).findById("goal123");
    }

    @Test
    void getGoalById_WhenGoalDoesNotExist_ShouldThrowNotFoundException() {
        // Arrange
        when(goalRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> goalService.getGoalById("nonexistent"));

        verify(goalRepository, times(1)).findById("nonexistent");
    }

    @Test
    void updateGoal_WhenGoalExists_ShouldUpdateAndReturnGoal() {
        // Arrange
        Goal updatedGoal = new Goal();
        updatedGoal.setGoalName("Updated Goal");
        updatedGoal.setAmountRequired(2000.0);

        when(goalRepository.findById(anyString())).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Goal result = goalService.updateGoal("goal123", updatedGoal);

        // Assert
        assertNotNull(result);
        assertEquals("goal123", result.getId());
        assertEquals("user123", result.getUserID());
        assertEquals("Updated Goal", result.getGoalName());
        assertEquals(2000.0, result.getAmountRequired());

        verify(goalRepository, times(1)).findById("goal123");
        verify(goalRepository, times(1)).save(any(Goal.class));
    }

    @Test
    void deleteGoal_WhenGoalExists_ShouldDeleteGoal() {
        // Arrange
        when(goalRepository.existsById(anyString())).thenReturn(true);
        doNothing().when(goalRepository).deleteById(anyString());

        // Act
        goalService.deleteGoal("goal123");

        // Assert
        verify(goalRepository, times(1)).existsById("goal123");
        verify(goalRepository, times(1)).deleteById("goal123");
    }

    @Test
    void deleteGoal_WhenGoalDoesNotExist_ShouldThrowNotFoundException() {
        // Arrange
        when(goalRepository.existsById(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> goalService.deleteGoal("nonexistent"));

        verify(goalRepository, times(1)).existsById("nonexistent");
        verify(goalRepository, never()).deleteById(anyString());
    }

    @Test
    void updateAllGoals_WithMatchingCollectionDay_ShouldProcessAutoCollections() {
        // Arrange
        when(goalRepository.findByEnableAutoCollectTrue()).thenReturn(Arrays.asList(testGoal));
        when(goalContributionRepository.save(any(GoalContribution.class))).thenReturn(testContribution);
        when(transactionsRepo.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        goalService.updateAllGoals();

        // Assert
        verify(goalRepository, times(1)).findByEnableAutoCollectTrue();
        verify(goalContributionRepository, times(1)).save(any(GoalContribution.class));
        verify(transactionsRepo, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateAllGoals_WithNonMatchingCollectionDay_ShouldNotProcessCollections() {
        // Arrange
        Goal nonMatchingGoal = new Goal();
        nonMatchingGoal.setId("goal456");
        nonMatchingGoal.setUserID("user123");
        nonMatchingGoal.setEnableAutoCollect(true);
        nonMatchingGoal.setCollectionDayOfMonth(LocalDate.now().getDayOfMonth() + 1); // Different day

        when(goalRepository.findByEnableAutoCollectTrue()).thenReturn(Arrays.asList(nonMatchingGoal));

        // Act
        goalService.updateAllGoals();

        // Assert
        verify(goalRepository, times(1)).findByEnableAutoCollectTrue();
        verify(goalContributionRepository, never()).save(any(GoalContribution.class));
        verify(transactionsRepo, never()).save(any(Transaction.class));
    }

    @Test
    void getCurrentAmountForGoal_ShouldReturnSumOfContributions() {
        // Arrange
        List<GoalContribution> contributions = new ArrayList<>();
        contributions.add(new GoalContribution("id1", "goal123", "user123", 100.0, "2021-01-01"));
        contributions.add(new GoalContribution("id2", "goal123", "user123", 150.0, "2021-01-02"));

        when(goalContributionRepository.findByGoalId(anyString())).thenReturn(contributions);

        // Act
        double result = goalService.getCurrentAmountForGoal("goal123");

        // Assert
        assertEquals(250.0, result);
        verify(goalContributionRepository, times(1)).findByGoalId("goal123");
    }

    @Test
    void getCurrentAmountForGoal_WithNoContributions_ShouldReturnZero() {
        // Arrange
        when(goalContributionRepository.findByGoalId(anyString())).thenReturn(new ArrayList<>());

        // Act
        double result = goalService.getCurrentAmountForGoal("goal123");

        // Assert
        assertEquals(0.0, result);
        verify(goalContributionRepository, times(1)).findByGoalId("goal123");
    }
}
