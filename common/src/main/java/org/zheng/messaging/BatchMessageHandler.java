package org.zheng.messaging;

import org.zheng.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {
    void processMessages(List<T> messages);
}
