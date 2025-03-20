package org.zheng.message.event;

import jakarta.annotation.Nullable;
import org.zheng.message.AbstractMessage;

public class AbstractEvent extends AbstractMessage {
    public long sequenceId;
    //保证消息的连贯处理
    public long previousId;
    @Nullable
    public String uniqueId;
}
