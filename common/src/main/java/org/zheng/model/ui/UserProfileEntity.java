package org.zheng.model.ui;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zheng.model.support.EntitySupport;
import org.zheng.support.AbstractDbService;

@Entity
@Table(name = "user_profiles", uniqueConstraints = { @UniqueConstraint(name = "UNI_EMAIL", columnNames = { "email" }) })
public class UserProfileEntity implements EntitySupport {

    /**
     * 关联至用户ID.
     */
    @Id
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * 登录Email
     */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_100)
    public String email;

    @Column(nullable = false, length = VAR_CHAR_100)
    public String name;

    @Column(nullable = false, updatable = false)
    public long createTime;

    @Column(nullable = false)
    public long updateTime;

    @Override
    public String toString() {
        return "UserProfileEntity [userId=" + userId + ", email=" + email + ", name=" + name + ", createdAt="
                + createTime + ", updatedAt=" + updateTime + "]";
    }
}