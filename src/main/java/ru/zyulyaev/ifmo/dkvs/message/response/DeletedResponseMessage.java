package ru.zyulyaev.ifmo.dkvs.message.response;

import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("deleted")
public class DeletedResponseMessage extends ResponseMessage {
    DeletedResponseMessage() {}

    public DeletedResponseMessage(int requestId, int viewNumber) {
        super(requestId, viewNumber);
    }
}
