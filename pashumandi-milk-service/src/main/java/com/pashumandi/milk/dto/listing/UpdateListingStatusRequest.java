package com.pashumandi.milk.dto.listing;

import com.pashumandi.milk.enums.ListingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingStatusRequest {

    @NotNull(message = "Listing status is required")
    @Schema(description = "Target status: PUBLISHED, PAUSED, or INACTIVE", example = "PUBLISHED")
    private ListingStatus status;
}
