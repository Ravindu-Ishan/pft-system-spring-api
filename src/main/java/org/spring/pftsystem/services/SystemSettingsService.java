package org.spring.pftsystem.services;

import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.spring.pftsystem.repository.SystemSettingsRepo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class SystemSettingsService {

    private final SystemSettingsRepo systemSettingsRepo;

    public SystemSettingsService(SystemSettingsRepo systemSettingsRepo) {
        this.systemSettingsRepo = systemSettingsRepo;
    }

    public SystemSettings getSystemSettings(){
        SystemSettings settings = systemSettingsRepo.findFirstByOrderByIdAsc();
        if (settings == null) {
            // Initialize with default values
            settings = createDefaultSettings();
        }
        return settings;
    }

    public SystemSettings updateSystemSettings(SystemSettings settings) {
        return systemSettingsRepo.save(settings);
    }

    private SystemSettings createDefaultSettings() {
        SystemSettings defaultSettings = new SystemSettings(
                "default",  // ID
                1000,       // TotalTransactionsLimit
                100,        // RecurringTransactionsLimit
                new ArrayList<>(), // Categories
                86400       // JWTExpirationTime (24 hours in seconds)
        );
        return systemSettingsRepo.save(defaultSettings);
    }
}
