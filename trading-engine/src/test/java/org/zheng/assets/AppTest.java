package org.zheng.assets;

import junit.framework.TestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zheng.enums.AssetEnum;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
        // freeze 120000 ok:
        service.tryFreeze(USER_A, AssetEnum.USD, new BigDecimal("120000"));
        assertBDEquals(3321, service.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(120000, service.getAsset(USER_A, AssetEnum.USD).frozen);

        // freeze 3321 failed:
        assertFalse(service.tryFreeze(USER_A, AssetEnum.USD, new BigDecimal("3322")));
        assertBDEquals(3321, service.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(120000, service.getAsset(USER_A, AssetEnum.USD).frozen);
    }
    @Test
    void unfreeze(){
        // freeze 120000 ok:
        service.tryFreeze(USER_A, AssetEnum.USD, new BigDecimal("120000"));
        assertBDEquals(3321, service.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(120000, service.getAsset(USER_A, AssetEnum.USD).frozen);

        // unfreeze 9000 ok:
        service.unfreeze(USER_A, AssetEnum.USD, new BigDecimal("90000"));
        assertBDEquals(93321, service.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(30000, service.getAsset(USER_A, AssetEnum.USD).frozen);

        // unfreeze 3001 failed:
        assertThrows(RuntimeException.class, () -> {
            service.unfreeze(USER_A, AssetEnum.USD, new BigDecimal("93322"));
        });
    }
    @Test
    void transfer(){
        // A USD -> A frozen:
        service.transfer(Transfer.AVAILABLE_TO_FROZEN, USER_A, USER_A, AssetEnum.USD, new BigDecimal("120000"));
        assertBDEquals(3321, service.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(120000, service.getAsset(USER_A, AssetEnum.USD).frozen);

        // A frozen -> C available:
        service.transfer(Transfer.FROZEN_TO_AVAILABLE, USER_A, USER_C, AssetEnum.USD, new BigDecimal("80000"));
        assertBDEquals(40000, service.getAsset(USER_A, AssetEnum.USD).frozen);
        assertBDEquals(80000, service.getAsset(USER_C, AssetEnum.USD).available);

        // A frozen -> B available failed:
        assertThrows(RuntimeException.class, () -> {
            service.transfer(Transfer.FROZEN_TO_AVAILABLE, USER_A, USER_B, AssetEnum.USD, new BigDecimal("40001"));
        });
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
