package org.spring.pftsystem.entity.schema.main;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "Goals")
public class Goal {
    @Id
    private String id;

    private String userID;

    @NotBlank(message = "GoalName cannot be blank")
    private String goalName;

    @Min(value = 0, message = "AmountRequired must be at least 0")
    private double amountRequired;

    @Min(value = 0, message = "MonthlyCommitment must be at least 0")
    private double monthlyCommitment;

    @NotNull(message = "EnableAutoCollect cannot be null")
    private boolean enableAutoCollect;

    @Min(value = 1, message = "CollectionDayOfMonth must be at least 1")
    @Max(value = 31, message = "CollectionDayOfMonth must be at most 31")
    private int collectionDayOfMonth;

    private boolean notify = true;
}