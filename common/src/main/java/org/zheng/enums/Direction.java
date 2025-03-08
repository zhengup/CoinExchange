package org.zheng.enums;

public enum Direction {
    BUY(1),
    SELL(0);

    public final int value;

    public Direction negate(){
        return this == BUY ? SELL : BUY;
    }
    Direction(int value){
        this.value = value;
    }
    public static Direction of(int value){
        if(value == 1) return BUY;
        if(value == 0) return SELL;
        throw new IllegalArgumentException("交易方向错误！");
    }
}
