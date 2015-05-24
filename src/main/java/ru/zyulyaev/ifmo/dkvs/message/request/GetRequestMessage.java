package ru.zyulyaev.ifmo.dkvs.message.request;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 17.05.15.
 */
@MessageType("get")
public class GetRequestMessage extends RequestMessage {
    @MessageParam(3)
    private String key;

    public String getKey() {
        return key;
    }

    GetRequestMessage() {}

    public GetRequestMessage(int clientId, int requestId, String key) {
        super(clientId, requestId);
        this.key = key;
    }
}
