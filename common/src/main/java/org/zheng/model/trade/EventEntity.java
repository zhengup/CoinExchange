package org.zheng.model.trade;

import jakarta.persistence.*;
import org.zheng.model.support.EntitySupport;

import java.io.Serializable;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(name = "UNI_PREV_ID", columnNames = { "previousId" }))
public class EventEntity implements EntitySupport {
    /**
     * Primary key: assigned.
     */
    @Id
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Keep previous id. The previous id of first event is 0.
     */
    @Column(nullable = false, updatable = false)
    public long previousId;

    /**
     * JSON-encoded event data.
     */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_10000)
    public String data;

    @Column(nullable = false, updatable = false)
    public long createTime;

    @Override
    public String toString() {
        return "EventEntity [sequenceId=" + sequenceId + ", previousId=" + previousId + ", data=" + data
                + ", createdAt=" + createTime + "]";
    }
}
