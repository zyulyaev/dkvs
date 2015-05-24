package ru.zyulyaev.ifmo.dkvs.message.recovery;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 23.05.15.
 */
@MessageType("recovery")
public class RecoveryMessage extends Message {
    @MessageParam(1)
    private int nodeIndex;
    @MessageParam(2)
    private String nonce;

    RecoveryMessage() {}

    public RecoveryMessage(int nodeIndex, String nonce) {
        this.nodeIndex = nodeIndex;
        this.nonce = nonce;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public String getNonce() {
        return nonce;
    }
}
