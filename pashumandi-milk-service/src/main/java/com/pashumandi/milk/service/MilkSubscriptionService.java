package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.subscription.CreateSubscriptionRequest;
import com.pashumandi.milk.dto.subscription.MilkSubscriptionResponse;
import com.pashumandi.milk.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface MilkSubscriptionService {

    MilkSubscriptionResponse createSubscription(CreateSubscriptionRequest request, AuthenticatedUser currentUser);

    PagedResponse<MilkSubscriptionResponse> getMySubscriptions(Pageable pageable, AuthenticatedUser currentUser);

    PagedResponse<MilkSubscriptionResponse> getSellerSubscriptions(Pageable pageable, AuthenticatedUser currentUser);

    MilkSubscriptionResponse getSubscriptionById(Long subscriptionId, AuthenticatedUser currentUser);

    MilkSubscriptionResponse pauseSubscription(Long subscriptionId, AuthenticatedUser currentUser);

    MilkSubscriptionResponse resumeSubscription(Long subscriptionId, AuthenticatedUser currentUser);

    MilkSubscriptionResponse cancelSubscription(Long subscriptionId, AuthenticatedUser currentUser);

    List<Long> generateDailyOrdersForSubscriptions(LocalDate targetDate);
}
