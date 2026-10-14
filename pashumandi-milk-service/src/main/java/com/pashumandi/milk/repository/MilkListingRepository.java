package com.pashumandi.milk.repository;

import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.enums.ListingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface MilkListingRepository extends JpaRepository<MilkListing, Long>, JpaSpecificationExecutor<MilkListing> {

    Page<MilkListing> findByStatusAndAvailableQuantityLitresGreaterThan(ListingStatus status, BigDecimal minQuantity, Pageable pageable);

    Page<MilkListing> findBySellerUserId(Long sellerUserId, Pageable pageable);

    Page<MilkListing> findBySellerUserIdAndStatus(Long sellerUserId, ListingStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM MilkListing l WHERE l.id = :id")
    Optional<MilkListing> findByIdForUpdate(@Param("id") Long id);
}
