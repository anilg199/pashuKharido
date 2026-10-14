package com.pashumandi.milk.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pashumandi.milk.entity.MilkOrder;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.OrderStatus;
import com.pashumandi.milk.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkOrderResponse {

    private Long id;
    private String orderNumber;
    private Long listingId;
    private String listingTitle;
    private Long buyerUserId;
    private Long sellerUserId;
    private BigDecimal quantityLitres;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private FulfillmentType fulfillmentType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledFor;

    private DeliveryAddressDto deliveryAddress;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private String cancellationReason;
    private String cancelledBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static MilkOrderResponse fromEntity(MilkOrder order) {
        if (order == null) {
            return null;
        }

        DeliveryAddressDto address = null;
        if (order.getDeliveryState() != null) {
            address = DeliveryAddressDto.builder()
                    .state(order.getDeliveryState())
                    .district(order.getDeliveryDistrict())
                    .village(order.getDeliveryVillage())
                    .pincode(order.getDeliveryPincode())
                    .deliveryNotes(order.getDeliveryNotes())
                    .build();
        }

        return MilkOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .listingId(order.getListing() != null ? order.getListing().getId() : null)
                .listingTitle(order.getListing() != null ? order.getListing().getTitle() : null)
                .buyerUserId(order.getBuyerUserId())
                .sellerUserId(order.getSellerUserId())
                .quantityLitres(order.getQuantityLitres())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
                .fulfillmentType(order.getFulfillmentType())
                .scheduledFor(order.getScheduledFor())
                .deliveryAddress(address)
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .cancellationReason(order.getCancellationReason())
                .cancelledBy(order.getCancelledBy())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
