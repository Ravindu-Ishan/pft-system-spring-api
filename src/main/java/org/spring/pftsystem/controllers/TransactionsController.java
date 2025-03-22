package org.spring.pftsystem.controllers;

import jakarta.validation.Valid;
import lombok.extern.java.Log;
import org.spring.pftsystem.entity.response.GenericResponse;
import org.spring.pftsystem.entity.schema.main.Transaction;
import org.spring.pftsystem.services.TransactionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Log
@RequestMapping("/api/transactions")
public class TransactionsController {

    private final TransactionsService transactionsService;

    public TransactionsController(TransactionsService transactionsService) {
        this.transactionsService = transactionsService;
    }

    @PreAuthorize("hasRole('user')")
    @PostMapping()
    public ResponseEntity<Transaction> transactionCreate(@Valid @RequestBody Transaction transaction) {
        Transaction newTransaction = transactionsService.createTransaction(transaction);
        return ResponseEntity.ok().body(newTransaction);
    }

    @PreAuthorize("hasRole('administrator')")
    @GetMapping()
    public ResponseEntity<List<Transaction>> transactionsGetAll() {
        List <Transaction> transactionList =  transactionsService.getAllTransactions();
        return ResponseEntity.ok().body(transactionList);
    }

    @PreAuthorize("hasRole('user') || hasRole('administrator')")
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> transactionsGetByID(@PathVariable String id) {
        Transaction transaction = transactionsService.getTransactionById(id);
        return ResponseEntity.ok().body(transaction);
    }

    @PreAuthorize("hasRole('user')")
    @GetMapping("/user")
    public ResponseEntity<List<Transaction>> transactionsGetOfUser() {
        List <Transaction> transactionList =  transactionsService.getAllTransactionsOfUser();
        return ResponseEntity.ok().body(transactionList);
    }

    @PreAuthorize("hasRole('administrator')")
    @GetMapping("/user/{uid}")
    public ResponseEntity<List<Transaction>> transactionsGetByUserID(@PathVariable String uid) {
        List <Transaction> transactionList =  transactionsService.getTransactionByUserId(uid);
        return ResponseEntity.ok().body(transactionList);
    }

    @PreAuthorize("hasRole('user')")
    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable String id, @Valid @RequestBody Transaction transaction) {
        Transaction updatedTransaction = transactionsService.updateTransaction(id, transaction);
        return ResponseEntity.ok().body(updatedTransaction);
    }

    @PreAuthorize("hasRole('user')")
    @DeleteMapping({"/{id}"})
    public ResponseEntity<GenericResponse> transactionsDelete(@PathVariable String id) {
        GenericResponse response = new GenericResponse(200,transactionsService.deleteTransaction(id));
        return ResponseEntity.ok().body(response);
    }


}
