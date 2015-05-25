package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.MessageFormatter;
import ru.zyulyaev.ifmo.dkvs.message.normal.PrepareOkMessage;
import ru.zyulyaev.ifmo.dkvs.message.viewchange.DoViewChangeMessage;
import ru.zyulyaev.ifmo.dkvs.message.viewchange.StartViewChangeMessage;
import ru.zyulyaev.ifmo.dkvs.message.viewchange.StartViewMessage;
import ru.zyulyaev.ifmo.dkvs.server.NodeStatus;
import ru.zyulyaev.ifmo.dkvs.server.RemoteNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by nikita on 23.05.15.
 */
public class ViewChangeWorkflow extends Workflow {
    private final int lastNormalViewNumber;
    private final List<DoViewChangeMessage> doViewChangeMessages = new ArrayList<>();
    private int sameViewChangeRequests;

    public ViewChangeWorkflow(WorkflowContext context, int lastNormalViewNumber, boolean byRequest) {
        super(context);
        this.lastNormalViewNumber = lastNormalViewNumber;
        this.sameViewChangeRequests = byRequest ? 1 : 0;
    }

    @Override
    public NodeStatus getStatus() {
        return NodeStatus.VIEW_CHANGE;
    }

    @MessageProcessor(StartViewChangeMessage.class)
    private void processStartViewChange(StartViewChangeMessage message, RemoteNode node) {
        int requestViewNumber = message.getViewNumber();
        if (requestViewNumber == context.getViewNumber()) {
            sameViewChangeRequests++;
            if (sameViewChangeRequests * 2 + 1 >= context.getNodeCount()
                    && sameViewChangeRequests * 2 - 1 < context.getNodeCount()) {
                DoViewChangeMessage doViewChangeMessage = new DoViewChangeMessage(
                        context.getViewNumber(),
                        MessageFormatter.formatLog(context.getLog()),
                        lastNormalViewNumber,
                        context.getOpNumber(),
                        context.getCommitNumber(),
                        context.getNodeIndex()
                );
                if (!context.isPrimaryNode()) {
                    context.sendMessageToPrimaryNode(doViewChangeMessage);
                } else {
                    doViewChangeMessages.add(doViewChangeMessage);
                }
            }
        } else if (requestViewNumber > context.getViewNumber()) {
            startViewChange(requestViewNumber, lastNormalViewNumber, true);
        }
    }

    @MessageProcessor(DoViewChangeMessage.class)
    private void processDoViewChange(DoViewChangeMessage message, RemoteNode node) {
        if (message.getViewNumber() == context.getViewNumber()) {
            doViewChangeMessages.add(message);
            if (doViewChangeMessages.size() * 2 > context.getNodeCount()) {
                DoViewChangeMessage bestLog = doViewChangeMessages.stream()
                        .max(Comparator.comparingInt(DoViewChangeMessage::getLastNormalViewNumber)
                                .thenComparing(DoViewChangeMessage::getOpNumber))
                        .get();
                context.getLog().setEntries(parseLog(bestLog.getLog()));
                context.setOpNumber(bestLog.getOpNumber());
                int newCommitNumber = doViewChangeMessages.stream()
                        .mapToInt(DoViewChangeMessage::getCommitNumber)
                        .max()
                        .getAsInt();
                while (context.getCommitNumber() < newCommitNumber)
                    context.commit();
                context.sendMessageToOtherNodes(new StartViewMessage(
                        context.getViewNumber(),
                        MessageFormatter.formatLog(context.getLog()),
                        context.getOpNumber(),
                        context.getCommitNumber()
                ));
                context.switchWorkflow(new NormalPrimaryWorkflow(context));
            }
        } else if (message.getViewNumber() > context.getViewNumber()) {
            startViewChange(message.getViewNumber(), lastNormalViewNumber, true);
        }
    }

    @MessageProcessor(StartViewMessage.class)
    private void processStartView(StartViewMessage message, RemoteNode node) {
        if (message.getViewNumber() < context.getViewNumber())
            return;
        context.setViewNumber(message.getViewNumber());
        context.getLog().setEntries(parseLog(message.getLog()));
        context.setOpNumber(message.getOpNumber());
        while (context.getCommitNumber() < message.getCommitNumber())
            context.commit();
        for (int op = context.getCommitNumber(); op < context.getOpNumber(); ++op)
            context.sendMessageToPrimaryNode(new PrepareOkMessage(context.getViewNumber(), op, context.getNodeIndex()));
        context.switchWorkflow(new NormalReplicaWorkflow(context));
    }

    @Override
    protected void configureTasks(TasksConfigurer configurer) {
        super.configureTasks(configurer);
        configurer.add(context.getIdleTimeout(), this::onViewChangeFailed);
    }

    private void onViewChangeFailed() {
        startViewChange(context.getViewNumber() + 1, lastNormalViewNumber, false);
    }
}
