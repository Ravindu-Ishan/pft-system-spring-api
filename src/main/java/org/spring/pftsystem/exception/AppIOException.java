package org.spring.pftsystem.exception;

import lombok.Getter;

import java.io.IOException;

@Getter
public class AppIOException extends IOException {

    private final int statusCode;

    public AppIOException(String message, int statusCode) {
        super(message); // Pass the message to the parent class
        this.statusCode = statusCode; // Custom status code
    }


}
