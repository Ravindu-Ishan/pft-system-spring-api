package org.spring.pftsystem.entity.schema.main;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.spring.pftsystem.entity.schema.sub.CategoryLimit;
import org.spring.pftsystem.validations.ValidCurrency;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "Budgets")
public class Budget {
    @Id
    private String id;

    private String userID;

    @Min(value = 0, message = "Monthly limit must be at least 0")
    private float monthlyLimit;

    @Min(value = 0, message = "Current expenditure must be at least 0")
    private float currentExpenditure;

    @ValidCurrency
    private String currency;

    @NotNull(message = "Category limits flag cannot be null")
    private boolean categoryLimitsOn;

    private List<CategoryLimit> categoryLimits;

    private boolean warning;

    private String timestamp = java.time.LocalDateTime.now().toString();
}
