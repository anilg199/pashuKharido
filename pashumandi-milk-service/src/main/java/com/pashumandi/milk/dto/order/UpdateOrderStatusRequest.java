package com.pashumandi.milk.dto.order;

import com.pashumandi.milk.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotNull(message = "Order status is required")
    @Schema(description = "Target order status", example = "ACCEPTED")
    private OrderStatus status;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    @Schema(description = "Optional reason for rejection or cancellation", example = "Out of delivery service hours")
    private String reason;
}
