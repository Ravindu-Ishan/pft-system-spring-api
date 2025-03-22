package org.spring.pftsystem.controllers;

import org.spring.pftsystem.entity.schema.main.Budget;
import org.spring.pftsystem.services.BudgetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PreAuthorize("hasRole('user')")
    @PostMapping()
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody Budget budget) {
        // Create a budget for the user, allowing only one budget per user
        Budget createdBudget = budgetService.createBudget(budget);
        return ResponseEntity.ok(createdBudget);
    }

    @PreAuthorize("hasRole('administrator')")
    @GetMapping()
    public ResponseEntity<List<Budget>> getAllBudgets() {
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    @PreAuthorize("hasRole('user') || hasRole('administrator')")
    @GetMapping("/user")
    public ResponseEntity<Budget> getBudgetForUser() {
        Budget budget = budgetService.getBudgetForUser();
        return ResponseEntity.ok(budget);
    }

    @PreAuthorize("hasRole('administrator')")
    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudgetById(@PathVariable String id) {
        Optional<Budget> budget = budgetService.getBudgetById(id);
        return budget.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('user')")
    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable String id, @Valid @RequestBody Budget updatedBudget) {
        // Update the user's existing budget
        return ResponseEntity.ok(budgetService.updateBudget(id, updatedBudget));
    }

    @PreAuthorize("hasRole('user')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable String id) {
        // Allow the user to delete their own budget
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
