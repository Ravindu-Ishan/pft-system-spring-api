package org.spring.pftsystem.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenericResponse{
    private int statusCode;
    private String message;
}