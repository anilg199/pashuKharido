package com.pashumandi.milk.repository;

import com.pashumandi.milk.entity.MilkSubscription;
import com.pashumandi.milk.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MilkSubscriptionRepository extends JpaRepository<MilkSubscription, Long> {

    Optional<MilkSubscription> findBySubscriptionNumber(String subscriptionNumber);

    Page<MilkSubscription> findByBuyerUserId(Long buyerUserId, Pageable pageable);

    Page<MilkSubscription> findBySellerUserId(Long sellerUserId, Pageable pageable);

    List<MilkSubscription> findByStatusAndStartDateLessThanEqual(SubscriptionStatus status, LocalDate date);
}
