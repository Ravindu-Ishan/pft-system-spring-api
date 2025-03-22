package org.spring.pftsystem.entity.schema.sub;

import lombok.Data;


@Data
public class UserSettings {
    private String currency;
    private NotificationPreferences notificationPreferences;

    public UserSettings(){
        currency = "LKR";
        notificationPreferences = new NotificationPreferences();
    }
}