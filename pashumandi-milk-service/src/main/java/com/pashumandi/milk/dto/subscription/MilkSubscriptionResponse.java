package com.pashumandi.milk.dto.subscription;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pashumandi.milk.dto.order.DeliveryAddressDto;
import com.pashumandi.milk.entity.MilkSubscription;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.SubscriptionFrequency;
import com.pashumandi.milk.enums.SubscriptionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkSubscriptionResponse {

    private Long id;
    private String subscriptionNumber;
    private Long listingId;
    private String listingTitle;
    private Long buyerUserId;
    private Long sellerUserId;
    private BigDecimal quantityLitres;
    private BigDecimal unitPrice;
    private SubscriptionFrequency frequency;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String preferredTimeSlot;
    private FulfillmentType fulfillmentType;
    private DeliveryAddressDto deliveryAddress;
    private SubscriptionStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static MilkSubscriptionResponse fromEntity(MilkSubscription sub) {
        if (sub == null) {
            return null;
        }

        DeliveryAddressDto address = null;
        if (sub.getDeliveryState() != null) {
            address = DeliveryAddressDto.builder()
                    .state(sub.getDeliveryState())
                    .district(sub.getDeliveryDistrict())
                    .village(sub.getDeliveryVillage())
                    .pincode(sub.getDeliveryPincode())
                    .deliveryNotes(sub.getDeliveryNotes())
                    .build();
        }

        return MilkSubscriptionResponse.builder()
                .id(sub.getId())
                .subscriptionNumber(sub.getSubscriptionNumber())
                .listingId(sub.getListing() != null ? sub.getListing().getId() : null)
                .listingTitle(sub.getListing() != null ? sub.getListing().getTitle() : null)
                .buyerUserId(sub.getBuyerUserId())
                .sellerUserId(sub.getSellerUserId())
                .quantityLitres(sub.getQuantityLitres())
                .unitPrice(sub.getUnitPrice())
                .frequency(sub.getFrequency())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .preferredTimeSlot(sub.getPreferredTimeSlot())
                .fulfillmentType(sub.getFulfillmentType())
                .deliveryAddress(address)
                .status(sub.getStatus())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }
}
