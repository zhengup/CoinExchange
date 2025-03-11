package org.zheng.match;

import org.springframework.stereotype.Component;
import org.zheng.bean.OrderBookBean;
import org.zheng.enums.Direction;
import org.zheng.enums.OrderStatus;
import org.zheng.model.trade.OrderEntity;

import java.math.BigDecimal;

@Component
public class MatchEngine {
    public final OrderBook buyBook = new OrderBook(Direction.BUY);
    public final OrderBook sellBook = new OrderBook(Direction.SELL);
    public BigDecimal marketPrice = BigDecimal.ZERO; //记录最新市场价
    private long sequenceId; //记录上次处理的sequence id

    public MatchResult processOrder(long sequenceId, OrderEntity order) {
        return switch (order.direction){
            case BUY -> processOrder(sequenceId, order, this.sellBook, this.buyBook);
            case SELL -> processOrder(sequenceId, order, this.buyBook, this.sellBook);
            default -> throw new IllegalArgumentException("Invalid direction");
        };
    }

    private MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook makerBook, OrderBook anotherBook) {
        this.sequenceId = sequenceId;
        long ts = takerOrder.createTime;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        for(;;){
            OrderEntity makerOrder = makerBook.getFirst();
            if(makerOrder == null){
                //对手盘不存在
                break;
            }
            if(takerOrder.direction == Direction.BUY && takerOrder.price.compareTo(makerOrder.price) < 0){
                //买入价格低于最低卖出价格
                break;
            }else if(takerOrder.direction == Direction.SELL && takerOrder.price.compareTo(makerOrder.price) > 0){
                //卖出价格高于买入的最高价格
                break;
            }
            //以maker价格成交
            this.marketPrice = makerOrder.price;
            //成交量
            BigDecimal matchedQuantity = takerUnfilledQuantity.min(makerOrder.unfilledQuantity);
            //成交记录
            matchResult.add(makerOrder.price, matchedQuantity, makerOrder);
            //更新成交后的订单数量
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchedQuantity);
            BigDecimal makerUnfilledQuantity = makerOrder.unfilledQuantity.subtract(matchedQuantity);
            //对手盘全部成交后，从订单簿删除
            if(makerUnfilledQuantity.signum() == 0){
                makerOrder.updateOrderStatus(makerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                makerBook.remove(makerOrder);
            }else{
                //对手盘部分成交
                makerOrder.updateOrderStatus(makerUnfilledQuantity, OrderStatus.PARTIAL_FILLED, ts);
            }
            //taker订单全部完成后，退出
            if(takerUnfilledQuantity.signum() == 0){
                takerOrder.updateOrderStatus(takerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                break;
            }
        }
        //taker订单没有全部成交，放入订单簿
        if(takerUnfilledQuantity.signum() > 0){
            takerOrder.updateOrderStatus(takerUnfilledQuantity, takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatus.PENDING : OrderStatus.PARTIAL_FILLED, ts);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }
    public void cancel (long ts, OrderEntity order) {
        OrderBook book = order.direction == Direction.BUY ? this.buyBook : this.sellBook;
        if(!book.remove(order)){
            throw new IllegalArgumentException("Order not found in order book.");
        }
        OrderStatus status = order.unfilledQuantity.compareTo(order.quantity) == 0 ? OrderStatus.FULLY_CANCELLED : OrderStatus.PARTIAL_CANCELLED;
        order.updateOrderStatus(order.quantity, status, ts);
    }
    public OrderBookBean getOrderBook(int maxDepth) {
        return new OrderBookBean(this.sequenceId,this.marketPrice,this.buyBook.getOrderBook(maxDepth),this.sellBook.getOrderBook(maxDepth));
    }

    public void debug() {
        System.out.println("---------- match engine ----------");
        System.out.println(this.sellBook);
        System.out.println("  ----------");
        System.out.println("  " + this.marketPrice);
        System.out.println("  ----------");
        System.out.println(this.buyBook);
        System.out.println("---------- // match engine ----------");
    }
}

