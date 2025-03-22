package org.spring.pftsystem.entity.schema.main;
import lombok.*;
import org.spring.pftsystem.entity.schema.sub.UserSettings;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "User")
public class User {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String password; // Stored as a hashed password
    private String role; // User, Admin
    private UserSettings settings = new UserSettings();
    private String timeStamp = java.time.LocalDateTime.now().toString();
}

