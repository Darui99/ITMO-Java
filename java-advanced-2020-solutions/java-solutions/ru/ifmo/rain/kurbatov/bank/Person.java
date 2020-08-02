package ru.ifmo.rain.kurbatov.bank;

import java.rmi.*;
import java.util.Map;
import java.util.Set;

public interface Person extends Remote {

    int getPassportId() throws RemoteException;

    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    Set<Map.Entry<String, Account>> getAccounts() throws RemoteException;

    Account addAccount(String subId) throws RemoteException;

    Account getAccount(String subId) throws RemoteException;
}
