package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.order.CancelOrderRequest;
import com.pashumandi.milk.dto.order.CreateMilkOrderRequest;
import com.pashumandi.milk.dto.order.MilkOrderResponse;
import com.pashumandi.milk.dto.order.UpdateOrderStatusRequest;
import com.pashumandi.milk.enums.OrderStatus;
import com.pashumandi.milk.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

public interface MilkOrderService {

    MilkOrderResponse placeOrder(CreateMilkOrderRequest request, AuthenticatedUser currentUser);

    PagedResponse<MilkOrderResponse> getBuyerOrders(OrderStatus status, Pageable pageable, AuthenticatedUser currentUser);

    PagedResponse<MilkOrderResponse> getSellerSales(OrderStatus status, Pageable pageable, AuthenticatedUser currentUser);

    MilkOrderResponse getOrderById(Long orderId, AuthenticatedUser currentUser);

    MilkOrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, AuthenticatedUser currentUser);

    MilkOrderResponse cancelOrder(Long orderId, CancelOrderRequest request, AuthenticatedUser currentUser);
}
