package org.zheng.model.trade;

import jakarta.persistence.*;
import org.zheng.enums.Direction;
import org.zheng.enums.MatchType;
import org.zheng.model.support.EntitySupport;

import java.math.BigDecimal;

//用于存储每个订单的匹配细节
@Entity
@Table(name = "match_details", uniqueConstraints = @UniqueConstraint(name = "UNI_OLD_COLD", columnNames = {"orderId ", "counterOrderId"}), indexes = @Index(name = "IDX_OLD_CT", columnList = "orderId,creatTime"))
public class MatchDetailEntity implements EntitySupport, Comparable<MatchDetailEntity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public Long id;

    @Column(nullable = false, updatable = false)
    public long sequenceId;

    @Column(nullable = false, updatable = false)
    public Long orderId;

    @Column(nullable = false, updatable = false)
    public Long counterOrderId;

    @Column(nullable = false, updatable = false)
    public Long userId;

    @Column(nullable = false, updatable = false)
    public Long counterUserId;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public MatchType type;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Direction direction;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    @Column(nullable = false, updatable = false)
    public long createTime;

    @Override
    public int compareTo(MatchDetailEntity o) {
        int cmp = Long.compare(this.orderId, o.orderId);
        if (cmp == 0) {
            cmp = Long.compare(this.counterOrderId, o.orderId);
        }
        return cmp;
    }
}
