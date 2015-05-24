package ru.zyulyaev.ifmo.dkvs.message.viewchange;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("start_view_change")
public class StartViewChangeMessage extends Message {
    @MessageParam(1)
    private int viewNumber;
    @MessageParam(2)
    private int nodeIndex;

    StartViewChangeMessage() {}

    public StartViewChangeMessage(int viewNumber, int nodeIndex) {
        this.viewNumber = viewNumber;
        this.nodeIndex = nodeIndex;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }
}
