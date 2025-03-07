package org.zheng.assets;

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

}
