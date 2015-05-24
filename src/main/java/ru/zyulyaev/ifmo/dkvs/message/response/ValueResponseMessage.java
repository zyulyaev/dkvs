package ru.zyulyaev.ifmo.dkvs.message.response;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("value")
public class ValueResponseMessage extends Message {
    @MessageParam(1)
    private String key;
    @MessageParam(2)
    private String value;

    ValueResponseMessage() {
    }

    public ValueResponseMessage(String key, String value) {
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
