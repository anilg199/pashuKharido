package com.pashumandi.dto.animal;

import com.pashumandi.enums.AnimalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for updating the lifecycle status of an animal listing")
public class AnimalStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target listing status (e.g., PUBLISHED, SOLD, INACTIVE, DRAFT)", example = "SOLD")
    private AnimalStatus status;
}
