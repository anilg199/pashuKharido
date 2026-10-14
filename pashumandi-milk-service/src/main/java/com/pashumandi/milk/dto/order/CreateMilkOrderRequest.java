package com.pashumandi.milk.dto.order;

import com.pashumandi.milk.enums.FulfillmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMilkOrderRequest {

    @NotNull(message = "Listing ID is required")
    @Schema(description = "ID of the milk listing to purchase", example = "101")
    private Long listingId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.10", message = "Minimum order quantity is 0.10 litre")
    @Digits(integer = 6, fraction = 2, message = "Quantity must have at most 6 digits and 2 decimals")
    @Schema(description = "Quantity to order in litres", example = "2.00")
    private BigDecimal quantityLitres;

    @NotNull(message = "Fulfillment type is required")
    @Schema(description = "Order fulfillment method: DELIVERY or PICKUP", example = "DELIVERY")
    private FulfillmentType fulfillmentType;

    @Valid
    @Schema(description = "Delivery address (required when fulfillmentType is DELIVERY)")
    private DeliveryAddressDto deliveryAddress;

    @Schema(description = "Requested delivery or pickup schedule (ISO 8601)", example = "2026-11-01T07:00:00")
    private LocalDateTime scheduledFor;
}
