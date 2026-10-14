package com.pashumandi.milk.service.impl;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.favorite.MilkFavoriteResponse;
import com.pashumandi.milk.entity.MilkFavorite;
import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.exception.DuplicateResourceException;
import com.pashumandi.milk.exception.ResourceNotFoundException;
import com.pashumandi.milk.repository.MilkFavoriteRepository;
import com.pashumandi.milk.repository.MilkListingRepository;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilkFavoriteServiceImpl implements MilkFavoriteService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MilkFavoriteRepository milkFavoriteRepository;
    private final MilkListingRepository milkListingRepository;

    @Override
    @Transactional
    public MilkFavoriteResponse addFavorite(Long listingId, AuthenticatedUser currentUser) {
        Long userId = currentUser.getUserId();

        if (milkFavoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            throw new DuplicateResourceException("Listing already added to favorites");
        }

        MilkListing listing = milkListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + listingId));

        MilkFavorite favorite = MilkFavorite.builder()
                .userId(userId)
                .listing(listing)
                .build();

        MilkFavorite saved = milkFavoriteRepository.save(favorite);
        log.info("User {} favorited listing {}", userId, listingId);
        return MilkFavoriteResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void removeFavorite(Long listingId, AuthenticatedUser currentUser) {
        Long userId = currentUser.getUserId();
        milkFavoriteRepository.deleteByUserIdAndListingId(userId, listingId);
        log.info("User {} removed favorite for listing {}", userId, listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkFavoriteResponse> getMyFavorites(Pageable pageable, AuthenticatedUser currentUser) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageSize, sort);

        Page<MilkFavorite> page = milkFavoriteRepository.findByUserId(currentUser.getUserId(), safePageable);
        return PagedResponse.from(page, MilkFavoriteResponse::fromEntity);
    }
}
