package org.zheng.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zheng.support.LoggerSupport;

//接受和发送消息
@Component
public class MessagingFactory extends LoggerSupport {

    @Autowired
    private MessageTypes messageTypes;
}
