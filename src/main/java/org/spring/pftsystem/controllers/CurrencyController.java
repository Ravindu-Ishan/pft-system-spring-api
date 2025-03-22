package org.spring.pftsystem.controllers;

import org.spring.pftsystem.services.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    @Autowired
    private CurrencyService currencyService;

    @PreAuthorize("hasRole('user') || hasRole('administrator')")
    @GetMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertCurrency(@RequestParam String from, @RequestParam String to, @RequestParam double amount) {
        Map<String, Object> response = currencyService.convertCurrency(from, to, amount);
        return ResponseEntity.ok(response);
    }
}

