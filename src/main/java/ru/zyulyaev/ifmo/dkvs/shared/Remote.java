package ru.zyulyaev.ifmo.dkvs.shared;

import ru.zyulyaev.ifmo.dkvs.MessageFormatter;
import ru.zyulyaev.ifmo.dkvs.message.Message;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nikita on 22.05.15.
 */
public abstract class Remote implements Closeable {
    private static final Logger logger = Logger.getLogger(Remote.class.getName());
    private BoundChannel outbound;

    public BoundChannel getOutbound() {
        return outbound;
    }

    public void setOutbound(BoundChannel outbound) {
        if (this.outbound != null && this.outbound != outbound) {
            try {
                this.outbound.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Unable to close old outbound channel", ex);
            }
        }
        this.outbound = outbound;
        outbound.setMessageProcessor(this::processIncomingMessage);
    }

    public void sendMessage(Message message) {
        outbound.sendMessage(MessageFormatter.format(message));
    }

    protected abstract void processIncomingMessage(String message);

    @Override
    public void close() throws IOException {
        outbound.close();
    }
}
