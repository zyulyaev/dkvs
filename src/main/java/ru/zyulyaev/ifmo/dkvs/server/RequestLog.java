package ru.zyulyaev.ifmo.dkvs.server;

import ru.zyulyaev.ifmo.dkvs.message.request.RequestMessage;

import java.util.Arrays;

/**
 * Created by nikita on 22.05.15.
 */
public class RequestLog {
    private static final int INITIAL_SIZE = 16;
    private RequestMessage[] entries = new RequestMessage[INITIAL_SIZE];

    private void ensureSize(int opNumber) {
        int size = entries.length;
        while (size <= opNumber)
            size = size + (size >> 1);
        if (size != entries.length)
            entries = Arrays.copyOf(entries, size);
    }

    public void set(int opNumber, RequestMessage message) {
        ensureSize(opNumber);
        entries[opNumber] = message;
    }

    public boolean contains(int opNumber) {
        return opNumber < entries.length && entries[opNumber] != null;
    }

    public RequestMessage get(int opNumber) {
        return entries[opNumber];
    }

    public RequestMessage[] getEntries() {
        return entries;
    }

    public void setEntries(RequestMessage[] entries) {
        this.entries = entries;
    }
}
