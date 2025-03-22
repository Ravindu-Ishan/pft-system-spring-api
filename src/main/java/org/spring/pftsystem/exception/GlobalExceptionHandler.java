package org.spring.pftsystem.exception;

import io.lettuce.core.RedisException;
import lombok.extern.java.Log;
import org.spring.pftsystem.constants.Constants;
import org.spring.pftsystem.entity.response.ErrorResponse;
import org.spring.pftsystem.entity.response.ValidationErrors;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.security.auth.login.CredentialNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;


@Log
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppIllegalArgument.class)
    public ResponseEntity<Object> AppIllegalArgumentException(AppIllegalArgument ex) {
        log.warning(Constants.EXCEPTION_ALERT + ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ex.getStatusCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> illegalArgumentException(IllegalArgumentException ex) {
        log.warning(Constants.EXCEPTION_ALERT + ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(400, ex.getMessage());
        return ResponseEntity.status(400).body(errorResponse);
    }


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> userNotFoundException(UserNotFoundException ex) {
        log.warning(Constants.EXCEPTION_ALERT + ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(500, ex.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }

    @ExceptionHandler(CredentialNotFoundException.class)
    public ResponseEntity<Object> credentialNotFoundException(CredentialNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(401 , "Invalid Credentials");
        return ResponseEntity.status(401).body(errorResponse);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Object> handleSecurityException(SecurityException ex) {
        log.warning(Constants.EXCEPTION_ALERT + ex.getMessage());
        return ResponseEntity.status(403).body(Constants.EXCEPTION_ALERT + ex.getMessage());
    }

    // Handle generic exceptions (fallback for unexpected errors)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        log.warning(Constants.EXCEPTION_ALERT + ex.getMessage());
       /* // Print the full stack trace to the log
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        log.warning(sw.toString());  // print the stack trace*/
        ErrorResponse errorResponse = new ErrorResponse(500, ex.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }

    @ExceptionHandler(RedisException.class)
    public ResponseEntity<Object> handleRedisException(RedisException ex) {
        log.warning(Constants.EXCEPTION_ALERT + ex.getMessage());
        /*// Print the full stack trace to the log
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        log.warning(sw.toString());  // print the stack trace*/
        ErrorResponse errorResponse = new ErrorResponse(500, Constants.SERVER_ERROR);
        return ResponseEntity.status(500).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        ValidationErrors errorResponse = new ValidationErrors(400, errors);
        log.warning(Constants.EXCEPTION_ALERT + errors);
        return ResponseEntity.status(400).body(errorResponse);

    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex) {
        log.warning(Constants.EXCEPTION_ALERT + ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(404, ex.getMessage());
        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(DatabaseOperationException.class)
    public ResponseEntity<Object> handleDatabaseOperationException(DatabaseOperationException ex) {
        log.severe(Constants.EXCEPTION_ALERT + ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(500, ex.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }


}
