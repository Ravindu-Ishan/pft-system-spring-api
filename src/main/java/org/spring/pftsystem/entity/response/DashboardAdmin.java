package org.spring.pftsystem.entity.response;

import lombok.Data;

@Data
public class DashboardAdmin {
    private String username;
    private long totalTransactionsToDate;
    private long totalTransactionsThisMonth;
    private int totalUsers;
    private long systemUsage;
}
