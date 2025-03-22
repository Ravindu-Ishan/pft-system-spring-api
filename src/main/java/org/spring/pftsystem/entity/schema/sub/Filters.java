package org.spring.pftsystem.entity.schema.sub;

import lombok.Data;

import java.util.List;

@Data
public class Filters {
    private List<String> categories;
    private List<String> tags;
}
