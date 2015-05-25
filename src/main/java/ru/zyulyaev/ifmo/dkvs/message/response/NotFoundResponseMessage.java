package ru.zyulyaev.ifmo.dkvs.message.response;

import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("not_found")
public class NotFoundResponseMessage extends ResponseMessage {
    NotFoundResponseMessage() {}

    public NotFoundResponseMessage(int requestId, int viewNumber) {
        super(requestId, viewNumber);
    }
}
