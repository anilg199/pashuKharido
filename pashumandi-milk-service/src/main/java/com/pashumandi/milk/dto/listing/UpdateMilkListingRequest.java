package com.pashumandi.milk.dto.listing;

import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.MilkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMilkListingRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    @Schema(description = "Listing headline", example = "Pure Desi Gir Cow A2 Raw Milk")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Detailed listing description", example = "Fresh organic A2 cow milk delivered daily morning.")
    private String description;

    @NotNull(message = "Milk type is required")
    @Schema(description = "Milk type", example = "COW")
    private MilkType milkType;

    @Size(max = 100, message = "Animal source must not exceed 100 characters")
    @Schema(description = "Animal breed or source description", example = "Gir Cow")
    private String animalSource;

    @NotNull(message = "Price per litre is required")
    @DecimalMin(value = "0.01", message = "Price per litre must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimals")
    @Schema(description = "Price per litre in INR", example = "75.00")
    private BigDecimal pricePerLitre;

    @NotNull(message = "Available quantity is required")
    @DecimalMin(value = "0.00", message = "Available quantity cannot be negative")
    @Digits(integer = 6, fraction = 2, message = "Quantity must have at most 6 digits and 2 decimals")
    @Schema(description = "Available daily or total quantity in litres", example = "50.00")
    private BigDecimal availableQuantityLitres;

    @Builder.Default
    @Schema(description = "Unit of measurement", example = "LITRE")
    private String unit = "LITRE";

    @DecimalMin(value = "0.0", message = "Fat percentage must be non-negative")
    @DecimalMax(value = "15.0", message = "Fat percentage must not exceed 15.0%")
    @Schema(description = "Fat percentage", example = "4.5")
    private BigDecimal fatPercentage;

    @DecimalMin(value = "0.0", message = "SNF percentage must be non-negative")
    @DecimalMax(value = "15.0", message = "SNF percentage must not exceed 15.0%")
    @Schema(description = "Solid-Not-Fat (SNF) percentage", example = "8.5")
    private BigDecimal snfPercentage;

    @Size(max = 500, message = "Quality description must not exceed 500 characters")
    @Schema(description = "Quality, testing, or hygiene details", example = "Tested daily with lactometer; unpasteurized fresh milk.")
    private String qualityDescription;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State", example = "Uttar Pradesh")
    private String state;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    @Schema(description = "District", example = "Kushinagar")
    private String district;

    @NotBlank(message = "Village is required")
    @Size(max = 100, message = "Village must not exceed 100 characters")
    @Schema(description = "Village or locality", example = "Ramkola")
    private String village;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be a valid 6-digit Indian postal code")
    @Schema(description = "Postal code", example = "274401")
    private String pincode;

    @Schema(description = "Latitude coordinate", example = "26.9015")
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate", example = "83.8921")
    private BigDecimal longitude;

    @NotNull(message = "Fulfillment type is required")
    @Schema(description = "Fulfillment method: DELIVERY, PICKUP, or BOTH", example = "BOTH")
    private FulfillmentType fulfillmentType;

    @Schema(description = "Daily availability start time", example = "06:00:00")
    private LocalTime availabilityStart;

    @Schema(description = "Daily availability end time", example = "10:00:00")
    private LocalTime availabilityEnd;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Schema(description = "Listing picture URL", example = "https://res.cloudinary.com/pashumandi/image/upload/milk1.jpg")
    private String imageUrl;
}
