package ru.zyulyaev.ifmo.dkvs.server.workflow;

import ru.zyulyaev.ifmo.dkvs.MessageFormatter;
import ru.zyulyaev.ifmo.dkvs.MessageRouter;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.debug.DieMessage;
import ru.zyulyaev.ifmo.dkvs.message.debug.SleepMessage;
import ru.zyulyaev.ifmo.dkvs.message.normal.AcceptMessage;
import ru.zyulyaev.ifmo.dkvs.message.normal.NodeMessage;
import ru.zyulyaev.ifmo.dkvs.message.normal.PrepareMessage;
import ru.zyulyaev.ifmo.dkvs.message.ping.PingMessage;
import ru.zyulyaev.ifmo.dkvs.message.ping.PongMessage;
import ru.zyulyaev.ifmo.dkvs.message.recovery.RecoveryMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.DeleteRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.GetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.RequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.SetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.viewchange.StartViewChangeMessage;
import ru.zyulyaev.ifmo.dkvs.server.NodeStatus;
import ru.zyulyaev.ifmo.dkvs.server.RemoteClient;
import ru.zyulyaev.ifmo.dkvs.server.RemoteNode;
import ru.zyulyaev.ifmo.dkvs.shared.Remote;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by nikita on 23.05.15.
 */
public abstract class Workflow {
    private static final Logger logger = Logger.getLogger(Workflow.class.getName());
    private static final MessageFormatter formatter = MessageFormatter.builder()
            .register(AcceptMessage.class, DeleteRequestMessage.class, GetRequestMessage.class, NodeMessage.class,
                    PingMessage.class, PongMessage.class, PrepareMessage.class, SetRequestMessage.class)
            .build();
    private final List<TimerTask> timerTasks = new ArrayList<>();
    protected final WorkflowContext context;
    private MessageRouter<RemoteNode> nodeRouter;
    private MessageRouter<RemoteClient> clientRouter;

    protected Workflow(WorkflowContext context) {
        this.context = context;
        configureRouters();
        configureTasks(new TasksConfigurer(System.currentTimeMillis()));
    }

    private Stream<Method> findAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        Queue<Class<?>> parents = Collections.asLifoQueue(new ArrayDeque<>());
        Class<?> clazz = getClass();
        do {
            parents.add(clazz);
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return parents.stream()
                .flatMap(c -> Stream.of(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(annotationClass))
                .peek(m -> m.setAccessible(true));
    }

    private <T> T makeLambda(Method method, Class<T> iface) throws IllegalAccessException {
        MethodHandle handle = MethodHandles.lookup().unreflect(method).bindTo(this);
        return MethodHandleProxies.asInterfaceInstance(iface, handle);
    }

    private void configureRouters() {
        MessageRouter.Builder<RemoteNode> nodeRouterBuilder = MessageRouter.builder();
        MessageRouter.Builder<RemoteClient> clientRouterBuilder = MessageRouter.builder();
        findAnnotatedMethods(MessageProcessor.class)
                .forEachOrdered(method -> {
                    try {
                        MessageProcessor ann = method.getAnnotation(MessageProcessor.class);
                        BiConsumer consumer = makeLambda(method, BiConsumer.class);
                        Class<?>[] types = method.getParameterTypes();
                        Class<?> actualMessageClass = types[0];
                        Class<?> originClass = types[1];
                        for (Class<? extends Message> messageClass : ann.value()) {
                            if (!actualMessageClass.isAssignableFrom(messageClass)) {
                                logger.severe("Cannot assign " + actualMessageClass + " from " + messageClass);
                                continue;
                            }
                            if (originClass.isAssignableFrom(RemoteNode.class))
                                nodeRouterBuilder.add(messageClass, consumer);
                            if (originClass.isAssignableFrom(RemoteClient.class))
                                clientRouterBuilder.add(messageClass, consumer);
                        }
                    } catch (IllegalAccessException e) {
                        logger.log(Level.SEVERE, "Something went wrong", e);
                    }
                });
        this.nodeRouter = nodeRouterBuilder.build();
        this.clientRouter = clientRouterBuilder.build();
    }

    protected void configureTasks(TasksConfigurer configurer) {
    }

    public abstract NodeStatus getStatus();

    protected final void startViewChange(int viewNumber, int lastNormalViewNumber, boolean byRequest) {
        context.setViewNumber(viewNumber);
        context.sendMessageToOtherNodes(new StartViewChangeMessage(viewNumber, context.getNodeIndex()));
        context.switchWorkflow(new ViewChangeWorkflow(context, lastNormalViewNumber, byRequest));
    }

    private static String generateNonce() {
        return new BigInteger(128, ThreadLocalRandom.current()).toString(36);
    }

    public final void startRecovery() {
        String nonce = generateNonce();
        context.sendMessageToOtherNodes(new RecoveryMessage(context.getNodeIndex(), nonce));
        context.switchWorkflow(new RecoveryWorkflow(context, nonce));
    }

    protected Message parseMessage(String message) {
        return formatter.parse(message);
    }

    protected RequestMessage[] parseLog(String log) {
        return formatter.parseLog(log);
    }

    public void processClientMessage(String message, RemoteClient client) {
        clientRouter.dispatch(message, client);
    }

    public void processNodeMessage(String message, RemoteNode node) {
        nodeRouter.dispatch(message, node);
    }

    public void tick() {
        long time = System.currentTimeMillis();
        timerTasks.forEach(t -> t.tick(time));
    }

    // Common message process methods

    @MessageProcessor(PingMessage.class)
    private void processPing(PingMessage pingMessage, Remote remote) {
        remote.sendMessage(new PongMessage());
    }

    @MessageProcessor(NodeMessage.class)
    private void processNodeMessage(NodeMessage message, RemoteClient client) {
        int index = message.getIndex();
        logger.info("Accepting connection from node #" + index);
        context.identifyAsNode(client, index);
        context.sendMessageToNode(index, new AcceptMessage());
    }

    @MessageProcessor(AcceptMessage.class)
    private void processAccept(AcceptMessage message, Remote remote) {
        logger.info("Node #" + context.getNodeIndex() + " is accepted");
    }

    @MessageProcessor(SleepMessage.class)
    private void processSleep(SleepMessage message, RemoteClient client) {
        try {
            Thread.sleep(message.getTimeout());
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "interrupted while asleep", e);
        }
    }

    @MessageProcessor(DieMessage.class)
    private void processDie(DieMessage message, RemoteClient client) {
        context.die();
    }

    protected static class TimerTask {
        private final int timeout;
        private final Runnable task;
        private long lastExecuted;

        private TimerTask(int timeout, Runnable task, long lastExecuted) {
            this.timeout = timeout;
            this.task = task;
            this.lastExecuted = lastExecuted;
        }

        private void tick(long time) {
            if (time - lastExecuted > timeout) {
                lastExecuted = time;
                task.run();
            }
        }

        protected void reset() {
            this.lastExecuted = System.currentTimeMillis();
        }
    }

    protected class TasksConfigurer {
        private final long initialTime;

        private TasksConfigurer(long initialTime) {
            this.initialTime = initialTime;
        }

        public TimerTask add(int timeout, Runnable task) {
            TimerTask timerTask = new TimerTask(timeout, task, initialTime);
            timerTasks.add(timerTask);
            return timerTask;
        }
    }
}
