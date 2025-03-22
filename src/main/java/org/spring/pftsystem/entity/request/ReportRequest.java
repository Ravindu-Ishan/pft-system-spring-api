package org.spring.pftsystem.entity.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.spring.pftsystem.entity.schema.sub.Filters;
import org.spring.pftsystem.entity.schema.sub.TimePeriod;

@Data
public class ReportRequest {
    @NotNull(message = "Report type cannot be null")
    @NotEmpty(message = "Report type cannot be empty")
    @Pattern(regexp = "income|expenditure|cashFlow", message = "Report type must be either 'income', 'expenditure', or 'cashFlow'")
    private String reportType;

    @NotNull(message = "Time period cannot be null")
    private TimePeriod timePeriod;

    private Filters filters;

}
