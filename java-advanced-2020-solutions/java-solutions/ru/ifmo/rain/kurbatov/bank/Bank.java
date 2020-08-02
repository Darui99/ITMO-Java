package ru.ifmo.rain.kurbatov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {

    Account createAccount(String id) throws RemoteException;

    Account getAccount(String id) throws RemoteException;

    Person createPerson(int passportId, String firstName, String lastName) throws RemoteException;

    Person getLocalPerson(int passportId) throws RemoteException;

    Person getRemotePerson(int passportId) throws RemoteException;
}