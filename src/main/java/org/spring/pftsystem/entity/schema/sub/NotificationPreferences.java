package org.spring.pftsystem.entity.schema.sub;

import lombok.Data;


@Data
public class NotificationPreferences{
    private boolean budgetWarnings;

    public NotificationPreferences(){
        budgetWarnings = true;
    }
}
