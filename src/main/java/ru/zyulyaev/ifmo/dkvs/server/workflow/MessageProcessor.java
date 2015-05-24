package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.message.Message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nikita on 23.05.15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MessageProcessor {
    Class<? extends Message>[] value();
}
