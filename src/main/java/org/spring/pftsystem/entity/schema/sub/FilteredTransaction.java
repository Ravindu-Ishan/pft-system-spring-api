package org.spring.pftsystem.entity.schema.sub;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

@Data
public class FilteredTransaction {
    private String type;

    private String date;
    private double amount;
    private String category;
    private String beneficiary;
    private List<String> tags;
    private String description;
}