package org.spring.pftsystem.exception;
import static org.spring.pftsystem.constants.Constants.USER_NOT_FOUND;

public class UserNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = USER_NOT_FOUND;
    public UserNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
