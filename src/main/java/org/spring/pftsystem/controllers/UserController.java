package org.spring.pftsystem.controllers;

import org.spring.pftsystem.entity.response.UserDetails;
import org.spring.pftsystem.entity.schema.sub.UserSettings;
import org.spring.pftsystem.services.UserDetailsService;
import org.spring.pftsystem.services.UserSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('user') || hasRole('administrator')")
public class UserController {

    UserSettingsService userSettings;
    UserDetailsService userDetailsService;

    UserController(UserSettingsService userSettings, UserDetailsService userDetailsService) {
        this.userSettings = userSettings;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/settings/get")
    public ResponseEntity<UserSettings> settingsUpdate() {
        UserSettings settings = userSettings.getUserSettings();
        return ResponseEntity.ok().body(settings);
    }

    @PutMapping("/settings/update")
    public ResponseEntity<UserSettings> settingsUpdate(@RequestBody UserSettings updateReq) {
        UserSettings updatedSettings = userSettings.updateSettings(updateReq);
        return ResponseEntity.ok().body(updatedSettings);

    }

    @GetMapping("/details/get")
    public ResponseEntity<UserDetails> getUserDetails() {
        UserDetails details  = userDetailsService.getUserDetails();
        return ResponseEntity.ok().body(details);

    }

    @PutMapping("/details/update")
    public ResponseEntity<UserDetails> userDetailsUpdate(@RequestBody UserDetails updateReq) {
        UserDetails updatedDetails = userDetailsService.updateUserDetails(updateReq);
        return ResponseEntity.ok().body(updatedDetails);

    }


}
