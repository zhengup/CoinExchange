package org.zheng.enums;

public enum OrderStatus {
    //等待成交
    PENDING(false),
    //完全成交
    FULLY_FILLED(true),
    //部分成交
    PARTIAL_FILLED(false),
    //部分成交后取消
    PARTIAL_CANCELLED(true),
    //全部取消
    FULLY_CANCELLED(true);

    public final boolean isFinalStatus;
    OrderStatus(boolean isFinalStatus) {
        this.isFinalStatus = isFinalStatus;
    }
}

