package ru.zyulyaev.ifmo.dkvs.message.viewchange;

import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;

/**
 * Created by nikita on 22.05.15.
 */
@MessageType("start_view")
public class StartViewMessage extends Message {
    @MessageParam(1)
    private int viewNumber;
    @MessageParam(2)
    private String log;
    @MessageParam(3)
    private int opNumber;
    @MessageParam(4)
    private int commitNumber;

    StartViewMessage() {}

    public StartViewMessage(int viewNumber, String log, int opNumber, int commitNumber) {
        this.viewNumber = viewNumber;
        this.log = log;
        this.opNumber = opNumber;
        this.commitNumber = commitNumber;
    }

    public int getViewNumber() {
        return viewNumber;
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
}
