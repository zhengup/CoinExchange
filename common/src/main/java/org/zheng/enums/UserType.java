package org.zheng.enums;

public enum UserType {
    DEBT(1),
    TRADER(0);

    private final long userId;

    public long getInternalUserId() {
        return this.userId;
    }
    UserType(long userId) {
        this.userId = userId;
    }
}
