package org.zheng.sequencer;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zheng.message.event.AbstractEvent;
import org.zheng.messaging.MessageTypes;
import org.zheng.model.trade.EventEntity;
import org.zheng.model.trade.UniqueEventEntity;
import org.zheng.support.AbstractDbService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Transactional(rollbackFor = Throwable.class)
public class SequenceHandler extends AbstractDbService {

    private long lastTimestamp = 0;

    // 消息定序，并批量写入数据库，定序前输入事件只包含一个可选的uniqueId，忽略sequenceId和previousId
    public List<AbstractEvent> sequenceMessages(final MessageTypes messageTypes, final AtomicLong sequence, final List<AbstractEvent> messages) throws Exception {
        final long t = System.currentTimeMillis();
        if (t < this.lastTimestamp) {
            logger.warn("[Sequence] current time {} is turned back from {}!", t, this.lastTimestamp);
        } else {
            this.lastTimestamp = t;
        }
        List<UniqueEventEntity> uniques = null;
        Set<String> uniqueKeys = null;
        List<AbstractEvent> sequencedMessages = new ArrayList<>(messages.size());
        List<EventEntity> events = new ArrayList<>(messages.size());
        for (AbstractEvent message : messages) {
            UniqueEventEntity unique = null;
            final String uniqueId = message.uniqueId;
            if (uniqueId != null) {
                // uniqueId已经存在了
                if ((uniqueKeys != null && uniqueKeys.contains(uniqueId)) || db.fetch(UniqueEventEntity.class, uniqueId) != null) {
                    logger.warn("ignore processed unique message: {}", message);
                    continue;
                }
                // 保存unique event和id
                unique = new UniqueEventEntity();
                unique.uniqueId = uniqueId;
                unique.createTime = message.createTime;
                if (uniques == null) {
                    uniques = new ArrayList<>();
                }
                uniques.add(unique);
                if (uniqueKeys == null) {
                    uniqueKeys = new HashSet<>();
                }
                uniqueKeys.add(uniqueId);
                logger.info("unique event {} sequenced.", uniqueId);
            }

            final long previousId = sequence.get();
            final long currentId = sequence.incrementAndGet();

            // 先设置message的sequenceId / previouseId / createTime，再序列化并落库:
            message.sequenceId = currentId;
            message.previousId = previousId;
            message.createTime= this.lastTimestamp;

            // 如果此消息关联了UniqueEvent，给UniqueEvent加上相同的sequenceId：
            if (unique != null) {
                unique.sequenceId = message.sequenceId;
            }

            // 创建event并保存至db
            EventEntity event = new EventEntity();
            event.previousId = previousId;
            event.sequenceId = currentId;

            event.data = messageTypes.serialize(message);
            event.createTime = this.lastTimestamp;
            events.add(event);

            sequencedMessages.add(message);
        }

        if (uniques != null) {
            db.insert(uniques);
        }
        db.insert(events);
        return sequencedMessages;
    }

    public long getMaxSequenceId() {
        EventEntity last = db.from(EventEntity.class).orderBy("sequenceId").desc().first();
        if (last == null) {
            logger.info("no max sequenceId found. set max sequenceId = 0.");
            return 0;
        }
        this.lastTimestamp = last.createTime;
        logger.info("find max sequenceId = {}, last timestamp = {}", last.sequenceId, this.lastTimestamp);
        return last.sequenceId;
    }
}