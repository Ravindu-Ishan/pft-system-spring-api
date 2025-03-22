package org.spring.pftsystem.controllers;

import org.spring.pftsystem.entity.request.ReportRequest;
import org.spring.pftsystem.entity.schema.main.Report;
import org.spring.pftsystem.services.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PreAuthorize("hasRole('user')")
    @GetMapping("/generate")
    public ResponseEntity<Report> generateReport(@RequestBody ReportRequest request) throws ParseException {
        Report response = reportService.generateReport(request);
        return ResponseEntity.ok(response);
    }
}