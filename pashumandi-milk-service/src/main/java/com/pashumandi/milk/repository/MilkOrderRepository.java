package com.pashumandi.milk.repository;

import com.pashumandi.milk.entity.MilkOrder;
import com.pashumandi.milk.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MilkOrderRepository extends JpaRepository<MilkOrder, Long> {

    Optional<MilkOrder> findByOrderNumber(String orderNumber);

    Page<MilkOrder> findByBuyerUserId(Long buyerUserId, Pageable pageable);

    Page<MilkOrder> findByBuyerUserIdAndOrderStatus(Long buyerUserId, OrderStatus status, Pageable pageable);

    Page<MilkOrder> findBySellerUserId(Long sellerUserId, Pageable pageable);

    Page<MilkOrder> findBySellerUserIdAndOrderStatus(Long sellerUserId, OrderStatus status, Pageable pageable);
}
