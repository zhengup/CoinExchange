package org.zheng.web.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.zheng.assets.Asset;
import org.zheng.assets.AssetService;
import org.zheng.bean.OrderBookBean;
import org.zheng.clearing.ClearingService;
import org.zheng.enums.AssetEnum;
import org.zheng.enums.MatchType;
import org.zheng.enums.UserType;
import org.zheng.match.MatchDetailRecord;
import org.zheng.match.MatchEngine;
import org.zheng.message.ApiResultMessage;
import org.zheng.message.NotificationMessage;
import org.zheng.message.TickMessage;
import org.zheng.message.event.AbstractEvent;
import org.zheng.message.event.OrderCancelEvent;
import org.zheng.message.event.OrderRequestEvent;
import org.zheng.message.event.TransferEvent;
import org.zheng.messaging.MessageConsumer;
import org.zheng.messaging.MessageProducer;
import org.zheng.messaging.MessagingFactory;
import org.zheng.model.trade.MatchDetailEntity;
import org.zheng.model.trade.OrderEntity;
import org.zheng.order.OrderService;
import org.zheng.redis.RedisService;
import org.zheng.store.StoreService;
import org.zheng.support.LoggerSupport;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@Component
public class TradingEngineService extends LoggerSupport {

    @Autowired(required = false)
    ZoneId zoneId = ZoneId.systemDefault();

    @Value("#{exchangeConfiguration.orderBookDepth}")
    int orderBookDepth = 100;

    @Value("#{exchangeConfiguration.debugMode}")
    boolean debugMode = false;

    boolean fatalError = false;

    @Autowired
    AssetService assetService;
    @Autowired
    OrderService orderService;
    @Autowired
    MatchEngine matchEngine;
    @Autowired
    ClearingService clearingService;

    //消息处理 kafka
    @Autowired
    MessagingFactory messagingFactory;
    //存储服务
    @Autowired
    StoreService storeService;
    //redis服务
    @Autowired
    RedisService redisService;

    //消息处理
    private MessageConsumer consumer;
    private MessageProducer<TickMessage> producer;

    private long lastSequenceId = 0;

    private boolean orderBookChanged = false;

    private String shaUpdateOrderBookLua;

    private Thread tickThread;
    private Thread notifyThread;
    private Thread apiResultThread;
    private Thread orderBookThread;
    private Thread dbThread;

    private OrderBookBean lastOrderBook = null;
    private Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedQueue<>();
    private Queue<List<MatchDetailEntity> matchQueue = new ConcurrentLinkedQueue<>();
    private Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();
    private Queue<ApiResultMessage> apiResultQueue = new ConcurrentLinkedQueue<>();
    private Queue<NotificationMessage> notificationQueue = new ConcurrentLinkedQueue<>();

    //初始化
    @PostConstruct
    public void init() {
        this.shaUpdateOrderBookLua = this.redisService.
    }

    //清理
    @PreDestroy
    public void destroy() {

    }

    //启动tick
    private void runTickThread() {

    }

    //启动notify线程
    private void runNotifyThread() {
    }

    private void runApiResultThread() {
    }

    private void runOrderBookThread() {
    }

    private void runDbThread() {
    }

    //保存到db
    private void saveToDb() {
    }

    //消息处理 kafka
    public void processMessage(List<AbstractEvent> messages) {
        this.orderBookChanged = false;
        for (AbstractEvent event : messages) {
            processEvent(event);
        }
        if (this.orderBookChanged) {
            //获取最新的OrderBook快照
            this.lastOrderBook = this.matchEngine.getOrderBook(this.orderBookDepth);
        }
    }

    public void processEvent(AbstractEvent event) {
        if (this.fatalError) return;
        if (event.sequenceId <= this.lastSequenceId) {
            logger.warn("skip duplicate event: {}", event);
            return;
        }
    }

    //中止
    private void panic() {
        logger.error("application panic. exit now...");
        this.fatalError = true;
        System.exit(1);
    }

    boolean transfer(TransferEvent event) {
        boolean ok = false;
        return ok;
    }

    void createOrder(OrderRequestEvent event) {

    }

    private NotificationMessage createNotification(long ts, String type, Long userId, Object data) {
        NotificationMessage msg = new NotificationMessage();
        msg.createTime = ts;
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }

    MatchDetailEntity createMatchDetail(long sequenceId, long timestamp, MatchDetailRecord detail, boolean forTaker) {
        MatchDetailEntity matchDetail = new MatchDetailEntity();
        matchDetail.sequenceId = sequenceId;
        matchDetail.orderId = forTaker ? detail.takerOrder().id : detail.makerOrder().id;
        matchDetail.counterOrderId = forTaker ? detail.makerOrder().id : detail.takerOrder().id;
        matchDetail.direction = forTaker ? detail.takerOrder().direction : detail.makerOrder().direction;
        matchDetail.price = detail.price();
        matchDetail.quantity = detail.quantity();
        matchDetail.type = forTaker ? MatchType.TAKER : MatchType.MAKER;
        matchDetail.userId = forTaker ? detail.takerOrder().userId : detail.makerOrder().userId;
        matchDetail.counterUserId = forTaker ? detail.makerOrder().userId : detail.takerOrder().userId;
        matchDetail.createTime = timestamp;
        return matchDetail;
    }

    void cancelOrder(OrderCancelEvent event) {
        OrderEntity order = this.orderService.getOrder(event.refOrderId);
        //未找到活动订单或者订单不属于该用户
        if (order == null || !Objects.equals(order.userId, event.userId)) {
            this.apiResultQueue.add(ApiResultMessage.cancelOrderFailed(event.refId, event.createTime));
            return;
        }
        this.matchEngine.cancel(event.createTime, order);
        this.clearingService.clearCancelOrder(order);
        this.orderBookChanged = true;

        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order, event.createTime));
        this.notificationQueue.add(createNotification(event.createTime, "order_canceled", order.userId, order));

    }

    public void debug() {
        System.out.println("========== trading engine ==========");
        this.assetService.debug();
        this.orderService.debug();
        this.matchEngine.debug();
        System.out.println("========== // trading engine ==========");
    }

    void validate() {
        logger.debug("start validate...");

        logger.debug("validate done...");
    }

    //验证系统资产完整性
    void validateAssets() {
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for (Map.Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.assetService.getUserAssets().entrySet()) {
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetEnum, Asset> assets = userEntry.getValue();
            for (Map.Entry<AssetEnum, Asset> assetEntry : assets.entrySet()) {
                AssetEnum assetId = assetEntry.getKey();
                Asset asset = assetEntry.getValue();
                if (userId == UserType.DEBT.getInternalUserId()) {
                    // 系统负债账户available不允许为正:
                    require(asset.getAvailable().signum() <= 0, "Debt has positive available: " + asset);
                    // 系统负债账户frozen必须为0:
                    require(asset.getFrozen().signum() == 0, "Debt has non-zero frozen: " + asset);
                } else {
                    // 交易用户的available/frozen不允许为负数:
                    require(asset.getAvailable().signum() >= 0, "Trader has negative available: " + asset);
                    require(asset.getFrozen().signum() >= 0, "Trader has negative frozen: " + asset);
                }switch (assetId) {
                    case USD -> totalUSD = totalUSD.add(asset.getTotal());
                    case BTC -> totalBTC = totalBTC.add(asset.getTotal());
                    default -> require(false, "Unexpected asset id: " + assetId);
                }
            }
        }
        // 各类别资产总额为0:
        require(totalUSD.signum() == 0, "Non zero USD balance: " + totalUSD);
        require(totalBTC.signum() == 0, "Non zero BTC balance: " + totalBTC);
    }
    //验证订单
    void validateOrders(){
        Map<Long, Map<AssetEnum, BigDecimal>> userOrderFrozen = new HashMap<>();
        for (Map.Entry<Long, OrderEntity> entry : this.orderService.getActiveOrders().entrySet()) {
            OrderEntity order = entry.getValue();
            require(order.unfilledQuantity.signum() > 0, "Active order must have positive unfilled amount: " + order);
            switch (order.direction) {
                case BUY -> {
                    // 订单必须在MatchEngine中:
                    require(this.matchEngine.buyBook.exist(order), "order not found in buy book: " + order);
                    // 累计冻结的USD:
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.USD, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.USD);
                    frozenAssets.put(AssetEnum.USD, frozen.add(order.price.multiply(order.unfilledQuantity)));
                }
                case SELL -> {
                    // 订单必须在MatchEngine中:
                    require(this.matchEngine.sellBook.exist(order), "order not found in sell book: " + order);
                    // 累计冻结的BTC:
                    userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                    Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                    frozenAssets.putIfAbsent(AssetEnum.BTC, BigDecimal.ZERO);
                    BigDecimal frozen = frozenAssets.get(AssetEnum.BTC);
                    frozenAssets.put(AssetEnum.BTC, frozen.add(order.unfilledQuantity));
                }
                default -> require(false, "Unexpected order direction: " + order.direction);
            }
        }
        // 订单冻结的累计金额必须和Asset冻结一致:
        for (Map.Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.assetService.getUserAssets().entrySet()) {
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetEnum, Asset> assets = userEntry.getValue();
            for (Map.Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (asset.getFrozen().signum() > 0) {
                    Map<AssetEnum, BigDecimal> orderFrozen = userOrderFrozen.get(userId);
                    require(orderFrozen != null, "No order frozen found for user: " + userId + ", asset: " + asset);
                    BigDecimal frozen = orderFrozen.get(assetId);
                    require(frozen != null, "No order frozen found for asset: " + asset);
                    require(frozen.compareTo(asset.getFrozen()) == 0,
                            "Order frozen " + frozen + " is not equals to asset frozen: " + asset);
                    // 从userOrderFrozen中删除已验证的Asset数据:
                    orderFrozen.remove(assetId);
                }
            }
        }
        // userOrderFrozen不存在未验证的Asset数据:
        for (Map.Entry<Long, Map<AssetEnum, BigDecimal>> userEntry : userOrderFrozen.entrySet()) {
            Long userId = userEntry.getKey();
            Map<AssetEnum, BigDecimal> frozenAssets = userEntry.getValue();
            require(frozenAssets.isEmpty(), "User " + userId + " has unexpected frozen for order: " + frozenAssets);
        }
    }
    void validateMatchEngine() {
        // OrderBook的Order必须在ActiveOrders中:
        Map<Long, OrderEntity> copyOfActiveOrders = new HashMap<>(this.orderService.getActiveOrders());
        for (OrderEntity order : this.matchEngine.buyBook.book.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in buy book is not in active orders: " + order);
        }
        for (OrderEntity order : this.matchEngine.sellBook.book.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in sell book is not in active orders: " + order);
        }
        // activeOrders的所有Order必须在Order Book中:
        require(copyOfActiveOrders.isEmpty(), "Not all active orders are in order book.");
    }

    void require(boolean condition, String errorMessage) {
        if (!condition) {
            logger.error("validate failed: {}", errorMessage);
            panic();
        }
    }

}
