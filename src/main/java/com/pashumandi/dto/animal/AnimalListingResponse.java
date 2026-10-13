package com.pashumandi.dto.animal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pashumandi.dto.image.AnimalImageResponse;
import com.pashumandi.entity.Animal;
import com.pashumandi.enums.AnimalStatus;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimalListingResponse {

    private Long id;
    private Long sellerId;
    private String title;
    private String category;
    private AnimalStatus status;
    private String primaryImageUrl;
    private Integer imageCount;
    private List<AnimalImageResponse> images;

    public static AnimalListingResponse fromEntity(Animal animal) {
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

        return AnimalListingResponse.builder()
                .id(animal.getId())
                .sellerId(animal.getUser() != null ? animal.getUser().getId() : null)
                .title(animal.getTitle())
                .category(animal.getCategory())
                .status(animal.getStatus())
                .primaryImageUrl(thumbnail)
                .imageCount(imageResponses.size())
                .images(imageResponses)
                .build();
    }
}
