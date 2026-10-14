package com.pashumandi.milk.dto.inquiry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInquiryRequest {

    @NotBlank(message = "Inquiry message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    @Schema(description = "Inquiry message or question for the seller", example = "Is daily morning delivery available to Ramkola Ward 4?")
    private String message;

    @Size(max = 15, message = "Phone must not exceed 15 digits")
    @Schema(description = "Optional contact phone number", example = "9876543210")
    private String phone;
}
