package org.spring.pftsystem.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.spring.pftsystem.validations.ValidCurrency;

@Data
public class CurrencyRateRequest {
    @NotBlank(message = "Base currency is required")
    @NotNull(message = "Base currency is required")
    @ValidCurrency(message = "Base currency is invalid")
    private String baseCurrency;

    @NotBlank(message = "Target currency is required")
    @NotNull(message = "Target currency is required")
    @ValidCurrency(message = "Target currency is invalid")
    private String targetCurrency;

    @NotBlank(message = "Amount is required")
    @NotNull(message = "Amount is required")
    @Pattern(regexp = "^[0-9]+(\\.[0-9]{1,2})?$", message = "Amount must be a valid number with up to two decimal places")
    private double amount;

}
