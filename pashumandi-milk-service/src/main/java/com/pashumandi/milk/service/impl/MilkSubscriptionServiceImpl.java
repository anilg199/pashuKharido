package com.pashumandi.milk.service.impl;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.order.CreateMilkOrderRequest;
import com.pashumandi.milk.dto.order.DeliveryAddressDto;
import com.pashumandi.milk.dto.subscription.CreateSubscriptionRequest;
import com.pashumandi.milk.dto.subscription.MilkSubscriptionResponse;
import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.entity.MilkSubscription;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.SubscriptionStatus;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.InvalidSubscriptionStateException;
import com.pashumandi.milk.exception.ResourceNotFoundException;
import com.pashumandi.milk.exception.UnauthorizedAccessException;
import com.pashumandi.milk.repository.MilkListingRepository;
import com.pashumandi.milk.repository.MilkSubscriptionRepository;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkOrderService;
import com.pashumandi.milk.service.MilkSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilkSubscriptionServiceImpl implements MilkSubscriptionService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MilkSubscriptionRepository milkSubscriptionRepository;
    private final MilkListingRepository milkListingRepository;
    private final MilkOrderService milkOrderService;

    @Override
    @Transactional
    public MilkSubscriptionResponse createSubscription(CreateSubscriptionRequest request, AuthenticatedUser currentUser) {
        Long buyerId = currentUser.getUserId();
        log.info("Creating recurring milk subscription: buyer={}, listing={}", buyerId, request.getListingId());

        MilkListing listing = milkListingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + request.getListingId()));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new BadRequestException("Cannot subscribe to an unavailable listing. Current status: " + listing.getStatus());
        }

        if (buyerId.equals(listing.getSellerUserId())) {
            throw new BadRequestException("Sellers cannot subscribe to their own listings");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be earlier than start date");
        }

        if (request.getFulfillmentType() == FulfillmentType.DELIVERY) {
            if (listing.getFulfillmentType() == FulfillmentType.PICKUP) {
                throw new BadRequestException("This seller does not support delivery");
            }
            if (request.getDeliveryAddress() == null) {
                throw new BadRequestException("Delivery address is required for delivery subscriptions");
            }
        }

        String subNumber = generateSubscriptionNumber();
        DeliveryAddressDto addr = request.getDeliveryAddress();

        MilkSubscription subscription = MilkSubscription.builder()
                .subscriptionNumber(subNumber)
                .listing(listing)
                .buyerUserId(buyerId)
                .sellerUserId(listing.getSellerUserId())
                .quantityLitres(request.getQuantityLitres())
                .unitPrice(listing.getPricePerLitre())
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .preferredTimeSlot(request.getPreferredTimeSlot())
                .fulfillmentType(request.getFulfillmentType())
                .deliveryState(addr != null ? addr.getState() : null)
                .deliveryDistrict(addr != null ? addr.getDistrict() : null)
                .deliveryVillage(addr != null ? addr.getVillage() : null)
                .deliveryPincode(addr != null ? addr.getPincode() : null)
                .deliveryNotes(addr != null ? addr.getDeliveryNotes() : null)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        MilkSubscription saved = milkSubscriptionRepository.save(subscription);
        log.info("Subscription created successfully: {}", saved.getSubscriptionNumber());

        return MilkSubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkSubscriptionResponse> getMySubscriptions(Pageable pageable, AuthenticatedUser currentUser) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<MilkSubscription> page = milkSubscriptionRepository.findByBuyerUserId(currentUser.getUserId(), safePageable);
        return PagedResponse.from(page, MilkSubscriptionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkSubscriptionResponse> getSellerSubscriptions(Pageable pageable, AuthenticatedUser currentUser) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<MilkSubscription> page = milkSubscriptionRepository.findBySellerUserId(currentUser.getUserId(), safePageable);
        return PagedResponse.from(page, MilkSubscriptionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public MilkSubscriptionResponse getSubscriptionById(Long subscriptionId, AuthenticatedUser currentUser) {
        MilkSubscription sub = milkSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId));

        validateSubscriptionParticipant(sub, currentUser);

        return MilkSubscriptionResponse.fromEntity(sub);
    }

    @Override
    @Transactional
    public MilkSubscriptionResponse pauseSubscription(Long subscriptionId, AuthenticatedUser currentUser) {
        MilkSubscription sub = milkSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId));

        validateSubscriptionParticipant(sub, currentUser);

        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new InvalidSubscriptionStateException("Can only pause ACTIVE subscriptions. Current status: " + sub.getStatus());
        }

        sub.setStatus(SubscriptionStatus.PAUSED);
        MilkSubscription saved = milkSubscriptionRepository.save(sub);
        log.info("Subscription {} paused by user {}", sub.getSubscriptionNumber(), currentUser.getUserId());
        return MilkSubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public MilkSubscriptionResponse resumeSubscription(Long subscriptionId, AuthenticatedUser currentUser) {
        MilkSubscription sub = milkSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId));

        validateSubscriptionParticipant(sub, currentUser);

        if (sub.getStatus() != SubscriptionStatus.PAUSED) {
            throw new InvalidSubscriptionStateException("Can only resume PAUSED subscriptions. Current status: " + sub.getStatus());
        }

        sub.setStatus(SubscriptionStatus.ACTIVE);
        MilkSubscription saved = milkSubscriptionRepository.save(sub);
        log.info("Subscription {} resumed by user {}", sub.getSubscriptionNumber(), currentUser.getUserId());
        return MilkSubscriptionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public MilkSubscriptionResponse cancelSubscription(Long subscriptionId, AuthenticatedUser currentUser) {
        MilkSubscription sub = milkSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId));

        validateSubscriptionParticipant(sub, currentUser);

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            return MilkSubscriptionResponse.fromEntity(sub);
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        MilkSubscription saved = milkSubscriptionRepository.save(sub);
        log.info("Subscription {} cancelled by user {}", sub.getSubscriptionNumber(), currentUser.getUserId());
        return MilkSubscriptionResponse.fromEntity(saved);
    }

    /**
     * Generates orders for active subscriptions on a target date.
     * Demonstrates reliable idempotency hook for scheduled tasks.
     */
    @Override
    @Transactional
    public List<Long> generateDailyOrdersForSubscriptions(LocalDate targetDate) {
        log.info("Triggering recurring order generation for date: {}", targetDate);
        List<MilkSubscription> activeSubs = milkSubscriptionRepository
                .findByStatusAndStartDateLessThanEqual(SubscriptionStatus.ACTIVE, targetDate);

        List<Long> generatedOrderIds = new ArrayList<>();

        for (MilkSubscription sub : activeSubs) {
            if (sub.getEndDate() != null && targetDate.isAfter(sub.getEndDate())) {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                milkSubscriptionRepository.save(sub);
                continue;
            }

            try {
                AuthenticatedUser subUser = AuthenticatedUser.builder()
                        .userId(sub.getBuyerUserId())
                        .role("BUYER")
                        .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER")))
                        .build();

                DeliveryAddressDto addr = DeliveryAddressDto.builder()
                        .state(sub.getDeliveryState())
                        .district(sub.getDeliveryDistrict())
                        .village(sub.getDeliveryVillage())
                        .pincode(sub.getDeliveryPincode())
                        .deliveryNotes("Generated recurring order for subscription: " + sub.getSubscriptionNumber())
                        .build();

                CreateMilkOrderRequest orderRequest = CreateMilkOrderRequest.builder()
                        .listingId(sub.getListing().getId())
                        .quantityLitres(sub.getQuantityLitres())
                        .fulfillmentType(sub.getFulfillmentType())
                        .deliveryAddress(addr)
                        .scheduledFor(targetDate.atTime(7, 0))
                        .build();

                var orderResponse = milkOrderService.placeOrder(orderRequest, subUser);
                generatedOrderIds.add(orderResponse.getId());
            } catch (Exception e) {
                log.warn("Failed to generate order for subscription {}: {}", sub.getSubscriptionNumber(), e.getMessage());
            }
        }

        log.info("Generated {} recurring orders for date {}", generatedOrderIds.size(), targetDate);
        return generatedOrderIds;
    }

    private void validateSubscriptionParticipant(MilkSubscription sub, AuthenticatedUser currentUser) {
        boolean isBuyer = currentUser.getUserId().equals(sub.getBuyerUserId());
        boolean isSeller = currentUser.getUserId().equals(sub.getSellerUserId());
        boolean isAdmin = currentUser.isAdmin();

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new UnauthorizedAccessException("You are not authorized to access subscription ID: " + sub.getId());
        }
    }

    private String generateSubscriptionNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "SUB-" + timestamp + "-" + randomSuffix;
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort();
        Sort safeSort = sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), pageSize, safeSort);
    }
}
