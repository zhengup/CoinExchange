package org.zheng.assets;

import org.springframework.stereotype.Component;
import org.zheng.enums.AssetEnum;
import org.zheng.support.LoggerSupport;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AssetService extends LoggerSupport {
    //资产结构
    final ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> userAssets = new ConcurrentHashMap<>();
    //获取资产
    public Asset getAsset(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> assets = userAssets.get(userId);
        if(assets == null) {
            return null;
        }
        return assets.get(assetId);
    }
    //获取类型和资产
    public Map<AssetEnum, Asset> getAssets(Long userId) {
        Map<AssetEnum, Asset> assets = userAssets.get(userId);
        if(assets == null) {
            return Map.of();
        }
        return assets;
    }
    //获取资产结构
    public ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> getUserAssets() {
        return this.userAssets;
    }
    //初始化资产
    public Asset initAssets(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> map = userAssets.get(userId);
        if(map == null) {
            map = new ConcurrentHashMap<>();
            userAssets.put(userId, map);
        }
        Asset zeroAsset = new Asset();
        map.put(assetId, zeroAsset);
        return zeroAsset;
    }

    //转账操作:转账类型，from/to，转账类型，转账数值，余额检查
    public boolean tryTransfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount, boolean checkBalance) {
        if(amount.signum() < 0){
            throw new IllegalArgumentException("钱不够啦");
        }
        Asset fromAsset = getAsset(fromUser, assetId);
        if(fromAsset == null){
            //资产不存在需要初始化
            fromAsset = initAssets(fromUser, assetId);
        }
        Asset toAsset = getAsset(toUser, assetId);
        if(toAsset == null){
            toAsset = initAssets(toUser, assetId);
        }
        return switch (type){
            case AVAILABLE_TO_AVAILABLE -> {
                //检查余额且余额不足
                if(checkBalance && fromAsset.available.compareTo(amount) < 0){
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            case AVAILABLE_TO_FROZEN -> {
                //检查余额且余额不足
                if(checkBalance && fromAsset.available.compareTo(amount) < 0){
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                if(checkBalance && fromAsset.frozen.compareTo(amount) < 0){
                    yield false;
                }
                fromAsset.frozen = toAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            default -> {
                throw new IllegalArgumentException("类型不对" + type);
            }
        };
    }
    //存钱不需要检查余额
    public void transfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount) {
        if(!tryTransfer(type, fromUser, toUser, assetId, amount, true)) {
            throw new RuntimeException("转账失败");
        }
        if(logger.isDebugEnabled()){
            logger.debug("transfer asset {}, from {} => {}, amount {}", assetId, fromUser, toUser, amount);
        }
    }
    //冻结资产
    public boolean tryFreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        boolean res = tryTransfer(Transfer.AVAILABLE_TO_FROZEN, userId,userId,assetId,amount,true);
        if(res && logger.isDebugEnabled()){
            logger.debug("freezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
        return res;
    }
    //解冻资产
    public void unfreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        if(!tryTransfer(Transfer.FROZEN_TO_AVAILABLE, userId,userId,assetId,amount,true)) {
            throw new RuntimeException("Unfreeze failed for user " + userId + ", asset = " + assetId + ", amount = " + amount);
        }
        if(logger.isDebugEnabled()){
            logger.debug("unfreezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
    }
}
