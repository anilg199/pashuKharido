package com.pashumandi.milk.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressDto {

    @NotBlank(message = "Delivery state is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "Delivery state", example = "Uttar Pradesh")
    private String state;

    @NotBlank(message = "Delivery district is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    @Schema(description = "Delivery district", example = "Kushinagar")
    private String district;

    @NotBlank(message = "Delivery village or locality is required")
    @Size(max = 100, message = "Village must not exceed 100 characters")
    @Schema(description = "Delivery village, town, or street address", example = "Ramkola Ward 4")
    private String village;

    @NotBlank(message = "Delivery pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be a valid 6-digit Indian postal code")
    @Schema(description = "Delivery pincode", example = "274401")
    private String pincode;

    @Size(max = 500, message = "Delivery notes must not exceed 500 characters")
    @Schema(description = "Special instructions for delivery partner", example = "Near primary school, house with blue gate")
    private String deliveryNotes;
}
