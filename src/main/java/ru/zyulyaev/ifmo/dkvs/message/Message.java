package ru.zyulyaev.ifmo.dkvs.message;

import ru.zyulyaev.ifmo.dkvs.MessageFormatter;

public abstract class Message {
    public String getType() {
        return getClass().getAnnotation(MessageType.class).value();
    }

    @Override
    public String toString() {
        return MessageFormatter.format(this);
    }
}
