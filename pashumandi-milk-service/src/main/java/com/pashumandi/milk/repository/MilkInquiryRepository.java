package com.pashumandi.milk.repository;

import com.pashumandi.milk.entity.MilkInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MilkInquiryRepository extends JpaRepository<MilkInquiry, Long> {

    Page<MilkInquiry> findBySenderUserId(Long senderUserId, Pageable pageable);

    Page<MilkInquiry> findBySellerUserId(Long sellerUserId, Pageable pageable);
}
