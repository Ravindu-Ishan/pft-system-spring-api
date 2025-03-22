package org.spring.pftsystem.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ValidationErrors {
    private int status;
    private List<String> errors;
}
