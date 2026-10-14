package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.favorite.MilkFavoriteResponse;
import com.pashumandi.milk.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

public interface MilkFavoriteService {

    MilkFavoriteResponse addFavorite(Long listingId, AuthenticatedUser currentUser);

    void removeFavorite(Long listingId, AuthenticatedUser currentUser);

    PagedResponse<MilkFavoriteResponse> getMyFavorites(Pageable pageable, AuthenticatedUser currentUser);
}
