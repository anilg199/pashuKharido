package com.pashumandi.milk.controller;

import com.pashumandi.milk.dto.common.ApiResponse;
import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.subscription.CreateSubscriptionRequest;
import com.pashumandi.milk.dto.subscription.MilkSubscriptionResponse;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/milk/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Milk Subscriptions", description = "Recurring milk delivery subscription management APIs")
public class MilkSubscriptionController {

    private final MilkSubscriptionService milkSubscriptionService;

    @PostMapping
    @Operation(summary = "Create recurring milk subscription", description = "Creates a daily or weekly recurring subscription for fresh milk delivery.")
    public ResponseEntity<ApiResponse<MilkSubscriptionResponse>> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkSubscriptionResponse response = milkSubscriptionService.createSubscription(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription created successfully", response));
    }

    @GetMapping("/my-subscriptions")
    @Operation(summary = "Get buyer's subscriptions", description = "Returns active and historical subscriptions for authenticated buyer.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkSubscriptionResponse>>> getMySubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkSubscriptionResponse> response = milkSubscriptionService.getMySubscriptions(pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/seller-subscriptions")
    @Operation(summary = "Get seller's subscriptions", description = "Returns subscriber orders serviced by the authenticated seller.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkSubscriptionResponse>>> getSellerSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkSubscriptionResponse> response = milkSubscriptionService.getSellerSubscriptions(pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Get subscription details", description = "Accessible by buyer, seller, or administrator.")
    public ResponseEntity<ApiResponse<MilkSubscriptionResponse>> getSubscriptionById(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkSubscriptionResponse response = milkSubscriptionService.getSubscriptionById(subscriptionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{subscriptionId}/pause")
    @Operation(summary = "Pause a subscription", description = "Temporarily halts recurring milk delivery.")
    public ResponseEntity<ApiResponse<MilkSubscriptionResponse>> pauseSubscription(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkSubscriptionResponse response = milkSubscriptionService.pauseSubscription(subscriptionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Subscription paused successfully", response));
    }

    @PatchMapping("/{subscriptionId}/resume")
    @Operation(summary = "Resume a paused subscription", description = "Resumes recurring milk delivery.")
    public ResponseEntity<ApiResponse<MilkSubscriptionResponse>> resumeSubscription(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkSubscriptionResponse response = milkSubscriptionService.resumeSubscription(subscriptionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Subscription resumed successfully", response));
    }

    @PatchMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancel a subscription", description = "Permanently cancels recurring milk delivery.")
    public ResponseEntity<ApiResponse<MilkSubscriptionResponse>> cancelSubscription(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkSubscriptionResponse response = milkSubscriptionService.cancelSubscription(subscriptionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled successfully", response));
    }

    @PostMapping("/generate-orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger scheduled order generation", description = "Generates daily orders for active subscriptions on a target date (Admin/cron hook).")
    public ResponseEntity<ApiResponse<List<Long>>> generateDailyOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        List<Long> generatedOrderIds = milkSubscriptionService.generateDailyOrdersForSubscriptions(date);
        return ResponseEntity.ok(ApiResponse.success("Generated " + generatedOrderIds.size() + " orders for date " + date, generatedOrderIds));
    }
}
