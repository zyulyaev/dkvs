package ru.zyulyaev.ifmo.dkvs.message.response;

import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("stored")
public class StoredResponseMessage extends ResponseMessage {
    StoredResponseMessage() {}

    public StoredResponseMessage(int requestId, int viewNumber) {
        super(requestId, viewNumber);
    }
}
