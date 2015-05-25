package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.message.normal.CommitMessage;
import ru.zyulyaev.ifmo.dkvs.message.normal.PrepareMessage;
import ru.zyulyaev.ifmo.dkvs.message.normal.PrepareOkMessage;
import ru.zyulyaev.ifmo.dkvs.message.recovery.RecoveryMessage;
import ru.zyulyaev.ifmo.dkvs.message.recovery.RecoveryResponseMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.RequestMessage;
import ru.zyulyaev.ifmo.dkvs.server.RemoteNode;

/**
 * Created by nikita on 23.05.15.
 */
public class NormalReplicaWorkflow extends BaseNormalWorkflow {
    private int primaryCommitNumber;
    private TimerTask primaryIdleTask;

    public NormalReplicaWorkflow(WorkflowContext context) {
        super(context);
    }

    @MessageProcessor(PrepareMessage.class)
    private void processPrepareMessage(PrepareMessage prepareMessage, RemoteNode node) {
        int viewNumber = context.getViewNumber();
        if (prepareMessage.getViewNumber() != viewNumber)
            return;
        int requestOpNumber = prepareMessage.getOpNumber(); // todo state transfer?
        RequestMessage requestMessage = (RequestMessage) parseMessage(prepareMessage.getClientMessage());
        context.getLog().set(requestOpNumber, requestMessage);
        while (context.getLog().contains(context.getOpNumber())) {
            int opNumber = context.getOpNumber();
            context.sendMessageToPrimaryNode(new PrepareOkMessage(viewNumber, opNumber, context.getNodeIndex()));
            context.setOpNumber(opNumber + 1);
        }
        updateCommitNumber(prepareMessage.getCommitNumber());
        primaryIdleTask.reset();
    }

    @MessageProcessor(CommitMessage.class)
    private void processCommitMessage(CommitMessage commitMessage, RemoteNode node) {
        if (commitMessage.getViewNumber() != context.getViewNumber())
            return;
        updateCommitNumber(commitMessage.getCommitNumber());
        primaryIdleTask.reset();
    }

    @MessageProcessor(RecoveryMessage.class)
    private void processRecovery(RecoveryMessage recoveryMessage, RemoteNode node) {
        node.sendMessage(new RecoveryResponseMessage(context.getViewNumber(), recoveryMessage.getNonce(), "null", 0, 0, context.getNodeIndex()));
    }

    private void updateCommitNumber(int commitNumber) {
        primaryCommitNumber = Math.max(primaryCommitNumber, commitNumber);
        while (context.getCommitNumber() < context.getOpNumber() && context.getCommitNumber() < primaryCommitNumber)
            context.commit();
    }

    @Override
    protected void configureTasks(TasksConfigurer configurer) {
        super.configureTasks(configurer);
        primaryIdleTask = configurer.add(context.getIdleTimeout(), this::primaryIdle);
    }

    private void primaryIdle() {
        startViewChange(context.getViewNumber() + 1, context.getViewNumber(), false);
    }
}
