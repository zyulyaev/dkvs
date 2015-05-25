package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.server.ClientTableEntry;
import ru.zyulyaev.ifmo.dkvs.server.RequestLog;
import ru.zyulyaev.ifmo.dkvs.server.RemoteClient;

/**
 * Created by nikita on 23.05.15.
 */
public interface WorkflowContext {
    int getNodeIndex();

    int getNodeCount();

    boolean isPrimaryNode();

    int getViewNumber();

    void setViewNumber(int viewNumber);

    int getOpNumber();

    void setOpNumber(int opNumber);

    int getCommitNumber();

    RequestLog getLog();

    ClientTableEntry getOrCreateClientEntry(int clientId);

    void identifyAsNode(RemoteClient client, int nodeId);

    void sendMessageToNode(int nodeId, Message message);

    void sendMessageToOtherNodes(Message message);

    void sendMessageToPrimaryNode(Message message);

    void switchWorkflow(Workflow workflow);

    void commit();

    int getIdleTimeout();

    // todo for debug purposes only
    void die();
}
