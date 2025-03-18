package org.zheng.messaging;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.zheng.message.AbstractMessage;
import org.zheng.support.LoggerSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

//接受和发送消息
@Component
public class MessagingFactory extends LoggerSupport {

    @Autowired
    private MessageTypes messageTypes;

    //用于将消息发送到kafka
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    //消费者监听容器
    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory;

    //用于管理kafka的配置和操作
    @Autowired
    private KafkaAdmin kafkaAdmin;

    //Topic（主题） 是一个核心概念，用于组织和分类消息。它是消息发布的逻辑容器，生产者将消息发送到特定的 Topic，消费者从 Topic 中读取消息。
    //检查并自动创建topic
    @PostConstruct
    public void init() throws InterruptedException, ExecutionException {
        logger.info("init kafka admin...");
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            //查询当前topic
            Set<String> allTopics = client.listTopics().names().get();
            // 自动创建不存在的topic，从messaging中:
            List<NewTopic> newTopics = new ArrayList<>();
            for (Messaging.Topic topic : Messaging.Topic.values()) {
                if (!allTopics.contains(topic.name())) {
                    newTopics.add(new NewTopic(topic.name(), topic.getPartitions(), (short) 1));
                }
            }
            if (!newTopics.isEmpty()) {
                client.createTopics(newTopics);
                newTopics.forEach(t -> {
                    logger.warn("auto-create kafka topics when init MessagingFactory: {}", t);
                });
            }
        }
        logger.info("init MessagingFactory ok.");
    }

    //创建消息生产者，向topic发消息
    public <T extends AbstractMessage> MessageProducer<T> createMessageProducer(Messaging.Topic topic, Class<T> messageClass) {
        logger.info("try create message producer for topic {}...", topic);
        final String name = topic.name();
        return new MessageProducer<>() {
            @Override
            public void sendMessage(AbstractMessage message) {
                //序列化
                kafkaTemplate.send(name, messageTypes.serialize(message));
            }
        };
    }

    //批量消息消费者，从指定Topic获取消息
    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(Messaging.Topic topic, String groupId, BatchMessageHandler<T> messageHandler) {
        return createBatchMessageListener(topic, groupId, messageHandler, null);
    }

    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(Messaging.Topic topic, String groupId, BatchMessageHandler<T> messageHandler, CommonErrorHandler errorHandler) {
        logger.info("try create batch message listener for topic {}: group id = {}...", topic, groupId);
        //消费者监听器容器
        ConcurrentMessageListenerContainer<String, String> listenerContainer = listenerContainerFactory.createListenerContainer(new KafkaListenerEndpointAdapter() {
            @Override
            public String getGroupId() {
                return groupId;
            }

            @Override
            public Collection<String> getTopics() {
                return List.of(topic.name());
            }
        });
        listenerContainer.setupMessageListener(new BatchMessageListener<String, String>() {
            @Override
            @SuppressWarnings("unchecked")
            //批量消息处理
            public void onMessage(List<ConsumerRecord<String, String>> data) {
                List<T> messages = new ArrayList<>(data.size());
                for (ConsumerRecord<String, String> record : data) {
                    AbstractMessage message = messageTypes.deserialize(record.value());
                    messages.add((T) message);
                }
                //调用处理器
                messageHandler.processMessages(messages);
            }
        });
        if (errorHandler != null) {
            listenerContainer.setCommonErrorHandler(errorHandler);
        }
        listenerContainer.start();
        return listenerContainer::stop;
    }
}

class KafkaListenerEndpointAdapter implements KafkaListenerEndpoint {

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getGroupId() {
        return null;
    }

    @Override
    public String getGroup() {
        return null;
    }

    @Override
    public Collection<String> getTopics() {
        return List.of();
    }

    @Override
    public Pattern getTopicPattern() {
        return null;
    }

    @Override
    public String getClientIdPrefix() {
        return null;
    }

    @Override
    public Integer getConcurrency() {
        return Integer.valueOf(1);
    }

    @Override
    public Boolean getAutoStartup() {
        return Boolean.FALSE;
    }

    @Override
    public void setupListenerContainer(MessageListenerContainer listenerContainer, MessageConverter messageConverter) {
    }

    @Override
    public TopicPartitionOffset[] getTopicPartitionsToAssign() {
        return null;
    }

    @Override
    public boolean isSplitIterables() {
        return false;
    }
}
