package com.pashumandi.dto.animal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pashumandi.dto.image.AnimalImageResponse;
import com.pashumandi.dto.user.UserSummaryResponse;
import com.pashumandi.entity.Animal;
import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.enums.AnimalType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimalResponse {

    private Long id;
    private UserSummaryResponse seller;
    private String title;
    private String description;
    private AnimalType animalType;
    private String category;
    private String breed;
    private Integer ageMonths;
    private AnimalGender gender;
    private BigDecimal price;
    private Boolean negotiable;
    private AnimalStatus status;
    private String primaryImageUrl;

    // Optional livestock fields
    private BigDecimal weightKg;
    private BigDecimal milkYieldLitresPerDay;
    private String lactationStatus;
    private Boolean pregnant;
    private Boolean vaccinated;
    private String healthDescription;

    // Location fields
    private String state;
    private String district;
    private String village;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distanceKm;

    private Integer imageCount;
    private List<AnimalImageResponse> images;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static AnimalResponse fromEntity(Animal animal) {
        return fromEntity(animal, null);
    }

    public static AnimalResponse fromEntity(Animal animal, Double distanceKm) {
        if (animal == null) {
            return null;
        }

        List<AnimalImageResponse> imageResponses = animal.getImages() != null
                ? animal.getImages().stream().map(AnimalImageResponse::fromEntity).collect(Collectors.toList())
                : Collections.emptyList();

        String thumbnail = animal.getPrimaryImageUrl();
        if (thumbnail == null && !imageResponses.isEmpty()) {
            thumbnail = imageResponses.get(0).getImageUrl();
        }

        return AnimalResponse.builder()
                .id(animal.getId())
                .seller(UserSummaryResponse.fromEntity(animal.getUser()))
                .title(animal.getTitle())
                .description(animal.getDescription())
                .animalType(animal.getAnimalType())
                .category(animal.getCategory())
                .breed(animal.getBreed())
                .ageMonths(animal.getAgeMonths())
                .gender(animal.getGender())
                .price(animal.getPrice())
                .negotiable(animal.getNegotiable())
                .status(animal.getStatus())
                .primaryImageUrl(thumbnail)
                .weightKg(animal.getWeightKg())
                .milkYieldLitresPerDay(animal.getMilkYieldLitresPerDay())
                .lactationStatus(animal.getLactationStatus())
                .pregnant(animal.getPregnant())
                .vaccinated(animal.getVaccinated())
                .healthDescription(animal.getHealthDescription())
                .state(animal.getState())
                .district(animal.getDistrict())
                .village(animal.getVillage())
                .pincode(animal.getPincode())
                .latitude(animal.getLatitude())
                .longitude(animal.getLongitude())
                .distanceKm(distanceKm != null ? Math.round(distanceKm * 100.0) / 100.0 : null)
                .imageCount(imageResponses.size())
                .images(imageResponses)
                .createdAt(animal.getCreatedAt())
                .updatedAt(animal.getUpdatedAt())
                .build();
    }
}
