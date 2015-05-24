package ru.zyulyaev.ifmo.dkvs.message.recovery;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 23.05.15.
 */
@MessageType("recovery_response")
public class RecoveryResponseMessage extends Message {
    @MessageParam(1)
    private int viewNumber;
    @MessageParam(2)
    private String nonce;
    @MessageParam(3)
    private String log;
    @MessageParam(4)
    private int opNumber;
    @MessageParam(5)
    private int commitNumber;
    @MessageParam(6)
    private int nodeIndex;

    RecoveryResponseMessage() {}

    public RecoveryResponseMessage(int viewNumber, String nonce, String log, int opNumber, int commitNumber, int nodeIndex) {
        this.viewNumber = viewNumber;
        this.nonce = nonce;
        this.log = log;
        this.opNumber = opNumber;
        this.commitNumber = commitNumber;
        this.nodeIndex = nodeIndex;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public String getNonce() {
        return nonce;
    }

    public String getLog() {
        return log;
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
