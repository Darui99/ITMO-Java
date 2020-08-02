package ru.ifmo.rain.kurbatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.kurbatov.hello.HelloUDPUtils.*;

/**
 * Implementation of {@link HelloServer}
 */
public class HelloUDPServer implements HelloServer {

    private DatagramSocket socket;
    private ExecutorService threads;

    /**
     * Receive {@link info.kgeorgiy.java.advanced.hello.HelloClient}'s requests
     * Send {@code Hello, } plus request's data
     * @param port is server port.
     */
    @Override
    public void start(final int port, final int threadsCount) {
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            HelloUDPUtils.log("Unable to create socket", e);
            return;
        }
        threads = Executors.newFixedThreadPool(threadsCount);
        for (int i = 0; i < threadsCount; i++) {
            threads.submit(this::responde);
        }
    }

    private void responde() {
        final int bufferSize;
        try {
            bufferSize = socket.getReceiveBufferSize();
        } catch (final SocketException e) {
            HelloUDPUtils.log("Troubles with getting ReceiveBufferSize", e);
            return;
        }
        final byte[] buffer = new byte[bufferSize];
        final DatagramPacket receivePacket = new DatagramPacket(buffer, bufferSize);
        while (!Thread.interrupted() && !socket.isClosed()) {
            try {
                receivePacket.setData(buffer);
                socket.receive(receivePacket);
                final String receiveText = "Hello, " + HelloUDPUtils.getDatagramPacketDataAsString(receivePacket);
                receivePacket.setData(receiveText.getBytes());
                socket.send(receivePacket);
            } catch (final IOException e) {
                HelloUDPUtils.log("Troubles with processing DatagramPacket", e);
            }
        }
    }

    /**
     * Stops {@code HelloUDPServer}
     */
    @Override
    public void close() {
        socket.close();
        closeAndAwaitTerm(threads);
    }

    /**
     * Run {@code HelloUDPServer}
     * @param args requires 2 args:
     *             1 - port
     *             2 - number of threads
     */
    public static void main(final String[] args) {
        runServerMain(args, HelloUDPServer::new);
    }
}
