package ru.zyulyaev.ifmo.dkvs.message.response;

import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageParam;

/**
 * Created by nikita on 26.05.15.
 */
public class ResponseMessage extends Message {
    @MessageParam(1)
    private int requestId;
    @MessageParam(2)
    private int viewNumber;

    ResponseMessage() {}

    protected ResponseMessage(int requestId, int viewNumber) {
        this.requestId = requestId;
        this.viewNumber = viewNumber;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getViewNumber() {
        return viewNumber;
    }
}
