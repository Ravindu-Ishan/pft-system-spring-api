package org.spring.pftsystem.entity.schema.sub;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryLimit {
    @NotBlank(message = "Category cannot be blank")
    private String category;

    @Min(value = 0, message = "Limit amount must be at least 0")
    private int limitAmount;

    @Min(value = 0, message = "Current expenditure must be at least 0")
    private int currentExpenditure;

    private boolean warning;
}
