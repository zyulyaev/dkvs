package ru.zyulyaev.ifmo.dkvs.message.normal;

import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 23.05.15.
 */
@MessageType("commit")
public class CommitMessage extends Message {
    @MessageParam(1)
    private int viewNumber;
    @MessageParam(2)
    private int commitNumber;

    CommitMessage() {}

    public CommitMessage(int viewNumber, int commitNumber) {
        this.viewNumber = viewNumber;
        this.commitNumber = commitNumber;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public int getCommitNumber() {
        return commitNumber;
    }
}
