package com.pashumandi.milk.dto.inquiry;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pashumandi.milk.entity.MilkInquiry;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkInquiryResponse {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private Long senderUserId;
    private Long sellerUserId;
    private String message;
    private String phone;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static MilkInquiryResponse fromEntity(MilkInquiry inquiry) {
        if (inquiry == null) {
            return null;
        }
        return MilkInquiryResponse.builder()
                .id(inquiry.getId())
                .listingId(inquiry.getListing() != null ? inquiry.getListing().getId() : null)
                .listingTitle(inquiry.getListing() != null ? inquiry.getListing().getTitle() : null)
                .senderUserId(inquiry.getSenderUserId())
                .sellerUserId(inquiry.getSellerUserId())
                .message(inquiry.getMessage())
                .phone(inquiry.getPhone())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
