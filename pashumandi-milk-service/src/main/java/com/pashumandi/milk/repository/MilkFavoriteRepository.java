package com.pashumandi.milk.repository;

import com.pashumandi.milk.entity.MilkFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MilkFavoriteRepository extends JpaRepository<MilkFavorite, Long> {

    Optional<MilkFavorite> findByUserIdAndListingId(Long userId, Long listingId);

    Page<MilkFavorite> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    void deleteByUserIdAndListingId(Long userId, Long listingId);
}
