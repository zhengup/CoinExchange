package org.zheng.web.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.zheng.assets.AssetService;
import org.zheng.bean.OrderBookBean;
import org.zheng.clearing.ClearingService;
import org.zheng.match.MatchDetailRecord;
import org.zheng.match.MatchEngine;
import org.zheng.message.ApiResultMessage;
import org.zheng.message.NotificationMessage;
import org.zheng.message.TickMessage;
import org.zheng.message.event.AbstractEvent;
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

import java.time.ZoneId;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private void runNotifyThread() {}
    private void runApiResultThread() {}
    private void runOrderBookThread() {}
    private void runDbThread() {}

    //保存到db
    private void saveToDb() {}

    //消息处理 kafka
    public void processMessage(List<AbstractEvent> messages){
        this.orderBookChanged = false;
        for(AbstractEvent event : messages){
            processEvent(event);
        }
        if(this.orderBookChanged){
            //获取最新的OrderBook快照
            this.lastOrderBook = this.matchEngine.getOrderBook(this.orderBookDepth);
        }
    }
    public void processEvent(AbstractEvent event){
        if(this.fatalError) return;
        if(event.sequenceId <= this.lastSequenceId){
            logger.warn("skip duplicate event: {}", event);
            return;
        }
    }

    //中止
    private void panic(){
        logger.error("application panic. exit now...");
        this.fatalError = true;
        System.exit(1);
    }

    boolean transfer(TransferEvent event){
        boolean ok = false;
        return ok;
    }

    void createOrder(OrderRequestEvent event){

    }

    private NotificationMessage createNotification(long ts, String type, Long userId, Object data){
        NotificationMessage msg = new NotificationMessage();
        msg.createTime = ts;
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }

    MatchDetailEntity createMatchDetail(long sequenceId, long timestamp, MatchDetailRecord detail, boolean forTaker){
        MatchDetailEntity matchDetail = new MatchDetailEntity();
    }

}
