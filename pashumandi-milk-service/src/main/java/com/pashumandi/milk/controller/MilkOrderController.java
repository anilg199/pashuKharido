package com.pashumandi.milk.controller;

import com.pashumandi.milk.dto.common.ApiResponse;
import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.order.CancelOrderRequest;
import com.pashumandi.milk.dto.order.CreateMilkOrderRequest;
import com.pashumandi.milk.dto.order.MilkOrderResponse;
import com.pashumandi.milk.dto.order.UpdateOrderStatusRequest;
import com.pashumandi.milk.enums.OrderStatus;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/milk/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Milk Orders", description = "One-time milk order placement, lifecycle management, and cancellation APIs")
public class MilkOrderController {

    private final MilkOrderService milkOrderService;

    @PostMapping
    @Operation(summary = "Place a one-time milk order", description = "Places an order, verifies and reserves inventory under concurrency lock, snapshotting current pricing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or delivery mismatch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Insufficient stock available")
    })
    public ResponseEntity<ApiResponse<MilkOrderResponse>> placeOrder(
            @Valid @RequestBody CreateMilkOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkOrderResponse response = milkOrderService.placeOrder(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get buyer's purchase history", description = "Returns orders placed by the authenticated buyer.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkOrderResponse>>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkOrderResponse> response = milkOrderService.getBuyerOrders(status, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-sales")
    @Operation(summary = "Get seller's sales orders", description = "Returns orders received by the authenticated seller.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkOrderResponse>>> getMySales(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkOrderResponse> response = milkOrderService.getSellerSales(status, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details", description = "Accessible only by the buyer, seller, or authorized administrator.")
    public ResponseEntity<ApiResponse<MilkOrderResponse>> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkOrderResponse response = milkOrderService.getOrderById(orderId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Enforces controlled lifecycle (e.g. ACCEPTED, OUT_FOR_DELIVERY, DELIVERED). Rejection or cancellation automatically restores stock.")
    public ResponseEntity<ApiResponse<MilkOrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkOrderResponse response = milkOrderService.updateOrderStatus(orderId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels a pending or accepted order and safely restores listing stock.")
    public ResponseEntity<ApiResponse<MilkOrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkOrderResponse response = milkOrderService.cancelOrder(orderId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled and inventory restored successfully", response));
    }
}
