package org.zheng.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zheng.assets.AssetService;
import org.zheng.enums.AssetEnum;
import org.zheng.enums.Direction;
import org.zheng.model.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OrderService {
    //需要注入AssetService
    final AssetService assetService;

    public OrderService(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }
    //活动订单
    final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();
    //用户订单
    final ConcurrentMap<Long,ConcurrentMap<Long,OrderEntity>> userOrders = new ConcurrentHashMap<>();

    //订单创建
    public OrderEntity createOrder(long sequenceId, long ts, Long orderId, Long userId, Direction direction, BigDecimal price, BigDecimal quantity) {
        switch (direction){
            case BUY -> {
                //买入，冻结USD
                if(!assetService.tryFreeze(userId, AssetEnum.USD,price.multiply(quantity))){
                    return null;
                }
            }
            case SELL -> {
                //卖出，冻结BTC
                if(!assetService.tryFreeze(userId, AssetEnum.BTC, price.multiply(quantity))){
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("买卖不明！");
        }
        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.sequenceId = sequenceId;
        order.userId = userId;
        order.direction = direction;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createTime = order.updateTime = ts;
        this.activeOrders.put(order.id, order);
        //查看当前userId下是不是有订单结构，没有就创建新的
        ConcurrentMap<Long, OrderEntity> uOrders = userOrders.get(userId);
        if(uOrders == null){
            uOrders = new ConcurrentHashMap<>();
            this.userOrders.put(userId, uOrders);
        }
        uOrders.put(order.id, order);
        return order;
    }

    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return this.activeOrders;
    }
    public OrderEntity getOrder(long orderId) {
        return this.activeOrders.get(orderId);
    }
    public ConcurrentMap<Long,  OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    //删除活动的订单
    public void removeOrder(Long orderId) {
        OrderEntity removed = this.activeOrders.remove(orderId);
        if(removed == null){
            throw new IllegalArgumentException("Order not found by orderId in active orders: " + orderId);
        }
        ConcurrentMap<Long, OrderEntity> uOrders = userOrders.get(removed.userId);
        if(uOrders == null){
            throw new IllegalArgumentException("User orders not found by userId: " + removed.userId);
        }
        if(uOrders.remove(orderId) == null){
            throw new IllegalArgumentException("Order not found by orderId: " + orderId);
        }
    }
    public void debug() {
        System.out.println("---------- orders ----------");
        List<OrderEntity> orders = new ArrayList<>(this.activeOrders.values());
        Collections.sort(orders);
        for (OrderEntity order : orders) {
            System.out.println("  " + order.id + " " + order.direction + " price: " + order.price + " unfilled: "
                    + order.unfilledQuantity + " quantity: " + order.quantity + " sequenceId: " + order.sequenceId
                    + " userId: " + order.userId);
        }
        System.out.println("---------- // orders ----------");
    }
}
