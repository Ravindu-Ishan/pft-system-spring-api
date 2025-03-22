package org.spring.pftsystem.entity.schema.main;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.spring.pftsystem.entity.schema.sub.RecurrenceDetails;
import org.spring.pftsystem.validations.ValidCategory;
import org.spring.pftsystem.validations.ValidCurrency;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "Transactions")
public class Transaction {

    @Id
    private String id;
    private String userId;

    @NotBlank(message = "Description is mandatory")
    @Pattern(regexp = "^(Expense|Income|Savings)$", message = "Type must be either 'Expense','Income' or 'Savings'")
    private String type; //Expense, Income

    @NotBlank(message = "Category is required")
    @ValidCategory
    private String category;

    private List<String> tags;

    @NotBlank(message = "beneficiary is required")
    @Size(max = 50, message = "Description too long (max 50 characters)")
    @Pattern(regexp = "^[^<>]*$", message = "Beneficiary cannot contain special characters")
    private String beneficiary;

    @NotBlank(message = "Sender description is required")
    @Size(max = 50, message = "Description too long (max 50 characters)")
    @Pattern(regexp = "^[^<>]*$", message = "Beneficiary cannot contain special characters")
    private String senderDescription; // User, Admin

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    @ValidCurrency
    private String currency;

    @NotNull
    private Boolean isRecurring;

    private RecurrenceDetails recurrence;

    @NotNull
    private boolean notify;

    private String transactionDate = java.time.LocalDateTime.now().toString();
    private String lastUpdatedAt = java.time.LocalDateTime.now().toString();
}