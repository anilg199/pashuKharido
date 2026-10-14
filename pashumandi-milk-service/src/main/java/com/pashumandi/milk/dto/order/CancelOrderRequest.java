package com.pashumandi.milk.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    @Schema(description = "Reason for cancellation", example = "Unable to collect milk today")
    private String reason;
}
