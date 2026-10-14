package com.pashumandi.milk.controller;

import com.pashumandi.milk.dto.common.ApiResponse;
import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.inquiry.CreateInquiryRequest;
import com.pashumandi.milk.dto.inquiry.MilkInquiryResponse;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkInquiryService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/milk/inquiries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Milk Inquiries", description = "Buyer questions and seller inquiry management")
public class MilkInquiryController {

    private final MilkInquiryService milkInquiryService;

    @PostMapping("/listings/{listingId}")
    @Operation(summary = "Submit inquiry to seller", description = "Sends a message or inquiry to the milk seller.")
    public ResponseEntity<ApiResponse<MilkInquiryResponse>> submitInquiry(
            @PathVariable Long listingId,
            @Valid @RequestBody CreateInquiryRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkInquiryResponse response = milkInquiryService.submitInquiry(listingId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inquiry submitted successfully", response));
    }

    @GetMapping("/my-inquiries")
    @Operation(summary = "Get inquiries sent by buyer", description = "Returns inquiries submitted by current user.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkInquiryResponse>>> getMyInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkInquiryResponse> response = milkInquiryService.getMyInquiries(pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/received")
    @Operation(summary = "Get inquiries received by seller", description = "Returns customer inquiries received on seller's milk listings.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkInquiryResponse>>> getReceivedInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkInquiryResponse> response = milkInquiryService.getReceivedInquiries(pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
