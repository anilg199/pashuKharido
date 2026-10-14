package com.pashumandi.dto.image;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pashumandi.entity.AnimalImage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimalImageResponse {

    private Long id;
    private Long animalId;
    private String imageUrl;
    private Integer displayOrder;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static AnimalImageResponse fromEntity(AnimalImage image) {
        if (image == null) {
            return null;
        }
        return AnimalImageResponse.builder()
                .id(image.getId())
                .animalId(image.getAnimal() != null ? image.getAnimal().getId() : null)
                .imageUrl(image.getImageUrl())
                .displayOrder(image.getDisplayOrder())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
