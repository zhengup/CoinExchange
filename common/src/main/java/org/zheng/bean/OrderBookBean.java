package org.zheng.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.zheng.util.JsonUtil;

import java.math.BigDecimal;
import java.util.List;

public class OrderBookBean {
    public static final String EMPTY = JsonUtil.writeJsonAsString(new OrderBookBean(0, BigDecimal.ZERO, List.of(), List.of()));

    @JsonIgnore
    public long sequenceId;

    public BigDecimal price;
    public List<OrderBookItemBean> buy;
    public List<OrderBookItemBean> sell;

    public OrderBookBean(long sequenceId, BigDecimal price, List<OrderBookItemBean> buy, List<OrderBookItemBean> sell) {
        this.sequenceId = sequenceId;
        this.price = price;
        this.buy = buy;
        this.sell = sell;
    }
}
