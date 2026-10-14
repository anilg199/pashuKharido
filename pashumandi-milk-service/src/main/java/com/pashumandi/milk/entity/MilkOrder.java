package com.pashumandi.milk.entity;

import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.OrderStatus;
import com.pashumandi.milk.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "milk_orders", indexes = {
        @Index(name = "idx_milk_orders_buyer", columnList = "buyer_user_id"),
        @Index(name = "idx_milk_orders_seller", columnList = "seller_user_id"),
        @Index(name = "idx_milk_orders_status", columnList = "order_status"),
        @Index(name = "idx_milk_orders_listing", columnList = "listing_id"),
        @Index(name = "idx_milk_orders_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 36)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private MilkListing listing;

    @Column(name = "buyer_user_id", nullable = false)
    private Long buyerUserId;

    @Column(name = "seller_user_id", nullable = false)
    private Long sellerUserId;

    @Column(name = "quantity_litres", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantityLitres;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "fulfillment_type", nullable = false, length = 20)
    private FulfillmentType fulfillmentType;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "delivery_state", length = 100)
    private String deliveryState;

    @Column(name = "delivery_district", length = 100)
    private String deliveryDistrict;

    @Column(name = "delivery_village", length = 100)
    private String deliveryVillage;

    @Column(name = "delivery_pincode", length = 10)
    private String deliveryPincode;

    @Column(name = "delivery_notes", length = 500)
    private String deliveryNotes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.orderStatus == null) {
            this.orderStatus = OrderStatus.PENDING;
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
