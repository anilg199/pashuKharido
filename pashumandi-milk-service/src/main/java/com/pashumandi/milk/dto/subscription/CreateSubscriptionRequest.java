package com.pashumandi.milk.dto.subscription;

import com.pashumandi.milk.dto.order.DeliveryAddressDto;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.SubscriptionFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionRequest {

    @NotNull(message = "Listing ID is required")
    @Schema(description = "ID of the milk listing for subscription", example = "101")
    private Long listingId;

    @NotNull(message = "Daily/delivery quantity is required")
    @DecimalMin(value = "0.10", message = "Minimum quantity is 0.10 litre")
    @Digits(integer = 6, fraction = 2, message = "Quantity must have at most 6 digits and 2 decimals")
    @Schema(description = "Milk quantity per delivery in litres", example = "1.50")
    private BigDecimal quantityLitres;

    @NotNull(message = "Subscription frequency is required")
    @Schema(description = "Delivery frequency: DAILY, WEEKLY, CUSTOM", example = "DAILY")
    private SubscriptionFrequency frequency;

    @NotNull(message = "Subscription start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    @Schema(description = "First delivery date", example = "2026-11-01")
    private LocalDate startDate;

    @Schema(description = "Optional end date for subscription", example = "2026-11-30")
    private LocalDate endDate;

    @Schema(description = "Preferred delivery slot", example = "MORNING_07_00")
    private String preferredTimeSlot;

    @NotNull(message = "Fulfillment type is required")
    @Schema(description = "Fulfillment type: DELIVERY or PICKUP", example = "DELIVERY")
    private FulfillmentType fulfillmentType;

    @Valid
    @Schema(description = "Delivery address (required when fulfillmentType is DELIVERY)")
    private DeliveryAddressDto deliveryAddress;
}
