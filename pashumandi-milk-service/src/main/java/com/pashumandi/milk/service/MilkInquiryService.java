package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.inquiry.CreateInquiryRequest;
import com.pashumandi.milk.dto.inquiry.MilkInquiryResponse;
import com.pashumandi.milk.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

public interface MilkInquiryService {

    MilkInquiryResponse submitInquiry(Long listingId, CreateInquiryRequest request, AuthenticatedUser currentUser);

    PagedResponse<MilkInquiryResponse> getMyInquiries(Pageable pageable, AuthenticatedUser currentUser);

    PagedResponse<MilkInquiryResponse> getReceivedInquiries(Pageable pageable, AuthenticatedUser currentUser);
}
