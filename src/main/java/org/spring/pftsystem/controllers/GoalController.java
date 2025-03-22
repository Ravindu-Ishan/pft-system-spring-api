package org.spring.pftsystem.controllers;

import org.spring.pftsystem.entity.schema.main.Goal;
import org.spring.pftsystem.services.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PreAuthorize("hasRole('user')")
    @PostMapping()
    public ResponseEntity<Goal> createGoal(@Valid @RequestBody Goal goal) {
        Goal createdGoal = goalService.createGoal(goal);
        return ResponseEntity.ok(createdGoal);
    }

    @PreAuthorize("hasRole('administrator')")
    @GetMapping()
    public ResponseEntity<List<Goal>> getAllGoals() {
        return ResponseEntity.ok(goalService.getAllGoals());
    }

    @PreAuthorize("hasRole('user')")
    @GetMapping("/user")
    public ResponseEntity<List<Goal>> getGoalsOfUser() {
        return ResponseEntity.ok(goalService.getGoalsOfUser());
    }

    @PreAuthorize("hasRole('administrator')")
    @GetMapping("/user/{userID}")
    public ResponseEntity<List<Goal>> getGoalsByUserID(@PathVariable String userID) {
        return ResponseEntity.ok(goalService.getGoalsByUserID(userID));
    }

    @PreAuthorize("hasRole('user') || hasRole('administrator')")
    @GetMapping("/{id}")
    public ResponseEntity<Goal> getGoalById(@PathVariable String id) {
        Goal goal = goalService.getGoalById(id);
        return ResponseEntity.ok(goal);
    }

    @PreAuthorize("hasRole('user')")
    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(@PathVariable String id, @Valid @RequestBody Goal updatedGoal) {
        return ResponseEntity.ok(goalService.updateGoal(id, updatedGoal));
    }

    @PreAuthorize("hasRole('user')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable String id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}