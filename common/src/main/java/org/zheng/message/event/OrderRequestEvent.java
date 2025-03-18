package org.zheng.message.event;

import org.zheng.enums.Direction;

import java.math.BigDecimal;

public class OrderRequestEvent extends AbstractEvent {
    public long userId;
    public Direction direction;
    public BigDecimal price;
    public BigDecimal quantity;

    @Override
    public String toString() {
        return "OrderRequestEvent [sequenceId=" + sequenceId + ", previousId=" + previousId + ", uniqueId=" + uniqueId
                + ", refId=" + refId + ", createdAt=" + createTime + ", userId=" + userId + ", direction=" + direction
                + ", price=" + price + ", quantity=" + quantity + "]";
    }
}
