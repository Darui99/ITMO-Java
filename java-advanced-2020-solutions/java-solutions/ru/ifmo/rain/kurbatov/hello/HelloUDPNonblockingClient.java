package ru.ifmo.rain.kurbatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static ru.ifmo.rain.kurbatov.hello.HelloUDPUtils.*;

public class HelloUDPNonblockingClient implements HelloClient {

    private Selector selector;
    private InetSocketAddress address;
    private int receiveBufferSize;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        List<DatagramChannel> channels = new ArrayList<>();
        try {
            selector = Selector.open();
            address = new InetSocketAddress(InetAddress.getByName(host), port);
            for (int i = 0; i < threads; i++) {
                final DatagramChannel channel = DatagramChannel.open();
                channel.connect(address);
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);
                channels.add(channel);
            }
            receiveBufferSize = channels.get(0).getOption(StandardSocketOptions.SO_RCVBUF);
        } catch (IOException e) {
            if (closeSelectorAfterException(selector, e)) {
                return;
            }
            for (final DatagramChannel channel : channels) {
                if (closeChannelAfterException(channel, e)) {
                    return;
                }
            }
            log("Troubles with DatagramChannel", e);
            return;
        }
        process(prefix, threads, requests);
        channels.forEach(x -> {
            try {
                x.close();
            } catch (IOException ignored) {
                // pass
            }
        });
    }

    private class ChannelAttachment {
        private final int threadNumber;
        private final int allRequests;
        private int requestNumber;

        public ChannelAttachment(final int threadNumber, final int allRequests) {
            this.threadNumber = threadNumber;
            this.allRequests = allRequests;
            this.requestNumber = 0;
        }

        public boolean increment() {
            return (++requestNumber == allRequests);
        }

        public int getThreadNumber() {
            return threadNumber;
        }

        public int getRequestNumber() {
            return requestNumber;
        }
    }

    public void process(final String prefix, final int threads, final int requests) {
        try {
            final ByteBuffer receiveBuffer = ByteBuffer.allocate(receiveBufferSize);
            int curThread = 0, rem = threads;
            for (SelectionKey key : selector.keys()) {
                key.attach(new ChannelAttachment(curThread++, requests));
            }
            while (!Thread.interrupted() && rem > 0) {
                selector.select(TIMEOUT);
                if (selector.selectedKeys().isEmpty()) {
                    for (SelectionKey key : selector.keys()) {
                        send((DatagramChannel) key.channel(), (ChannelAttachment) key.attachment(), prefix);
                    }
                } else {
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        final DatagramChannel curChannel = (DatagramChannel) key.channel();
                        final ChannelAttachment info = (ChannelAttachment) key.attachment();
                        try {
                            if (key.isReadable()) {
                                receiveBuffer.clear();
                                curChannel.receive(receiveBuffer);
                                if (verify(getBufferDataAsString(receiveBuffer), info.getThreadNumber(), info.getRequestNumber())) {
                                    if (info.increment()) {
                                        key.attach(null);
                                        rem--;
                                    }
                                }
                                if (needProcess(key)) {
                                    if (key.isValid()) {
                                        send(curChannel, info, prefix);
                                    }
                                } else {
                                    key.cancel();
                                }
                            }
                        } finally {
                            i.remove();
                        }
                    }
                }
            }
        } catch (IOException e) {
            log("Error in selector-thread", e);
        }
    }

    private void send(final DatagramChannel curChannel, final ChannelAttachment info, final String prefix) throws IOException {
        curChannel.send(ByteBuffer.wrap(createData(prefix,
                info.getThreadNumber(), info.getRequestNumber())), address);
    }

    private boolean needProcess(final SelectionKey key) {
        return key.attachment() != null;
    }

    private byte[] createData(final String prefix, final int threadNumber, final int requestNumber) {
        final String query = prefix + threadNumber + "_" + requestNumber;
        return query.getBytes();
    }

    public static void main(final String[] args) {
        runClientMain(args, HelloUDPNonblockingClient::new);
    }
}
