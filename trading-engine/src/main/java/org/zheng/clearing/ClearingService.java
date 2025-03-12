package org.zheng.clearing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zheng.assets.AssetService;
import org.zheng.assets.Transfer;
import org.zheng.enums.AssetEnum;
import org.zheng.match.MatchDetailRecord;
import org.zheng.match.MatchResult;
import org.zheng.model.trade.OrderEntity;
import org.zheng.order.OrderService;
import org.zheng.support.LoggerSupport;

import java.math.BigDecimal;

@Component
public class ClearingService extends LoggerSupport {
    final AssetService assetService;
    final OrderService orderService;

    public ClearingService(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    public void clearMatchResult(MatchResult matchResult) {
        OrderEntity taker = matchResult.takerOrder;
        switch (taker.direction) {
            case BUY -> {
                for (MatchDetailRecord detail : matchResult.matchDetails) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("clear buy matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                detail.price(), detail.quantity(), detail.takerOrder().id, detail.makerOrder().id,
                                detail.takerOrder().userId, detail.makerOrder().userId);
                    }
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matchedQuantity = detail.quantity();
                    //实际买价比自己的报价低
                    if (taker.price.compareTo(maker.price) > 0) {
                        BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matchedQuantity);
                        logger.debug("unfree extra unused quote {} back to taker user {}", unfreezeQuote, taker.userId);
                        assetService.unfreeze(taker.userId, AssetEnum.USD, unfreezeQuote);
                    }
                    //买方USD转到卖方账户
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.USD, maker.price.multiply(matchedQuantity));
                    //卖方BTC转到卖方账户
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC, matchedQuantity);
                    //删除完全成交的maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }
                //删除完全成交的taker
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            case SELL -> {
                for (MatchDetailRecord detail : matchResult.matchDetails) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "clear sell matched detail: price = {}, quantity = {}, takerOrderId = {}, makerOrderId = {}, takerUserId = {}, makerUserId = {}",
                                detail.price(), detail.quantity(), detail.takerOrder().id, detail.makerOrder().id,
                                detail.takerOrder().userId, detail.makerOrder().userId);
                    }
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matchedQuantity = detail.quantity();
                    //卖方btc转给买方
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.BTC, matchedQuantity);
                    //买方usd转给卖方
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC, maker.price.multiply(matchedQuantity));
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + taker.direction);
        }
    }
    public void clearCancelOrder(OrderEntity orderEntity) {
        switch (orderEntity.direction){
            case BUY -> {
                assetService.unfreeze(orderEntity.userId, AssetEnum.USD, orderEntity.price.multiply(orderEntity.unfilledQuantity));
            }
            case SELL -> {
                assetService.unfreeze(orderEntity.userId, AssetEnum.BTC, orderEntity.unfilledQuantity);
            }
            default -> throw new IllegalStateException("Unexpected value: " + orderEntity.direction);
        }
        orderService.removeOrder(orderEntity.id);
    }
}
