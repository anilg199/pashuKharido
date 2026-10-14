package com.pashumandi.milk.service.impl;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.inquiry.CreateInquiryRequest;
import com.pashumandi.milk.dto.inquiry.MilkInquiryResponse;
import com.pashumandi.milk.entity.MilkInquiry;
import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.ResourceNotFoundException;
import com.pashumandi.milk.repository.MilkInquiryRepository;
import com.pashumandi.milk.repository.MilkListingRepository;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkInquiryService;
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
public class MilkInquiryServiceImpl implements MilkInquiryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MilkInquiryRepository milkInquiryRepository;
    private final MilkListingRepository milkListingRepository;

    @Override
    @Transactional
    public MilkInquiryResponse submitInquiry(Long listingId, CreateInquiryRequest request, AuthenticatedUser currentUser) {
        Long senderId = currentUser.getUserId();

        MilkListing listing = milkListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + listingId));

        if (senderId.equals(listing.getSellerUserId())) {
            throw new BadRequestException("Cannot submit inquiries on your own listing");
        }

        MilkInquiry inquiry = MilkInquiry.builder()
                .listing(listing)
                .senderUserId(senderId)
                .sellerUserId(listing.getSellerUserId())
                .message(request.getMessage().trim())
                .phone(request.getPhone())
                .build();

        MilkInquiry saved = milkInquiryRepository.save(inquiry);
        log.info("Inquiry submitted: ID={}, from user {} to seller {}", saved.getId(), senderId, listing.getSellerUserId());
        return MilkInquiryResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkInquiryResponse> getMyInquiries(Pageable pageable, AuthenticatedUser currentUser) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageSize, sort);

        Page<MilkInquiry> page = milkInquiryRepository.findBySenderUserId(currentUser.getUserId(), safePageable);
        return PagedResponse.from(page, MilkInquiryResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkInquiryResponse> getReceivedInquiries(Pageable pageable, AuthenticatedUser currentUser) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageSize, sort);

        Page<MilkInquiry> page = milkInquiryRepository.findBySellerUserId(currentUser.getUserId(), safePageable);
        return PagedResponse.from(page, MilkInquiryResponse::fromEntity);
    }
}
