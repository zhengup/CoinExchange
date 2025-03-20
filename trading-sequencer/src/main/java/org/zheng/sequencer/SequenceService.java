package org.zheng.sequencer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;
import org.zheng.message.event.AbstractEvent;
import org.zheng.messaging.*;
import org.zheng.support.LoggerSupport;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SequenceService extends LoggerSupport implements CommonErrorHandler {

    private static final String GROUP_ID = "SequencerGroup";

    @Autowired
    private SequenceHandler sequenceHandler;

    @Autowired
    private MessagingFactory messagingFactory;

    @Autowired
    private MessageTypes messageTypes;

    private MessageProducer<AbstractEvent> messageProducer;

    private AtomicLong sequence;
    private Thread jobThread;
    private boolean running;

    @PostConstruct
    public void init() {
        Thread thread = new Thread(() -> {
            logger.info("start sequence job...");
            // TODO: try get global DB lock: 主备锁有待实现
            // while (!hasLock()) { sleep(10000); }
            this.messageProducer = this.messagingFactory.createMessageProducer(Messaging.Topic.TRADE,
                    AbstractEvent.class);

            // 查找最大事件ID
            this.sequence = new AtomicLong(this.sequenceHandler.getMaxSequenceId());

            // 初始化consumer
            logger.info("create message consumer for {}...", getClass().getName());
            // 共享group-id
            MessageConsumer consumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.SEQUENCE,
                    GROUP_ID, this::processMessages, this);
            this.running = true;
            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            logger.info("close message consumer for {}...", getClass().getName());
            consumer.stop();
            System.exit(1);
        });
        this.jobThread = thread;
        this.jobThread.start();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("shutdown sequence service...");
        running = false;
        if (jobThread != null) {
            jobThread.interrupt();
            try {
                jobThread.join(5000);
            } catch (InterruptedException e) {
                logger.error("interrupt job thread failed", e);
            }
            jobThread = null;
        }
    }

    private boolean crash = false;

    // 错误处理
    @Override
    public void handleBatch(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer, MessageListenerContainer container, Runnable invokeListener) {
        logger.error("batch error!", thrownException);
        panic();
    }

    private void sendMessages(List<AbstractEvent> messages) {
        this.messageProducer.sendMessages(messages);
    }

    //线程安全 处理消息列表
    private synchronized void processMessages(List<AbstractEvent> messages) {
        if (!running || crash) {
            panic();
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("do sequence for {} messages...", messages.size());
        }
        long start = System.currentTimeMillis();
        List<AbstractEvent> sequenced = null;
        try {
            // 消息排序
            sequenced = this.sequenceHandler.sequenceMessages(this.messageTypes, this.sequence, messages);
        } catch (Throwable e) {
            logger.error("exception when do sequence", e);
            shutdown();
            panic();
            throw new Error(e);
        }

        if (logger.isInfoEnabled()) {
            long end = System.currentTimeMillis();
            logger.info("sequenced {} messages in {} ms. current sequence id: {}", messages.size(), (end - start),
                    this.sequence.get());
        }
        sendMessages(sequenced);
    }

    private void panic() {
        this.crash = true;
        this.running = false;
        System.exit(1);
    }
}