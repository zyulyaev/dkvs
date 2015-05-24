package ru.zyulyaev.ifmo.dkvs.message.request;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;

/**
 * Created by nikita on 22.05.15.
 */
public abstract class RequestMessage extends Message {
    @MessageParam(1)
    private int clientId;
    @MessageParam(2)
    private int requestId;

    RequestMessage() {}

    public RequestMessage(int clientId, int requestId) {
        this.clientId = clientId;
        this.requestId = requestId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getRequestId() {
        return requestId;
    }
}
