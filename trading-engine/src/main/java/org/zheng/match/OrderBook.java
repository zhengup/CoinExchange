package org.zheng.match;

import org.aspectj.weaver.ast.Or;
import org.jetbrains.annotations.NotNull;
import org.zheng.bean.OrderBookBean;
import org.zheng.bean.OrderBookItemBean;
import org.zheng.enums.Direction;
import org.zheng.model.trade.OrderEntity;

import java.util.*;

public class OrderBook {
    //订单簿，使用红黑树实现
    public final Direction direction;
    public final TreeMap<OrderKey, OrderEntity> book;

    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }
    //查找首元素，插入，删除
    public OrderEntity getFirst() {
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }
    public boolean remove(OrderEntity order){
        return this.book.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }
    public boolean add(OrderEntity order){
        return this.book.put(new OrderKey(order.sequenceId, order.price), order) != null;
    }
    public boolean exist(OrderEntity order){
        return this.book.containsKey(new OrderKey(order.sequenceId, order.price));
    }
    public int size(){
        return this.book.size();
    }
    public List<OrderBookItemBean> getOrderBook(int maxDepth) {
        List<OrderBookItemBean> items = new ArrayList<>(maxDepth);
        OrderBookItemBean prev = null;
        for(OrderKey key : this.book.keySet()) {
            OrderEntity order = this.book.get(key);
            if(prev == null){
                prev = new OrderBookItemBean(order.price, order.unfilledQuantity);
                items.add(prev);
            }else{
                if(order.price.compareTo(prev.price) == 0){
                    prev.addQuantity(order.unfilledQuantity);
                }else{
                    if(items.size() > maxDepth){
                        break;
                    }
                    prev = new OrderBookItemBean(order.price, order.unfilledQuantity);
                    items.add(prev);
                }
            }
        }
        return items;
    }


    public String toString() {
        if (this.book.isEmpty()) return "(empty)";
        List<String> orders = new ArrayList<>(10);
        for (Map.Entry<OrderKey, OrderEntity> entry : this.book.entrySet()) {
            OrderEntity order = entry.getValue();
            orders.add("  " + order.price + " " + order.unfilledQuantity + " " + order.toString());
        }
        if(direction == Direction.SELL){
            Collections.reverse(orders);
        }
        return String.join("\n", orders);
    }
    //买盘排序，价格高优先，时间早优先
    private static final Comparator<OrderKey> SORT_BUY = new Comparator<>(){
        @Override
        public int compare(@NotNull OrderKey o1, @NotNull OrderKey o2) {
            int cmp = o2.price().compareTo(o1.price());
            return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };
    //卖盘排序，价格低优先，时间早优先
    private static final Comparator<OrderKey> SORT_SELL = new Comparator<>() {
        @Override
        public int compare(@NotNull OrderKey o1, @NotNull OrderKey o2) {
            int cmp = o1.price().compareTo(o2.price());
            return cmp == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : cmp;
        }
    };
}


