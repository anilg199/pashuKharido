package com.pashumandi.dto.animal;

import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for updating an existing animal listing")
public class AnimalUpdateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    @Schema(description = "Listing title", example = "Healthy Gir Cow (High Yield)")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Detailed listing description", example = "Healthy 4-year-old Gir cow with calm temperament and high milk yield.")
    private String description;

    @NotNull(message = "Animal type is required")
    @Schema(description = "Category of animal", example = "COW")
    private AnimalType animalType;

    @Size(max = 100, message = "Breed must not exceed 100 characters")
    @Schema(description = "Breed of animal", example = "Gir")
    private String breed;

    @Min(value = 0, message = "Age in months must be zero or positive")
    @Max(value = 600, message = "Age in months must not exceed 600")
    @Schema(description = "Age in months", example = "48")
    private Integer ageMonths;

    @NotNull(message = "Gender is required")
    @Schema(description = "Gender of animal", example = "FEMALE")
    private AnimalGender gender;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Price format must not exceed 10 integer digits and 2 decimal places")
    @Schema(description = "Listing price in INR", example = "65000.00")
    private BigDecimal price;

    @Schema(description = "Whether the price is negotiable", example = "true")
    private Boolean negotiable;

    // Optional livestock fields
    @DecimalMin(value = "0.01", message = "Weight must be greater than zero")
    @Digits(integer = 6, fraction = 2, message = "Weight format must not exceed 6 integer digits and 2 decimal places")
    @Schema(description = "Weight in kilograms", example = "380.00")
    private BigDecimal weightKg;

    @DecimalMin(value = "0.0", message = "Milk yield must be non-negative")
    @Digits(integer = 4, fraction = 2, message = "Milk yield format must not exceed 4 integer digits and 2 decimal places")
    @Schema(description = "Average milk yield in litres per day", example = "10.50")
    private BigDecimal milkYieldLitresPerDay;

    @Size(max = 50, message = "Lactation status must not exceed 50 characters")
    @Schema(description = "Lactation status or count", example = "2nd Lactation")
    private String lactationStatus;

    @Schema(description = "Whether the animal is pregnant", example = "false")
    private Boolean pregnant;

    @Schema(description = "Whether the animal is vaccinated", example = "true")
    private Boolean vaccinated;

    @Size(max = 500, message = "Health description must not exceed 500 characters")
    @Schema(description = "Health details, medical history, or vaccination records", example = "Regularly dewormed and FMD vaccinated.")
    private String healthDescription;

    // Location fields
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State where the animal is located", example = "Uttar Pradesh")
    private String state;

    @Size(max = 100, message = "District must not exceed 100 characters")
    @Schema(description = "District where the animal is located", example = "Kushinagar")
    private String district;

    @Size(max = 100, message = "Village must not exceed 100 characters")
    @Schema(description = "Village or town where the animal is located", example = "Fazilnagar")
    private String village;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "PIN code must be a valid 6-digit Indian PIN code")
    @Schema(description = "6-digit postal PIN code", example = "274401")
    private String pincode;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Geographic latitude coordinate", example = "26.8500000")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Geographic longitude coordinate", example = "83.9800000")
    private BigDecimal longitude;
}
