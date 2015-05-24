package ru.zyulyaev.ifmo.dkvs.server;

import ru.zyulyaev.ifmo.dkvs.message.Message;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nikita on 23.05.15.
 */
public class ClientTableEntry {
    private static final Logger logger = Logger.getLogger(ClientTableEntry.class.getName());
    private final int clientId;
    private RemoteClient remoteClient;
    private int lastRequestId = -1;
    private Message lastResponse;

    public ClientTableEntry(int clientId) {
        this.clientId = clientId;
    }

    public void setRemoteClient(RemoteClient remoteClient) {
        if (this.remoteClient != null && this.remoteClient != remoteClient) {
            logger.warning("Client with same id(" + clientId + ") arrived, closing previous connection");
            try {
                this.remoteClient.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Connection close failed", e);
            }
        }
        this.remoteClient = remoteClient;
    }

    public RemoteClient getRemoteClient() {
        return remoteClient;
    }

    public int getLastRequestId() {
        return lastRequestId;
    }

    public void setLastRequestId(int lastRequestId) {
        this.lastRequestId = lastRequestId;
        this.lastResponse = null;
    }

    public Message getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Message lastResponse) {
        this.lastResponse = lastResponse;
    }
}
