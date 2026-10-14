package com.pashumandi.dto.animal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pashumandi.entity.Animal;
import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.enums.AnimalType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimalSummaryResponse {

    private Long id;
    private Long sellerId;
    private String sellerName;
    private String title;
    private AnimalType animalType;
    private String breed;
    private BigDecimal price;
    private Boolean negotiable;
    private AnimalStatus status;
    private String primaryImageUrl;
    private AnimalGender gender;
    private Integer ageMonths;
    private BigDecimal milkYieldLitresPerDay;
    private String state;
    private String district;
    private Double distanceKm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static AnimalSummaryResponse fromEntity(Animal animal) {
        return fromEntity(animal, null);
    }

    public static AnimalSummaryResponse fromEntity(Animal animal, Double distanceKm) {
        if (animal == null) {
            return null;
        }

        String sellerName = null;
        Long sellerId = null;
        if (animal.getUser() != null) {
            sellerId = animal.getUser().getId();
            String first = animal.getUser().getFirstName() != null ? animal.getUser().getFirstName() : "";
            String last = animal.getUser().getLastName() != null ? animal.getUser().getLastName() : "";
            sellerName = (first + " " + last).trim();
            if (sellerName.isEmpty()) {
                sellerName = "Seller #" + sellerId;
            }
        }

        return AnimalSummaryResponse.builder()
                .id(animal.getId())
                .sellerId(sellerId)
                .sellerName(sellerName)
                .title(animal.getTitle())
                .animalType(animal.getAnimalType())
                .breed(animal.getBreed())
                .price(animal.getPrice())
                .negotiable(animal.getNegotiable())
                .status(animal.getStatus())
                .primaryImageUrl(animal.getPrimaryImageUrl())
                .gender(animal.getGender())
                .ageMonths(animal.getAgeMonths())
                .milkYieldLitresPerDay(animal.getMilkYieldLitresPerDay())
                .state(animal.getState())
                .district(animal.getDistrict())
                .distanceKm(distanceKm != null ? Math.round(distanceKm * 100.0) / 100.0 : null)
                .createdAt(animal.getCreatedAt())
                .build();
    }
}
