package org.zheng.assets;

import junit.framework.TestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zheng.enums.AssetEnum;

import java.math.BigDecimal;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
    static final Long DEBT = 1L;
    static final Long USER_A = 200L;
    static final Long USER_B = 300L;
    static final Long USER_C = 400L;

    AssetService service;

    @BeforeEach
    public void setUp() throws Exception {
        service = new AssetService();
        init();
    }
    @AfterEach
    public void tearDown() throws Exception {
        verify();
    }
    @Test
    void tryTransfer(){
        //A->B success
        service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, USER_A,USER_B,AssetEnum.USD,new BigDecimal("120000"),true);
        assertBDEquals(3321,service.getAsset(USER_A,AssetEnum.USD).available);
        assertBDEquals(120000+69888,service.getAsset(USER_B,AssetEnum.USD).available);
        //A->B failed
        assertFalse(service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, USER_A,USER_B,AssetEnum.USD,new BigDecimal("3322"),true));
        assertBDEquals(3321,service.getAsset(USER_A,AssetEnum.USD).available);
        assertBDEquals(120000+69888,service.getAsset(USER_B,AssetEnum.USD).available);
    }
    @Test
    void tryFreeze(){

    }
    @Test
    void unfreeze(){

    }
    @Test
    void transfer(){

    }

    void init() throws Exception {
        service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE,DEBT,USER_A, AssetEnum.USD, BigDecimal.valueOf(123321),false);
        service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE,DEBT,USER_A, AssetEnum.BTC, BigDecimal.valueOf(12),false);
        service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE,DEBT,USER_B, AssetEnum.USD, BigDecimal.valueOf(69888),false);
        service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE,DEBT,USER_C, AssetEnum.BTC, BigDecimal.valueOf(33),false);
        assertBDEquals(-193209,service.getAsset(DEBT,AssetEnum.USD).available);
        assertBDEquals(-45,service.getAsset(DEBT,AssetEnum.BTC).available);
    }
    void verify() throws Exception {
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for(Long userId:service.userAssets.keySet()){
            var assetUSD = service.getAsset(userId,AssetEnum.USD);
            if(assetUSD != null){
                totalUSD = totalUSD.add(assetUSD.available).add(assetUSD.frozen);
            }
            var assetBTC = service.getAsset(userId,AssetEnum.BTC);
            if(assetBTC != null){
                totalBTC = totalBTC.add(assetBTC.available).add(assetBTC.frozen);
            }
        }
        assertBDEquals(0,totalBTC);
        assertBDEquals(0,totalUSD);
    }

    void assertBDEquals(long value, BigDecimal bd) {
        assertBDEquals(String.valueOf(value), bd);
    }
    void assertBDEquals(String value, BigDecimal bd) {
        assertEquals(String.format("Expected %s but actual %s.", value, bd.toPlainString()), 0, new BigDecimal(value).compareTo(bd));
    }

}
