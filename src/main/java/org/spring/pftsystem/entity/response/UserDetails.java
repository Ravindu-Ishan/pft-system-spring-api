package org.spring.pftsystem.entity.response;

import lombok.Data;

@Data
public class UserDetails {
        private String firstName;
        private String lastName;
        private String email;
        private String timeStamp = java.time.LocalDateTime.now().toString();
}

