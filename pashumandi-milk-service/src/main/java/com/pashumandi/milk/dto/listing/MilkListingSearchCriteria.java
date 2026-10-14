package com.pashumandi.milk.dto.listing;

import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.MilkType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkListingSearchCriteria {

    @Schema(description = "Filter by milk type (COW, BUFFALO, GOAT, OTHER)")
    private MilkType milkType;

    @Schema(description = "Minimum price per litre")
    private BigDecimal minPrice;

    @Schema(description = "Maximum price per litre")
    private BigDecimal maxPrice;

    @Schema(description = "Minimum available quantity in litres")
    private BigDecimal minQuantity;

    @Schema(description = "Maximum available quantity in litres")
    private BigDecimal maxQuantity;

    @Schema(description = "State name (case-insensitive partial match)")
    private String state;

    @Schema(description = "District name (case-insensitive partial match)")
    private String district;

    @Schema(description = "6-digit postal pincode")
    private String pincode;

    @Schema(description = "Fulfillment type (DELIVERY, PICKUP, BOTH)")
    private FulfillmentType fulfillmentType;

    @Schema(description = "Minimum fat percentage")
    private BigDecimal minFatPercentage;

    @Schema(description = "Maximum fat percentage")
    private BigDecimal maxFatPercentage;
}
