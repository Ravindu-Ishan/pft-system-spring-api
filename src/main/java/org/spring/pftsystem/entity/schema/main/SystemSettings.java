package org.spring.pftsystem.entity.schema.main;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@Document(collection = "system_settings")
public class SystemSettings {
    @Id
    private String id;

    private int TotalTransactionsLimit;
    private int RecurringTransactionsLimit;
    private List<String> Categories;
    private int JWTExpirationTime;

}
