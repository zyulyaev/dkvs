package ru.zyulyaev.ifmo.dkvs.message.normal;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 17.05.15.
 */
@MessageType("node")
public class NodeMessage extends Message {
    @MessageParam(1)
    private int index;

    NodeMessage() {}

    public NodeMessage(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
