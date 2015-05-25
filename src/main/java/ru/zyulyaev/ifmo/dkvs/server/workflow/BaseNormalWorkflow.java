package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.message.viewchange.StartViewChangeMessage;
import ru.zyulyaev.ifmo.dkvs.server.NodeStatus;
import ru.zyulyaev.ifmo.dkvs.server.RemoteNode;

/**
 * Created by nikita on 23.05.15.
 */
public class BaseNormalWorkflow extends Workflow {
    protected BaseNormalWorkflow(WorkflowContext context) {
        super(context);
    }

    @Override
    public NodeStatus getStatus() {
        return NodeStatus.NORMAL;
    }

    @MessageProcessor(StartViewChangeMessage.class)
    private void processStartViewChange(StartViewChangeMessage message, RemoteNode node) {
        int requestViewNumber = message.getViewNumber();
        if (requestViewNumber > context.getViewNumber())
            startViewChange(requestViewNumber, context.getViewNumber(), true);
    }
}
