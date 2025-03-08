package org.zheng.model.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.zheng.enums.Direction;
import org.zheng.enums.OrderStatus;
import org.zheng.model.support.EntitySupport;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class OrderEntity implements EntitySupport, Comparable<OrderEntity> {
    //订单ID,primary key
    @Id
    @Column(nullable = false,updatable = false)
    public Long id;

    //定序ID，用来排列交易的顺序
    @Column(nullable = false,updatable = false)
    public long sequenceId;

    //对应的用户ID
    @Column(nullable = false,updatable = false)
    public Long userId;

    //交易价格
    @Column(nullable = false,updatable = false,precision = PRECISION,scale = SCALE)
    public BigDecimal price;

    //买还是卖，交易方向，枚举类来定义
    @Column(nullable = false,updatable = false,length = VAR_ENUM)
    public Direction direction;

    //订单的状态，应该使用枚举类来定义
    @Column(nullable = false,updatable = false,length = VAR_ENUM)
    public OrderStatus orderStatus;

    public void updateOrderStatus(BigDecimal unfilledQuantity, OrderStatus orderStatus, long updateTime) {
        this.version++;
        this.unfilledQuantity = unfilledQuantity;
        this.orderStatus = orderStatus;
        this.updateTime = updateTime;
        this.version++;
    }

    //订单数量
    @Column(nullable = false,updatable = false,precision = PRECISION,scale = SCALE)
    public BigDecimal quantity;
    //未成交数量
    @Column(nullable = false,updatable = false,precision = PRECISION,scale = SCALE)
    public BigDecimal unfilledQuantity;

    //创建时间
    @Column(nullable = false,updatable = false)
    public long createTime;
    //更新时间
    @Column(nullable = false,updatable = false)
    public long updateTime;

    private int version;

    //双重保护：版本号既不会存入数据库，也不会通过 API 接口暴露
    //典型应用：用于内部状态追踪字段（如乐观锁版本号）
    @Transient
    // 此数据不会出现在 JSON 响应中
    @JsonIgnore
    public int getVersion() {
        return this.version;
    }

    @Nullable
    public OrderEntity copy(){
        OrderEntity orderEntity = new OrderEntity();
        int version = this.version;
        orderEntity.orderStatus = this.orderStatus;
        orderEntity.unfilledQuantity = this.unfilledQuantity;
        orderEntity.updateTime = this.updateTime;
        if(version != this.version){
            return null;
        }
        orderEntity.createTime = this.createTime;
        orderEntity.direction = this.direction;
        orderEntity.id = this.id;
        orderEntity.price = this.price;
        orderEntity.quantity = this.quantity;
        orderEntity.sequenceId = this.sequenceId;
        orderEntity.userId = this.userId;
        return orderEntity;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if(obj instanceof OrderEntity orderEntity){
            return Objects.equals(this.id, orderEntity.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "OrderEntity [id=" + id + ", sequenceId=" + sequenceId + ", direction=" + direction + ", userId="
                + userId + ", status=" + orderStatus + ", price=" + price + ", createdAt=" + createTime + ", updatedAt="
                + updateTime + ", version=" + version + ", quantity=" + quantity + ", unfilledQuantity="
                + unfilledQuantity + "]";
    }

    //id排序
    @Override
    public int compareTo(OrderEntity o){
        return Long.compare(this.id, o.id);
    }
}
