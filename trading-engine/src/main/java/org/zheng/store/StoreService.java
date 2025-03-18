package org.zheng.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zheng.db.DbTemplate;
import org.zheng.message.event.AbstractEvent;
import org.zheng.messaging.MessageTypes;
import org.zheng.model.support.EntitySupport;
import org.zheng.model.trade.EventEntity;
import org.zheng.support.LoggerSupport;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional
public class StoreService extends LoggerSupport {
    @Autowired
    MessageTypes messageTypes;

    @Autowired
    DbTemplate dbTemplate;

    //加载事件
    public List<AbstractEvent> loadEventsFromDb(long lastEventId){
        List<EventEntity> events = this.dbTemplate.from(EventEntity.class).where("sequenceId > ?", lastEventId).orderBy("sequenceId").limit(10000).list();
        return events.stream().map(event -> (AbstractEvent) messageTypes.deserialize(event.data)).collect(Collectors.toList());
    }

    //插入事件
    public void insertIgnore(List<? extends EntitySupport> list) {
        dbTemplate.insertIgnore(list);
    }
}
