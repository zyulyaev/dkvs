package ru.zyulyaev.ifmo.dkvs.server;

import ru.zyulyaev.ifmo.dkvs.shared.BoundChannel;
import ru.zyulyaev.ifmo.dkvs.shared.Remote;
import ru.zyulyaev.ifmo.dkvs.shared.RemoteMessageProcessor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nikita on 22.05.15.
 */
public class RemoteNode extends Remote {
    private static final Logger logger = Logger.getLogger(RemoteNode.class.getName());
    private final int index;
    private final RemoteMessageProcessor<? super RemoteNode> messageProcessor;
    private BoundChannel inbound;

    public RemoteNode(int index, RemoteMessageProcessor<? super RemoteNode> messageProcessor) {
        this.index = index;
        this.messageProcessor = messageProcessor;
    }

    public void setInbound(BoundChannel inbound) {
        if (this.inbound != null && this.inbound != inbound) {
            try {
                this.inbound.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Unable to close old inbound channel", ex);
            }
        }
        this.inbound = inbound;
        inbound.setMessageProcessor(this::processIncomingMessage);
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected void processIncomingMessage(String message) {
        messageProcessor.process(message, this);
    }
}
