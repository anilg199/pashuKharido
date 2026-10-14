package com.pashumandi.milk.entity;

import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.MilkType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "milk_listings", indexes = {
        @Index(name = "idx_milk_listings_seller", columnList = "seller_user_id"),
        @Index(name = "idx_milk_listings_status", columnList = "status"),
        @Index(name = "idx_milk_listings_type", columnList = "milk_type"),
        @Index(name = "idx_milk_listings_price", columnList = "price_per_litre"),
        @Index(name = "idx_milk_listings_location", columnList = "state, district, pincode"),
        @Index(name = "idx_milk_listings_fulfillment", columnList = "fulfillment_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_user_id", nullable = false)
    private Long sellerUserId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "milk_type", nullable = false, length = 20)
    private MilkType milkType;

    @Column(name = "animal_source", length = 100)
    private String animalSource;

    @Column(name = "price_per_litre", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerLitre;

    @Column(name = "available_quantity_litres", nullable = false, precision = 8, scale = 2)
    private BigDecimal availableQuantityLitres;

    @Builder.Default
    @Column(name = "unit", nullable = false, length = 20)
    private String unit = "LITRE";

    @Column(name = "fat_percentage", precision = 4, scale = 2)
    private BigDecimal fatPercentage;

    @Column(name = "snf_percentage", precision = 4, scale = 2)
    private BigDecimal snfPercentage;

    @Column(name = "quality_description", length = 500)
    private String qualityDescription;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "village", nullable = false, length = 100)
    private String village;

    @Column(name = "pincode", nullable = false, length = 10)
    private String pincode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "fulfillment_type", nullable = false, length = 20)
    private FulfillmentType fulfillmentType;

    @Column(name = "availability_start")
    private LocalTime availabilityStart;

    @Column(name = "availability_end")
    private LocalTime availabilityEnd;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ListingStatus.DRAFT;
        }
        if (this.unit == null) {
            this.unit = "LITRE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
