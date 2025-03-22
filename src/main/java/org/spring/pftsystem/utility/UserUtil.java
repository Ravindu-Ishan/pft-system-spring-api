package org.spring.pftsystem.utility;

import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.exception.UserNotFoundException;
import org.spring.pftsystem.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public class UserUtil {

    public static User getUserFromContext(UserRepository userRepository) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Get user details
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Access user information
        String id = userDetails.getUsername(); // UserID from context

        Optional<User> user = userRepository.findById(id);

        if (user.isPresent()) {
            return user.get();
        }
        else{
            throw new UserNotFoundException();
        }

    }
}
