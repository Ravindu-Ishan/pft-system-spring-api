package org.spring.pftsystem.exception;

import lombok.Getter;

@Getter
public class AppIllegalArgument extends IllegalArgumentException{

    private final int statusCode;

    public AppIllegalArgument(String message, int statusCode) {
        super(message); // Pass the message to the parent class
        this.statusCode = statusCode; // Custom status code
    }

}
