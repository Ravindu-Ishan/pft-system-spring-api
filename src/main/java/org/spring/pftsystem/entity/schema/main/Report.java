package org.spring.pftsystem.entity.schema.main;
import lombok.Data;
import org.spring.pftsystem.entity.schema.sub.Filters;
import org.spring.pftsystem.entity.schema.sub.Summary;
import org.spring.pftsystem.entity.schema.sub.TimePeriod;
import org.spring.pftsystem.entity.schema.sub.FilteredTransaction;

import java.util.List;

@Data
public class Report {

    private String reportType;
    private TimePeriod timePeriod;
    private Filters filters;
    private Summary summary;
    private List<FilteredTransaction> transactions;
}