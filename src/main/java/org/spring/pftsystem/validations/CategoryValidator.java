package org.spring.pftsystem.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.spring.pftsystem.repository.SystemSettingsRepo;
import org.springframework.beans.factory.annotation.Autowired;

public class CategoryValidator implements ConstraintValidator<ValidCategory, String> {

    @Autowired
    private SystemSettingsRepo systemSettingsRepo;

    @Override
    public boolean isValid(String category, ConstraintValidatorContext context) {
        if (category == null || systemSettingsRepo == null) {
            return false;
        }

        boolean isValid = systemSettingsRepo.findAll().get(0).getCategories().contains(category);
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid Category. Available categories are: " + String.join(", ", systemSettingsRepo.findAll().get(0).getCategories())
            ).addConstraintViolation();
        }

        return isValid;
    }
}