package ru.zyulyaev.ifmo.dkvs.message.normal;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 17.05.15.
 */
@MessageType("prepare")
public class PrepareMessage extends Message {
    @MessageParam(1)
    private int viewNumber;
    @MessageParam(2)
    private String clientMessage;
    @MessageParam(3)
    private int opNumber;
    @MessageParam(4)
    private int commitNumber;

    PrepareMessage() {
    }

    public PrepareMessage(int viewNumber, String clientMessage, int opNumber, int commitNumber) {
        this.viewNumber = viewNumber;
        this.clientMessage = clientMessage;
        this.opNumber = opNumber;
        this.commitNumber = commitNumber;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public String getClientMessage() {
        return clientMessage;
    }

    public int getOpNumber() {
        return opNumber;
    }

    public int getCommitNumber() {
        return commitNumber;
    }
}
