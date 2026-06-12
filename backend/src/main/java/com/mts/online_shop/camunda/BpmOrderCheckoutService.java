package com.mts.online_shop.camunda;

import com.mts.online_shop.exception.EmptyCartException;
import com.mts.online_shop.exception.OrderNotFoundException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.mapper.OrderMapper;
import com.mts.online_shop.model.Order;
import com.mts.online_shop.model.OrderItem;
import com.mts.online_shop.model.OrderStatus;
import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.model.User;
import com.mts.online_shop.repository.OrderRepository;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.service.GoodsService;
import com.mts.online_shop.service.OrderService;
import com.mts.online_shop.service.ProductReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Checkout из Camunda delegate: отдельная JPA-транзакция (REQUIRES_NEW), иначе на WildFly/Narayana
 * падает при цепочке service task сразу после user task.
 */
@Service
public class BpmOrderCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(BpmOrderCheckoutService.class);

    private final UserRepository userRepository;
    private final GoodsService goodsService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductReservationService productReservationService;
    private final OrderService orderService;

    public BpmOrderCheckoutService(
            UserRepository userRepository,
            GoodsService goodsService,
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            ProductReservationService productReservationService,
            OrderService orderService) {
        this.userRepository = userRepository;
        this.goodsService = goodsService;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productReservationService = productReservationService;
        this.orderService = orderService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long createOrderFromCartAndReserve(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        List<ProductEntity> productsInCart = goodsService.findUserGoods(userId);
        if (productsInCart.isEmpty()) {
            throw new EmptyCartException("Cart for user with id: " + userId + " is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setItems(orderMapper.toOrderItems(order, productsInCart));
        Order savedOrder = orderRepository.save(order);
        goodsService.clearCart(userId);

        reserveOrderItems(savedOrder.getId());
        return savedOrder.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void reserveOrderProducts(Long orderId) {
        reserveOrderItems(orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markOrderPaid(Long orderId) {
        Order order = orderRepository.getOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void releaseReservation(Long orderId) {
        orderService.releaseProductReservation(orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void cancelOrderApproved(Long orderId) {
        try {
            orderService.executeAdminRefundIfPaid(orderId);
        } catch (Exception ex) {
            log.warn("Refund skipped for order {} during admin cancel: {}", orderId, ex.getMessage());
        }
        orderService.markOrderCancelled(orderId);
    }

    private void reserveOrderItems(Long orderId) {
        Order order = orderRepository.findByIdWithItemsAndProducts(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with id: " + orderId + " not found"));
        for (OrderItem item : order.getItems()) {
            productReservationService.reserveProduct(item.getProduct().getId(), 1);
        }
    }
}
