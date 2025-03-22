package org.spring.pftsystem.controllers;

import org.spring.pftsystem.entity.response.DashboardAdmin;
import org.spring.pftsystem.entity.response.DashboardUser;
import org.spring.pftsystem.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("hasRole('user')")
    @GetMapping("/user")
    public ResponseEntity<DashboardUser> userDashBoard() {
        DashboardUser response = dashboardService.getDashboardUserData();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('administrator')")
    @GetMapping("/admin")
    public ResponseEntity<DashboardAdmin> adminDashBoard() {
        DashboardAdmin response = dashboardService.getAdminDashboardData();
        return ResponseEntity.ok(response);
    }
}
