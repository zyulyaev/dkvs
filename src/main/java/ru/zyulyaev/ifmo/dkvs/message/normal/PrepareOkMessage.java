package ru.zyulyaev.ifmo.dkvs.message.normal;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("prepare_ok")
public class PrepareOkMessage extends Message {
    @MessageParam(1)
    private int viewNumber;
    @MessageParam(2)
    private int opNumber;
    @MessageParam(3)
    private int nodeIndex;

    PrepareOkMessage() {
    }

    public PrepareOkMessage(int viewNumber, int opNumber, int nodeIndex) {
        this.viewNumber = viewNumber;
        this.opNumber = opNumber;
        this.nodeIndex = nodeIndex;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public int getOpNumber() {
        return opNumber;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }
}
