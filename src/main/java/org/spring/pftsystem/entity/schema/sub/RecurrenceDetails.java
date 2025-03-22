package org.spring.pftsystem.entity.schema.sub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecurrenceDetails {
    @NotBlank(message = "Pattern cannot be blank")
    @Pattern(regexp = "^(Daily|Weekly|Monthly)$", message = "Pattern must be one of: Daily, Weekly, or Monthly")
    private String pattern; // "Daily", "Weekly", "Monthly"

    @NotBlank(message = "Start date cannot be blank")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Start date must be in the format YYYY-MM-DD")
    private String startDate;

    @NotBlank(message = "End date cannot be blank")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "End date must be in the format YYYY-MM-DD")
    private String endDate;

    @NotEmpty(message = "ExecuteOnDay cannot be empty")
    @Size(min = 1, max = 28, message = "ExecuteOnDay must have a day of the month specified")
    private int executeOnDay;

    @NotBlank(message = "Next execution date cannot be blank")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Next execution date must be in the format YYYY-MM-DD")
    private String nextExecutionDate;


}
