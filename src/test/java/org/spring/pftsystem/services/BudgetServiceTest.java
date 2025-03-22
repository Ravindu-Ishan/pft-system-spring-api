package org.spring.pftsystem.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.schema.main.Budget;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.entity.schema.sub.UserSettings;
import org.spring.pftsystem.exception.AppIllegalArgument;
import org.spring.pftsystem.exception.NotFoundException;
import org.spring.pftsystem.repository.BudgetRepository;
import org.spring.pftsystem.repository.TransactionsRepo;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionsRepo transactionsRepo;

    @InjectMocks
    private BudgetService budgetService;

    private MockedStatic<UserUtil> userUtilMock;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user with settings
        testUser = new User();
        testUser.setId("user123");
        UserSettings settings = new UserSettings();
        settings.setCurrency("USD");
        testUser.setSettings(settings);

        // Mock the static UserUtil class
        userUtilMock = Mockito.mockStatic(UserUtil.class);
        userUtilMock.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(testUser);
    }

    @AfterEach
    void tearDown() {
        // Important: Close the static mock to prevent memory leaks
        if (userUtilMock != null) {
            userUtilMock.close();
        }
    }

    @Test
    void testCreateBudget_Success() {
        // Arrange
        Budget budget = new Budget();

        when(budgetRepository.findByUserID("user123")).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        // Act
        Budget createdBudget = budgetService.createBudget(budget);

        // Assert
        assertNotNull(createdBudget);
        assertEquals("user123", createdBudget.getUserID());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void testCreateBudget_UserAlreadyHasBudget() {
        // Arrange
        Budget budget = new Budget();
        Budget existingBudget = new Budget();
        existingBudget.setUserID("user123");

        when(budgetRepository.findByUserID("user123")).thenReturn(Optional.of(existingBudget));

        // Act & Assert
        assertThrows(AppIllegalArgument.class, () -> budgetService.createBudget(budget));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void testGetBudgetForUser_Success() {
        // Arrange
        Budget budget = new Budget();
        budget.setUserID("user123");

        when(budgetRepository.findByUserID("user123")).thenReturn(Optional.of(budget));

        // Act
        Budget foundBudget = budgetService.getBudgetForUser();

        // Assert
        assertNotNull(foundBudget);
        assertEquals("user123", foundBudget.getUserID());
    }

    @Test
    void testGetBudgetForUser_NotFound() {
        // Arrange
        when(budgetRepository.findByUserID("user123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> budgetService.getBudgetForUser());
    }

    @Test
    void testUpdateBudget_Success() {
        // Arrange
        Budget originalBudget = new Budget();
        originalBudget.setId("budget123");
        originalBudget.setUserID("user123");

        Budget updatedBudget = new Budget();

        // Mock current user
        User mockUser = new User();
        mockUser.setId("user123");
        mockUser.setRole("user");

        // Mock the repository and UserUtil
        when(budgetRepository.findById("budget123")).thenReturn(Optional.of(originalBudget));
        when(budgetRepository.existsById("budget123")).thenReturn(true);

        // For static method, you'd typically use mockStatic with PowerMockito,
        // but for now let's assume you've set up a way to mock this
        when(UserUtil.getUserFromContext(userRepository)).thenReturn(mockUser);

        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget savedBudget = invocation.getArgument(0);
            return savedBudget;
        });

        // Act
        Budget result = budgetService.updateBudget("budget123", updatedBudget);

        // Assert
        assertNotNull(result);
        assertEquals("budget123", result.getId());
        assertEquals("user123", result.getUserID());

        // Verify repository interactions only
        verify(budgetRepository).findById("budget123");
        verify(budgetRepository).existsById("budget123");
        verify(budgetRepository).save(any(Budget.class));

        // Don't verify static methods or non-mock objects
    }

    @Test
    void testUpdateBudget_NotFound() {
        // Arrange
        when(budgetRepository.existsById("budget123")).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> budgetService.updateBudget("budget123", new Budget()));
    }

    @Test
    void testDeleteBudget_Success() {
        // Arrange
        when(budgetRepository.existsById("budget123")).thenReturn(true);

        // Act
        budgetService.deleteBudget("budget123");

        // Assert
        verify(budgetRepository).deleteById("budget123");
    }

    @Test
    void testDeleteBudget_NotFound() {
        // Arrange
        when(budgetRepository.existsById("budget123")).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> budgetService.deleteBudget("budget123"));
    }
}