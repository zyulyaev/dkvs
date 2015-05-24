package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.MessageFormatter;
import ru.zyulyaev.ifmo.dkvs.message.normal.CommitMessage;
import ru.zyulyaev.ifmo.dkvs.message.normal.PrepareMessage;
import ru.zyulyaev.ifmo.dkvs.message.normal.PrepareOkMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.DeleteRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.GetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.RequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.SetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.server.ClientTableEntry;
import ru.zyulyaev.ifmo.dkvs.server.RemoteClient;
import ru.zyulyaev.ifmo.dkvs.server.RemoteNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nikita on 23.05.15.
 */
public class NormalPrimaryWorkflow extends BaseNormalWorkflow {
    private final Map<Integer, Integer> oksReceived = new HashMap<>();
    private TimerTask idleTask;

    public NormalPrimaryWorkflow(WorkflowContext context) {
        super(context);
    }

    @MessageProcessor({GetRequestMessage.class, SetRequestMessage.class, DeleteRequestMessage.class})
    private void processClientRequest(RequestMessage requestMessage, RemoteClient client) {
        int requestId = requestMessage.getRequestId();
        ClientTableEntry clientEntry = context.getOrCreateClientEntry(requestMessage.getClientId());
        clientEntry.setRemoteClient(client);
        if (requestId <= clientEntry.getLastRequestId()) {
            if (requestId == clientEntry.getLastRequestId() && clientEntry.getLastResponse() != null)
                client.sendMessage(clientEntry.getLastResponse());
            return;
        }
        clientEntry.setLastRequestId(requestId);
        int requestOpNumber = context.getOpNumber();
        oksReceived.put(requestOpNumber, 0);
        context.setOpNumber(requestOpNumber + 1);
        context.getLog().set(requestOpNumber, requestMessage);
        PrepareMessage prepareMessage = new PrepareMessage(
                context.getViewNumber(),
                MessageFormatter.format(requestMessage),
                requestOpNumber,
                context.getCommitNumber()
        );
        context.sendMessageToOtherNodes(prepareMessage);
        idleTask.reset();
    }

    @MessageProcessor(PrepareOkMessage.class)
    private void processPrepareOk(PrepareOkMessage message, RemoteNode node) {
        int requestOpNumber = message.getOpNumber();
        if (!oksReceived.containsKey(requestOpNumber))
            return;
        int oks = oksReceived.get(requestOpNumber) + 1;
        oksReceived.put(requestOpNumber, oks);
        while (oksReceived.getOrDefault(context.getCommitNumber(), 0) * 2 + 1 >= context.getNodeCount()) {
            oksReceived.remove(context.getCommitNumber());
            context.commit();
        }
    }

    @Override
    protected void configureTasks(TasksConfigurer configurer) {
        super.configureTasks(configurer);
        idleTask = configurer.add(context.getIdleTimeout() / 2, this::onIdle);
    }

    private void onIdle() {
        context.sendMessageToOtherNodes(new CommitMessage(context.getViewNumber(), context.getCommitNumber()));
    }
}
