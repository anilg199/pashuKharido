package com.pashumandi.milk.dto.favorite;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pashumandi.milk.dto.listing.MilkListingResponse;
import com.pashumandi.milk.entity.MilkFavorite;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkFavoriteResponse {

    private Long id;
    private Long userId;
    private Long listingId;
    private MilkListingResponse listing;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static MilkFavoriteResponse fromEntity(MilkFavorite fav) {
        if (fav == null) {
            return null;
        }
        return MilkFavoriteResponse.builder()
                .id(fav.getId())
                .userId(fav.getUserId())
                .listingId(fav.getListing() != null ? fav.getListing().getId() : null)
                .listing(fav.getListing() != null ? MilkListingResponse.fromEntity(fav.getListing()) : null)
                .createdAt(fav.getCreatedAt())
                .build();
    }
}
