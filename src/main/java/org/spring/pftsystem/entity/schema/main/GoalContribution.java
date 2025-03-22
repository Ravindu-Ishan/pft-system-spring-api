package org.spring.pftsystem.entity.schema.main;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "GoalContributions")
public class GoalContribution {
    @Id
    private String id;

    private String goalId;
    private String userId;
    private double amount;
    private String contributionDate = java.time.LocalDateTime.now().toString();
}
