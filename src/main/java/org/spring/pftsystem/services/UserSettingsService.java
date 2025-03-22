package org.spring.pftsystem.services;

import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.entity.schema.sub.UserSettings;
import org.spring.pftsystem.exception.DatabaseOperationException;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {

    private final UserRepository userRepository;

    public UserSettingsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserSettings getUserSettings(){
        User user = UserUtil.getUserFromContext(userRepository);
        return user.getSettings();
    }

    public UserSettings updateSettings (UserSettings userSettings ) {

        User user = UserUtil.getUserFromContext(userRepository);
        user.setSettings(userSettings);

        try{
            userRepository.save(user);
            return userSettings;
        }catch (Exception e){
            throw new DatabaseOperationException(e.getMessage());
        }

    }

}
