package org.spring.pftsystem.controllers;

import lombok.extern.java.Log;
import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.spring.pftsystem.services.SystemSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Log
@RequestMapping("/api/system")
@PreAuthorize("hasRole('administrator')")
public class SystemController {

    private final SystemSettingsService systemSettings;

    public SystemController (SystemSettingsService systemSettings) {
        this.systemSettings = systemSettings;
    }

    @GetMapping("/settings")
    public ResponseEntity<SystemSettings> systemSettingsGet() {
        SystemSettings settings = systemSettings.getSystemSettings(); ;
        return ResponseEntity.ok().body(settings);
    }

    @PutMapping("/settings")
    public ResponseEntity<SystemSettings> systemSettingsUpdate(@RequestBody SystemSettings updateReq) {
        SystemSettings updatedSettings = systemSettings.updateSystemSettings(updateReq);
        return ResponseEntity.ok().body(updatedSettings);
    }

}
