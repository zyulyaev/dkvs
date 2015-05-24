package ru.zyulyaev.ifmo.dkvs;

import ru.zyulyaev.ifmo.dkvs.message.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nikita on 17.05.15.
 */
public class MessageRouter<C> {
    private static final Logger logger = Logger.getLogger(MessageRouter.class.getName());
    private final Map<Class<?>, BiConsumer<?, ? super C>> routes;
    private final MessageFormatter formatter;

    private MessageRouter(Map<Class<?>, BiConsumer<?, ? super C>> routes, MessageFormatter formatter) {
        this.routes = routes;
        this.formatter = formatter;
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatch(Message message, C context, BiConsumer<T, ? super C> consumer) {
        if (consumer == null) {
            logger.warning("Command " + message.getType() + " not found. Skipping.");
        } else {
            consumer.accept((T) message, context);
        }
    }

    public void dispatch(String message, C context) {
        try {
            dispatch(formatter.parse(message), context);
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    public void dispatch(Message message, C context) {
        dispatch(message, context, routes.get(message.getClass()));
    }

    public static <C> Builder<C> builder() {
        return new Builder<>();
    }

    public static class Builder<C> {
        private final Map<Class<?>, BiConsumer<?, ? super C>> routes = new HashMap<>();
        private final MessageFormatter.Builder formatterBuilder = MessageFormatter.builder();

        public <T extends Message> Builder<C> add(Class<T> clazz, BiConsumer<? super T, ? super C> callback) {
            formatterBuilder.register(clazz);
            this.routes.put(clazz, callback);
            return this;
        }

        public MessageRouter<C> build() {
            return new MessageRouter<>(new HashMap<>(routes), formatterBuilder.build());
        }
    }
}
