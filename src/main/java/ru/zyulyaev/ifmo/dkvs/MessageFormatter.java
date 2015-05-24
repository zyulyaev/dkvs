package ru.zyulyaev.ifmo.dkvs;

import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.MessageParam;
import ru.zyulyaev.ifmo.dkvs.message.MessageType;
import ru.zyulyaev.ifmo.dkvs.message.request.RequestMessage;
import ru.zyulyaev.ifmo.dkvs.server.RequestLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nikita on 17.05.15.
 */
public class MessageFormatter {
    private static final Logger logger = Logger.getLogger(MessageFormatter.class.getName());

    private final Map<String, Class<? extends Message>> classes;

    private MessageFormatter(Map<String, Class<? extends Message>> classes) {
        this.classes = classes;
    }

    public Message parse(String message) {
        List<String> parts = splitMessage(message);
        String type = parts.get(0);
        if (!classes.containsKey(type)) {
            throw new IllegalArgumentException("Type \"" + type + "\" not registered");
        }
        return parse(parts, classes.get(type));
    }

    public RequestMessage[] parseLog(String log) {
        return splitMessage(log).stream()
                .map(e -> "null".equals(e) ? null : (RequestMessage) parse(e))
                .toArray(RequestMessage[]::new);
    }

    private static Stream<Field> getArgFields(Class<? extends Message> messageClass) {
        List<Class<?>> parents = new ArrayList<>();
        Class<?> clazz = messageClass;
        do {
            parents.add(clazz);
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return parents.stream()
                .flatMap(c -> Stream.of(c.getDeclaredFields()))
                .filter(f -> f.isAnnotationPresent(MessageParam.class))
                .sorted((a, b) -> Integer.compare(a.getAnnotation(MessageParam.class).value(), b.getAnnotation(MessageParam.class).value()))
                .peek(f -> f.setAccessible(true));
    }

    public static String format(Message message) {
        return Stream.concat(
                Stream.of(message.getType()),
                getArgFields(message.getClass())
                        .map(f -> {
                            try {
                                return f.get(message).toString().replaceAll("([ \\\\])", "\\\\$1");
                            } catch (IllegalAccessException e) {
                                logger.log(Level.SEVERE, "Something went wrong", e);
                                return "NaN";
                            }
                        })
        )
                .collect(Collectors.joining(" "));
    }

    public static String formatLog(RequestLog log) {
        return Stream.of(log.getEntries())
                .map(message -> message == null ? "null" : format(message))
                .collect(Collectors.joining(" "));
    }

    private static List<String> splitMessage(String message) {
        List<String> parts = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean monitor = false;
        for (char c : message.toCharArray()) {
            if (monitor) {
                builder.append(c);
                monitor = false;
            } else if (c == '\\') {
                monitor = true;
            } else if (c == ' ') {
                if (builder.length() != 0) {
                    parts.add(builder.toString());
                    builder.setLength(0);
                }
            } else {
                builder.append(c);
            }
        }
        if (builder.length() != 0)
            parts.add(builder.toString());
        return parts;
    }

    private static <T extends Message> T parse(List<String> parts, Class<T> messageClass) {
        try {
            Constructor<T> constructor = messageClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();
            getArgFields(messageClass).forEach(field -> {
                int index = field.getAnnotation(MessageParam.class).value();
                String value = parts.get(index);
                Class<?> type = field.getType();
                try {
                    if (type == int.class) {
                        field.setInt(instance, Integer.parseInt(value));
                    } else if (type == double.class) {
                        field.setDouble(instance, Double.parseDouble(value));
                    } else if (type == String.class) {
                        field.set(instance, value);
                    } else {
                        logger.warning(type + " not supported.");
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            return instance;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not create instance of " + messageClass, e);
            return null;
        }
    }

    public static <T extends Message> T parse(String message, Class<T> messageClass) {
        return parse(splitMessage(message), messageClass);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Class<? extends Message>> classes = new HashMap<>();

        private void checkReregister(String type, Class<? extends Message> was, Class<? extends Message> now) {
            if (was != null) {
                logger.warning("Reregistering message class for type: " + type + ", was: " + was + ", now: " + now);
            }
        }

        public final Builder register(Class<? extends Message> messageClass) {
            String type = messageClass.getAnnotation(MessageType.class).value();
            checkReregister(type, classes.put(type, messageClass), messageClass);
            return this;
        }

        @SafeVarargs
        public final Builder register(Class<? extends Message>... messageClasses) {
            Stream.of(messageClasses).forEach(this::register);
            return this;
        }

        public final Builder register(String type, Class<? extends Message> messageClass) {
            checkReregister(type, classes.put(type, messageClass), messageClass);
            return this;
        }

        public MessageFormatter build() {
            return new MessageFormatter(new HashMap<>(classes));
        }
    }
}
