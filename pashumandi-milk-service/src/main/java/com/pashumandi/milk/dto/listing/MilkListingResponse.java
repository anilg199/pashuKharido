package com.pashumandi.milk.dto.listing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.MilkType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkListingResponse {

    private Long id;
    private Long sellerUserId;
    private String title;
    private String description;
    private MilkType milkType;
    private String animalSource;
    private BigDecimal pricePerLitre;
    private BigDecimal availableQuantityLitres;
    private String unit;
    private BigDecimal fatPercentage;
    private BigDecimal snfPercentage;
    private String qualityDescription;
    private String state;
    private String district;
    private String village;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private FulfillmentType fulfillmentType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime availabilityStart;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime availabilityEnd;

    private ListingStatus status;
    private String imageUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static MilkListingResponse fromEntity(MilkListing listing) {
        if (listing == null) {
            return null;
        }
        return MilkListingResponse.builder()
                .id(listing.getId())
                .sellerUserId(listing.getSellerUserId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .milkType(listing.getMilkType())
                .animalSource(listing.getAnimalSource())
                .pricePerLitre(listing.getPricePerLitre())
                .availableQuantityLitres(listing.getAvailableQuantityLitres())
                .unit(listing.getUnit())
                .fatPercentage(listing.getFatPercentage())
                .snfPercentage(listing.getSnfPercentage())
                .qualityDescription(listing.getQualityDescription())
                .state(listing.getState())
                .district(listing.getDistrict())
                .village(listing.getVillage())
                .pincode(listing.getPincode())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .fulfillmentType(listing.getFulfillmentType())
                .availabilityStart(listing.getAvailabilityStart())
                .availabilityEnd(listing.getAvailabilityEnd())
                .status(listing.getStatus())
                .imageUrl(listing.getImageUrl())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
}
