package ru.ifmo.rain.kurbatov.bank;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class Server {
    private final static int PORT = 8888;

    public static void main(final String[] args) {
        final Bank bank;
        try {
            bank = new RemoteBank(PORT);
            Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, PORT);
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind("bank", stub);
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            return;
        }
        System.out.println("Server started");
    }
}
