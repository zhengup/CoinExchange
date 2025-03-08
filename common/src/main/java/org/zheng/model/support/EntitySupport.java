package org.zheng.model.support;

public interface EntitySupport {
    /**
     * Default big decimal storage type: DECIMAL(PRECISION, SCALE)
     * <p>
     * Range = +/-999999999999999999.999999999999999999
     */
    int PRECISION = 36;
    //定义缩放系数
    int SCALE = 18;
    int VAR_ENUM = 32;
    int VAR_CHAR_50 = 50;
    int VAR_CHAR_100 = 100;
    int VAR_CHAR_200 = 200;
    int VAR_CHAR_1000 = 1000;
    int VAR_CHAR_10000 = 10000;
}
