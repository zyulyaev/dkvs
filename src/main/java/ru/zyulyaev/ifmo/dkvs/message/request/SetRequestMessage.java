package ru.zyulyaev.ifmo.dkvs.message.request;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 17.05.15.
 */
@MessageType("set")
public class SetRequestMessage extends RequestMessage {
    @MessageParam(3)
    private String key;
    @MessageParam(4)
    private String value;

    SetRequestMessage() {}

    public SetRequestMessage(int clientId, int requestId, String key, String value) {
        super(clientId, requestId);
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
