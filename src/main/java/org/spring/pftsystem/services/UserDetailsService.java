package org.spring.pftsystem.services;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.response.UserDetails;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.exception.DatabaseOperationException;
import org.spring.pftsystem.exception.UserNotFoundException;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.stereotype.Service;

@Log
@Service
public class UserDetailsService {

    private final UserRepository userRepository;

    private UserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public UserDetails getUserDetails(){

        User user = UserUtil.getUserFromContext(userRepository);
        UserDetails userDetails = new UserDetails();

        //filter details and assign to userDetails
        userDetails.setEmail(user.getEmail());
        userDetails.setFirstName(user.getFirstName());
        userDetails.setLastName(user.getLastName());

        return userDetails;
    }

    public UserDetails updateUserDetails (UserDetails userDetails) {

        User user = UserUtil.getUserFromContext(userRepository);
        // is user setting the same email :
        String userDetailsEmail = userDetails.getEmail();
        if(userDetailsEmail == null || userDetailsEmail.isEmpty()){
            throw new IllegalArgumentException("Email cannot be empty");
        }

        // check for email change
        User originalUser = userRepository.findById(user.getId()).orElseThrow(() -> new UserNotFoundException());
        if(!originalUser.getEmail().equals(userDetailsEmail)){
            log.info("Changing email");
            User existing = userRepository.findByEmail(userDetails.getEmail()).orElse(null);
            if(existing != null){
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(userDetails.getEmail());
        }
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setTimeStamp(null);

        try{
            userRepository.save(user);
            return getUserDetails(); // use method to get saved user details
        }catch (Exception e){
            throw new DatabaseOperationException(e.getMessage());
        }

    }
}
