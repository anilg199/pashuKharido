package com.pashumandi.milk.service.impl;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.order.CancelOrderRequest;
import com.pashumandi.milk.dto.order.CreateMilkOrderRequest;
import com.pashumandi.milk.dto.order.DeliveryAddressDto;
import com.pashumandi.milk.dto.order.MilkOrderResponse;
import com.pashumandi.milk.dto.order.UpdateOrderStatusRequest;
import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.entity.MilkOrder;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.OrderStatus;
import com.pashumandi.milk.enums.PaymentStatus;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.InsufficientStockException;
import com.pashumandi.milk.exception.InvalidOrderStateTransitionException;
import com.pashumandi.milk.exception.ResourceNotFoundException;
import com.pashumandi.milk.exception.UnauthorizedAccessException;
import com.pashumandi.milk.repository.MilkListingRepository;
import com.pashumandi.milk.repository.MilkOrderRepository;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilkOrderServiceImpl implements MilkOrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MilkOrderRepository milkOrderRepository;
    private final MilkListingRepository milkListingRepository;

    @Override
    @Transactional
    public MilkOrderResponse placeOrder(CreateMilkOrderRequest request, AuthenticatedUser currentUser) {
        Long buyerId = currentUser.getUserId();
        log.info("Processing order placement: buyer={}, listing={}, quantity={}", buyerId, request.getListingId(), request.getQuantityLitres());

        // Concurrency Control: Acquire Pessimistic Write Lock on the MilkListing row
        MilkListing listing = milkListingRepository.findByIdForUpdate(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + request.getListingId()));

        // 1. Availability validation
        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new BadRequestException("Listing is not currently available for orders. Current status: " + listing.getStatus());
        }

        // 2. Prevent self-purchase
        if (buyerId.equals(listing.getSellerUserId())) {
            throw new BadRequestException("Sellers cannot purchase milk from their own listings");
        }

        // 3. Concurrency-safe Stock Check
        BigDecimal requestedQty = request.getQuantityLitres();
        if (requestedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Order quantity must be positive");
        }

        if (listing.getAvailableQuantityLitres().compareTo(requestedQty) < 0) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock: requested %s L, but only %s L available",
                    requestedQty, listing.getAvailableQuantityLitres()
            ));
        }

        // 4. Fulfillment Type Validation
        if (request.getFulfillmentType() == FulfillmentType.DELIVERY) {
            if (listing.getFulfillmentType() == FulfillmentType.PICKUP) {
                throw new BadRequestException("This seller only provides pickup; delivery is not available");
            }
            if (request.getDeliveryAddress() == null) {
                throw new BadRequestException("Delivery address is mandatory when choosing delivery fulfillment");
            }
        } else if (request.getFulfillmentType() == FulfillmentType.PICKUP) {
            if (listing.getFulfillmentType() == FulfillmentType.DELIVERY) {
                throw new BadRequestException("This seller only provides delivery; pickup is not available");
            }
        }

        // 5. Server-side price calculation (snapshot)
        BigDecimal unitPrice = listing.getPricePerLitre();
        BigDecimal totalPrice = unitPrice.multiply(requestedQty).setScale(2, RoundingMode.HALF_UP);

        // 6. Deduct Inventory safely inside lock
        BigDecimal remainingStock = listing.getAvailableQuantityLitres().subtract(requestedQty);
        listing.setAvailableQuantityLitres(remainingStock);
        if (remainingStock.compareTo(BigDecimal.ZERO) == 0) {
            listing.setStatus(ListingStatus.SOLD_OUT);
            log.info("Listing {} is now SOLD_OUT after fulfilling order", listing.getId());
        }
        milkListingRepository.save(listing);

        // 7. Create Order Record
        DeliveryAddressDto addr = request.getDeliveryAddress();
        String orderNumber = generateOrderNumber();

        MilkOrder order = MilkOrder.builder()
                .orderNumber(orderNumber)
                .listing(listing)
                .buyerUserId(buyerId)
                .sellerUserId(listing.getSellerUserId())
                .quantityLitres(requestedQty)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .fulfillmentType(request.getFulfillmentType())
                .scheduledFor(request.getScheduledFor())
                .deliveryState(addr != null ? addr.getState() : null)
                .deliveryDistrict(addr != null ? addr.getDistrict() : null)
                .deliveryVillage(addr != null ? addr.getVillage() : null)
                .deliveryPincode(addr != null ? addr.getPincode() : null)
                .deliveryNotes(addr != null ? addr.getDeliveryNotes() : null)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        MilkOrder savedOrder = milkOrderRepository.save(order);
        log.info("Order placed successfully. OrderNumber={}, TotalPrice={}", savedOrder.getOrderNumber(), savedOrder.getTotalPrice());

        return MilkOrderResponse.fromEntity(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkOrderResponse> getBuyerOrders(OrderStatus status, Pageable pageable, AuthenticatedUser currentUser) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<MilkOrder> page;

        if (status != null) {
            page = milkOrderRepository.findByBuyerUserIdAndOrderStatus(currentUser.getUserId(), status, safePageable);
        } else {
            page = milkOrderRepository.findByBuyerUserId(currentUser.getUserId(), safePageable);
        }

        return PagedResponse.from(page, MilkOrderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkOrderResponse> getSellerSales(OrderStatus status, Pageable pageable, AuthenticatedUser currentUser) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<MilkOrder> page;

        if (status != null) {
            page = milkOrderRepository.findBySellerUserIdAndOrderStatus(currentUser.getUserId(), status, safePageable);
        } else {
            page = milkOrderRepository.findBySellerUserId(currentUser.getUserId(), safePageable);
        }

        return PagedResponse.from(page, MilkOrderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public MilkOrderResponse getOrderById(Long orderId, AuthenticatedUser currentUser) {
        MilkOrder order = milkOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk order not found with ID: " + orderId));

        validateOrderParticipant(order, currentUser);

        return MilkOrderResponse.fromEntity(order);
    }

    @Override
    @Transactional
    public MilkOrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, AuthenticatedUser currentUser) {
        MilkOrder order = milkOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk order not found with ID: " + orderId));

        validateOrderParticipant(order, currentUser);

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus targetStatus = request.getStatus();

        if (currentStatus == targetStatus) {
            return MilkOrderResponse.fromEntity(order);
        }

        boolean isSeller = currentUser.getUserId().equals(order.getSellerUserId());
        boolean isBuyer = currentUser.getUserId().equals(order.getBuyerUserId());
        boolean isAdmin = currentUser.isAdmin();

        // State Machine validation
        switch (currentStatus) {
            case PENDING:
                if (targetStatus == OrderStatus.ACCEPTED) {
                    if (!isSeller && !isAdmin) {
                        throw new UnauthorizedAccessException("Only the seller or admin can accept orders");
                    }
                    order.setOrderStatus(OrderStatus.ACCEPTED);
                } else if (targetStatus == OrderStatus.REJECTED) {
                    if (!isSeller && !isAdmin) {
                        throw new UnauthorizedAccessException("Only the seller or admin can reject orders");
                    }
                    order.setOrderStatus(OrderStatus.REJECTED);
                    order.setCancellationReason(request.getReason() != null ? request.getReason() : "Rejected by seller");
                    order.setCancelledBy("SELLER");
                    order.setCancelledAt(LocalDateTime.now());
                    restoreStock(order);
                } else if (targetStatus == OrderStatus.CANCELLED) {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    order.setCancellationReason(request.getReason() != null ? request.getReason() : "Cancelled before acceptance");
                    order.setCancelledBy(isBuyer ? "BUYER" : (isSeller ? "SELLER" : "ADMIN"));
                    order.setCancelledAt(LocalDateTime.now());
                    restoreStock(order);
                } else {
                    throw new InvalidOrderStateTransitionException("Cannot transition PENDING order directly to " + targetStatus);
                }
                break;

            case ACCEPTED:
                if (targetStatus == OrderStatus.READY_FOR_PICKUP) {
                    if (!isSeller && !isAdmin) {
                        throw new UnauthorizedAccessException("Only the seller can mark order ready for pickup");
                    }
                    order.setOrderStatus(OrderStatus.READY_FOR_PICKUP);
                } else if (targetStatus == OrderStatus.OUT_FOR_DELIVERY) {
                    if (!isSeller && !isAdmin) {
                        throw new UnauthorizedAccessException("Only the seller can mark order out for delivery");
                    }
                    order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
                } else if (targetStatus == OrderStatus.CANCELLED) {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    order.setCancellationReason(request.getReason() != null ? request.getReason() : "Cancelled after acceptance");
                    order.setCancelledBy(isBuyer ? "BUYER" : (isSeller ? "SELLER" : "ADMIN"));
                    order.setCancelledAt(LocalDateTime.now());
                    restoreStock(order);
                } else {
                    throw new InvalidOrderStateTransitionException("Cannot transition ACCEPTED order to " + targetStatus);
                }
                break;

            case READY_FOR_PICKUP:
            case OUT_FOR_DELIVERY:
                if (targetStatus == OrderStatus.DELIVERED) {
                    order.setOrderStatus(OrderStatus.DELIVERED);
                    order.setPaymentStatus(PaymentStatus.PAID);
                } else {
                    throw new InvalidOrderStateTransitionException("Order in dispatch/ready state cannot transition to " + targetStatus);
                }
                break;

            case DELIVERED:
                if (targetStatus == OrderStatus.COMPLETED) {
                    order.setOrderStatus(OrderStatus.COMPLETED);
                } else {
                    throw new InvalidOrderStateTransitionException("Delivered order can only be transitioned to COMPLETED");
                }
                break;

            case COMPLETED:
            case REJECTED:
            case CANCELLED:
                throw new InvalidOrderStateTransitionException("Order is in terminal state: " + currentStatus + " and cannot be altered");

            default:
                throw new InvalidOrderStateTransitionException("Unsupported order transition");
        }

        MilkOrder updatedOrder = milkOrderRepository.save(order);
        log.info("Order {} transitioned from {} to {}", order.getOrderNumber(), currentStatus, targetStatus);
        return MilkOrderResponse.fromEntity(updatedOrder);
    }

    @Override
    @Transactional
    public MilkOrderResponse cancelOrder(Long orderId, CancelOrderRequest request, AuthenticatedUser currentUser) {
        MilkOrder order = milkOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk order not found with ID: " + orderId));

        validateOrderParticipant(order, currentUser);

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.ACCEPTED) {
            throw new BadRequestException("Order cannot be cancelled in state: " + order.getOrderStatus() + ". Only PENDING or ACCEPTED orders are eligible for cancellation.");
        }

        boolean isBuyer = currentUser.getUserId().equals(order.getBuyerUserId());
        boolean isSeller = currentUser.getUserId().equals(order.getSellerUserId());

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getReason());
        order.setCancelledBy(isBuyer ? "BUYER" : (isSeller ? "SELLER" : "ADMIN"));
        order.setCancelledAt(LocalDateTime.now());

        // Restore inventory safely with lock
        restoreStock(order);

        MilkOrder saved = milkOrderRepository.save(order);
        log.info("Order {} cancelled by {}. Reason: {}", order.getOrderNumber(), order.getCancelledBy(), request.getReason());
        return MilkOrderResponse.fromEntity(saved);
    }

    private void restoreStock(MilkOrder order) {
        MilkListing listing = milkListingRepository.findByIdForUpdate(order.getListing().getId())
                .orElse(null);

        if (listing != null) {
            BigDecimal restoredQty = listing.getAvailableQuantityLitres().add(order.getQuantityLitres());
            listing.setAvailableQuantityLitres(restoredQty);

            // If the listing was SOLD_OUT, restore it to PUBLISHED
            if (listing.getStatus() == ListingStatus.SOLD_OUT && restoredQty.compareTo(BigDecimal.ZERO) > 0) {
                listing.setStatus(ListingStatus.PUBLISHED);
                log.info("Restoring listing {} status to PUBLISHED after order cancellation", listing.getId());
            }
            milkListingRepository.save(listing);
            log.info("Restored {} L to listing {}. New stock: {} L", order.getQuantityLitres(), listing.getId(), restoredQty);
        }
    }

    private void validateOrderParticipant(MilkOrder order, AuthenticatedUser currentUser) {
        boolean isBuyer = currentUser.getUserId().equals(order.getBuyerUserId());
        boolean isSeller = currentUser.getUserId().equals(order.getSellerUserId());
        boolean isAdmin = currentUser.isAdmin();

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new UnauthorizedAccessException("You are not authorized to access order ID: " + order.getId());
        }
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "ORD-" + timestamp + "-" + randomSuffix;
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort();
        Sort safeSort = sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), pageSize, safeSort);
    }
}
