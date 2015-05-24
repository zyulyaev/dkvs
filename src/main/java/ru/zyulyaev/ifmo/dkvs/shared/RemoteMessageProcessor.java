package ru.zyulyaev.ifmo.dkvs.shared;

/**
 * Created by nikita on 23.05.15.
 */
public interface RemoteMessageProcessor<O> {
    void process(String message, O origin);
}
