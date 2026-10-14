package com.pashumandi.milk.entity;

import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.SubscriptionFrequency;
import com.pashumandi.milk.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "milk_subscriptions", indexes = {
        @Index(name = "idx_milk_sub_buyer", columnList = "buyer_user_id"),
        @Index(name = "idx_milk_sub_seller", columnList = "seller_user_id"),
        @Index(name = "idx_milk_sub_status", columnList = "status"),
        @Index(name = "idx_milk_sub_listing", columnList = "listing_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_number", nullable = false, unique = true, length = 36)
    private String subscriptionNumber;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "frequency", nullable = false, length = 20)
    private SubscriptionFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "preferred_time_slot", length = 50)
    private String preferredTimeSlot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "fulfillment_type", nullable = false, length = 20)
    private FulfillmentType fulfillmentType;

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
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SubscriptionStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
