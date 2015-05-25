package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.message.normal.PrepareOkMessage;
import ru.zyulyaev.ifmo.dkvs.message.recovery.RecoveryResponseMessage;
import ru.zyulyaev.ifmo.dkvs.server.NodeStatus;
import ru.zyulyaev.ifmo.dkvs.server.RemoteNode;

/**
 * Created by nikita on 26.05.15.
 */
public class RecoveryWorkflow extends Workflow {
    private final String nonce;
    private int recoveryReceived = 0;
    private RecoveryResponseMessage latest;

    public RecoveryWorkflow(WorkflowContext context, String nonce) {
        super(context);
        this.nonce = nonce;
    }

    @Override
    public NodeStatus getStatus() {
        return NodeStatus.RECOVERING;
    }

    @MessageProcessor(RecoveryResponseMessage.class)
    private void processRecoveryResponse(RecoveryResponseMessage message, RemoteNode node) {
        if (!nonce.equals(message.getNonce()))
            return;
        recoveryReceived++;
        if (message.getViewNumber() % context.getNodeCount() == message.getNodeIndex()
                && (latest == null || latest.getViewNumber() < message.getViewNumber()))
            latest = message;
        if (recoveryReceived * 2 > context.getNodeCount() && latest != null) {
            context.getLog().setEntries(parseLog(latest.getLog()));
            context.setViewNumber(latest.getViewNumber());
            context.setOpNumber(latest.getOpNumber());
            while (context.getCommitNumber() < latest.getCommitNumber())
                context.commit();
            for (int op = context.getCommitNumber(); op < context.getOpNumber(); ++op)
                context.sendMessageToPrimaryNode(new PrepareOkMessage(context.getViewNumber(), op, context.getNodeIndex()));
            context.switchWorkflow(new NormalReplicaWorkflow(context));
        }
    }

    @Override
    protected void configureTasks(TasksConfigurer configurer) {
        super.configureTasks(configurer);
        configurer.add(context.getIdleTimeout(), this::noRecovery);
    }

    private void noRecovery() {
        startRecovery();
    }
}
