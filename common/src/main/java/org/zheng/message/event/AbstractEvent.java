package org.zheng.message.event;

import jakarta.annotation.Nullable;
import org.zheng.message.AbstractMessage;

public class AbstractEvent extends AbstractMessage {
    public long sequenceId;
    public long previousId;
    @Nullable
    public String uniqueId;
}
