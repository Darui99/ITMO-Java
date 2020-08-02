package ru.ifmo.rain.kurbatov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import static ru.ifmo.rain.kurbatov.bank.Utils.*;

public class RemotePerson extends AbstractPerson {

    private final int port;

    public RemotePerson(final int id, final String firstName, final String lastName, final int port) throws RemoteException {
        super(id, firstName, lastName);
        this.port = port;
        UnicastRemoteObject.exportObject(this, port);
    }

    @Override
    public Account addAccount(final String subId) throws RemoteException {
        if (isBadArg(subId, "subId")) {
            return null;
        }
        return createIfAbsent(accountsMap, subId, () -> new RemoteAccount(id + ":" + subId, port));
    }
}