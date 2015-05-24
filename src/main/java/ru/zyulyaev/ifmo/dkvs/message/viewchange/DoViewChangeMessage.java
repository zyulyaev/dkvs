package ru.zyulyaev.ifmo.dkvs.message.viewchange;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("do_view_change")
public class DoViewChangeMessage extends Message {
    @MessageParam(1)
    private int viewNumber;
    @MessageParam(2)
    private String log;
    @MessageParam(3)
    private int lastNormalViewNumber;
    @MessageParam(4)
    private int opNumber;
    @MessageParam(5)
    private int commitNumber;
    @MessageParam(6)
    private int nodeIndex;

    DoViewChangeMessage() {}

    public DoViewChangeMessage(int viewNumber, String log, int lastNormalViewNumber, int opNumber, int commitNumber, int nodeIndex) {
        this.viewNumber = viewNumber;
        this.log = log;
        this.lastNormalViewNumber = lastNormalViewNumber;
        this.opNumber = opNumber;
        this.commitNumber = commitNumber;
        this.nodeIndex = nodeIndex;

    }

    public int getViewNumber() {
        return viewNumber;
    }

    public String getLog() {
        return log;
    }

    public int getLastNormalViewNumber() {
        return lastNormalViewNumber;
    }

    public int getOpNumber() {
        return opNumber;
    }

    public int getCommitNumber() {
        return commitNumber;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }
}
