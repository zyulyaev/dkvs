package ru.zyulyaev.ifmo.dkvs.message.response;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("value")
public class ValueResponseMessage extends ResponseMessage {
    @MessageParam(3)
    private String key;
    @MessageParam(4)
    private String value;

    ValueResponseMessage() {
    }

    public ValueResponseMessage(int requestId, int viewNumber, String key, String value) {
        super(requestId, viewNumber);
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
