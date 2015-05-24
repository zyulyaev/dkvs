package ru.zyulyaev.ifmo.dkvs.shared;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Created by nikita on 23.05.15.
 */
public final class BoundChannel implements SocketChannelProcessor, Closeable {
    private static final Logger logger = Logger.getLogger(BoundChannel.class.getName());
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final int BUFFER_SIZE = 4096;

    private final Queue<ByteBuffer> outMessages = new ArrayDeque<>();
    private final ByteBuffer inBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final StringBuilder inMessages = new StringBuilder();

    private final SocketChannel channel;
    private final SelectionKey key;
    private final Runnable disconnectProcessor;
    private Consumer<String> messageProcessor;

    public BoundChannel(SocketChannel channel, Selector selector, Runnable disconnectProcessor) throws IOException {
        this.channel = channel;
        this.channel.configureBlocking(false);
        this.key = channel.register(selector, SelectionKey.OP_READ, this);
        this.disconnectProcessor = disconnectProcessor;
    }

    public BoundChannel(SocketAddress address, Selector selector, Runnable disconnectProcessor) throws IOException {
        this.channel = SocketChannel.open();
        this.channel.configureBlocking(false);
        this.channel.connect(address);
        this.key = this.channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, this);
        this.disconnectProcessor = disconnectProcessor;
    }

    public void sendMessage(String message) {
        logger.info("Message '" + message + "' added to queue");
        outMessages.add(ByteBuffer.wrap(message.concat("\n").getBytes(CHARSET)));
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }

    public void setMessageProcessor(Consumer<String> messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public void process(SelectionKey key) throws IOException {
        if (key != this.key)
            throw new IllegalStateException("Not my key!");
        if (key.isWritable()) {
            processWrite();
        }
        if (key.isConnectable()) {
            processConnect();
        }
        if (key.isReadable()) {
            processRead();
        }
    }

    private void processConnect() throws IOException {
        logger.info("Finishing connection with " + channel.getRemoteAddress());
        channel.finishConnect();
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
    }

    private void processRead() throws IOException {
        int rd;
        do {
            inBuffer.rewind();
            rd = channel.read(inBuffer);
            if (rd == -1) {
                onDisconnect();
                return;
            }
            inMessages.append(new String(inBuffer.array(), 0, rd, CHARSET));

            int nl;
            while ((nl = inMessages.indexOf("\n")) != -1) {
                String message = inMessages.substring(0, nl).trim();
                inMessages.delete(0, nl + 1);
                logger.info("Processing message from " + channel.getRemoteAddress() + ": " + message);
                messageProcessor.accept(message);
            }
        } while (rd != 0);
    }

    private void onDisconnect() {
        key.cancel();
        disconnectProcessor.run();
    }

    private void processWrite() throws IOException {
        while (!outMessages.isEmpty()) {
            ByteBuffer buffer = outMessages.peek();
            if (!buffer.hasRemaining()) {
                outMessages.remove();
                continue;
            }
            int wrote = channel.write(buffer);
            if (!buffer.hasRemaining())
                outMessages.remove();
            if (wrote == 0)
                break;
        }
        if (outMessages.isEmpty()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    @Override
    public void close() throws IOException {
        key.cancel();
        channel.close();
    }

    public Runnable getDisconnectProcessor() {
        return disconnectProcessor;
    }
}
