package org.zheng.assets;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

public class Asset {

    //可用的
    BigDecimal available;

    //冻结的
    BigDecimal frozen;

    public Asset(){
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }
    public Asset(BigDecimal available, BigDecimal frozen){
        this.available = available;
        this.frozen = frozen;
    }
    public BigDecimal getAvailable() {
        return available;
    }
    public BigDecimal getFrozen() {
        return frozen;
    }
    @JsonIgnore
    public BigDecimal getTotal() {
        return available.add(frozen);
    }

    @Override
    public String toString() {
        return String.format("[available=%04.2f, frozen=%02.2f]", available, frozen);
    }
}
