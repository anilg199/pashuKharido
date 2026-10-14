package com.pashumandi.entity;

import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.enums.AnimalType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "animals",
    indexes = {
        @Index(name = "idx_animals_user_id", columnList = "user_id"),
        @Index(name = "idx_animals_status", columnList = "status"),
        @Index(name = "idx_animals_type", columnList = "animal_type"),
        @Index(name = "idx_animals_breed", columnList = "breed"),
        @Index(name = "idx_animals_price", columnList = "price"),
        @Index(name = "idx_animals_state_district", columnList = "state, district"),
        @Index(name = "idx_animals_created_at", columnList = "created_at"),
        @Index(name = "idx_animals_lat_long", columnList = "latitude, longitude")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "animal_type", length = 50)
    private AnimalType animalType;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "age_months")
    private Integer ageMonths;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gender", length = 20)
    private AnimalGender gender;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "negotiable")
    @Builder.Default
    private Boolean negotiable = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private AnimalStatus status;

    @Column(name = "primary_image_url", length = 255)
    private String primaryImageUrl;

    // Optional livestock-specific fields
    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "milk_yield_litres_per_day", precision = 6, scale = 2)
    private BigDecimal milkYieldLitresPerDay;

    @Column(name = "lactation_status", length = 50)
    private String lactationStatus;

    @Column(name = "pregnant")
    private Boolean pregnant;

    @Column(name = "vaccinated")
    private Boolean vaccinated;

    @Column(name = "health_description", length = 500)
    private String healthDescription;

    // Location fields
    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "village", length = 100)
    private String village;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<AnimalImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User getSeller() {
        return this.user;
    }

    public void setSeller(User seller) {
        this.user = seller;
    }

    public static class AnimalBuilder {
        public AnimalBuilder seller(User seller) {
            this.user = seller;
            return this;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AnimalStatus.DRAFT;
        }
        if (this.negotiable == null) {
            this.negotiable = false;
        }
        syncAnimalTypeAndCategory();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncAnimalTypeAndCategory();
    }

    private void syncAnimalTypeAndCategory() {
        if (this.animalType != null && this.category == null) {
            this.category = this.animalType.name();
        } else if (this.category != null && this.animalType == null) {
            try {
                this.animalType = AnimalType.valueOf(this.category.toUpperCase());
            } catch (Exception ignored) {
            }
        }
    }
}
