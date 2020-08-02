package ru.ifmo.rain.kurbatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import static ru.ifmo.rain.kurbatov.hello.HelloUDPUtils.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class HelloUDPNonblockingServer implements HelloServer {

    private Selector selector = null;
    private DatagramChannel channel = null;
    private ExecutorService threads;
    private final Queue<Response> responses = new ArrayDeque<>();
    private final Queue<ByteBuffer> freeBuffers = new ArrayDeque<>();

    private class Response {
        private final SocketAddress address;
        private final String request;
        private String response;

        public Response(final SocketAddress address, final String request) {
            this.address = address;
            this.request = request;
            this.response = null;
        }

        public void generateResponse() {
            if (response == null) {
                response = "Hello, " + request;
            }
        }

        public SocketAddress getAddress() {
            return address;
        }

        public String getResponse() {
            return response;
        }
    }

    @Override
    public void start(final int port, final int threadsCount) {
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(port));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
        } catch (final IOException e) {
            if (closeSelectorAfterException(selector, e) || closeChannelAfterException(channel, e)) {
                return;
            }
            log("Troubles with processing DatagramChannel", e);
            return;
        }
        try {
            final int receiveBufferSize = channel.getOption(StandardSocketOptions.SO_RCVBUF);
            for (int i = 0; i < threadsCount; i++) {
                freeBuffers.add(ByteBuffer.allocate(receiveBufferSize));
            }
        } catch (final IOException e) {
            log("Troubles with getting receiveBufferSize", e);
        }
        threads = Executors.newFixedThreadPool(threadsCount + 1);
        threads.submit(this::run);
    }

    private void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                    final SelectionKey key = i.next();
                    final DatagramChannel curChannel = (DatagramChannel) key.channel();
                    try {
                        if (key.isReadable()) {
                            final ByteBuffer receiveBuffer = syncPoll(freeBuffers,
                                    () -> key.interestOpsAnd(SelectionKey.OP_WRITE));
                            if (receiveBuffer == null) {
                                continue;
                            }
                            final SocketAddress address = curChannel.receive(receiveBuffer);
                            threads.submit(() -> {
                                final String request = getBufferDataAsString(receiveBuffer);
                                final Response response = new Response(address, request);
                                response.generateResponse();
                                syncAdd(responses, response, () -> key.interestOpsOr(SelectionKey.OP_WRITE), selector);
                                receiveBuffer.clear();
                                syncAdd(freeBuffers, receiveBuffer, () -> key.interestOpsOr(SelectionKey.OP_READ), selector);
                            });
                        }
                        if (key.isValid() && key.isWritable()) {
                            final Response response = syncPoll(responses, () -> key.interestOpsAnd(SelectionKey.OP_READ));
                            if (response == null) {
                                continue;
                            }
                            curChannel.send(ByteBuffer.wrap(response.getResponse().getBytes()), response.getAddress());
                        }
                    } finally {
                        i.remove();
                    }
                }
            }
        } catch (final IOException e) {
            log("Error in selector-thread", e);
        }
    }

    @Override
    public void close() {
        closeAndAwaitTerm(threads);
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (final IOException e) {
            log("Troubles with closing", e);
        }
    }

    public static void main(final String[] args) {
        runServerMain(args, HelloUDPNonblockingServer::new);
    }
}