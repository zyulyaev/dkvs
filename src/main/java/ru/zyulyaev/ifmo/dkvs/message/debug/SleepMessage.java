package ru.zyulyaev.ifmo.dkvs.message.debug;

import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 25.05.15.
 */
@MessageType("sleep")
public class SleepMessage extends Message {
    @MessageParam(1)
    private int timeout;

    SleepMessage() {}

    public SleepMessage(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }
}
