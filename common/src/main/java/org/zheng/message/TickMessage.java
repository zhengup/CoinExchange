package org.zheng.message;

import org.zheng.model.quotation.TickEntity;

import java.util.List;


public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;

}
