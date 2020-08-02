package ru.ifmo.rain.kurbatov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(final String[] args) throws RemoteException {
        if (args == null) {
            System.out.println("Bad args");
            return;
        }

        if (args.length != 5) {
            System.out.println("Bad args count");
            return;
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                System.out.println(i + " args is null");
                return;
            }
        }

        // имя, фамилия, номер паспорта физического лица, номер счета, изменение суммы счета.
        final String firstName = args[0];
        final String lastName = args[1];
        final int passId;
        final String subId = args[3];
        final int diffAmount;

        try {
            passId = Integer.parseInt(args[2]);
            diffAmount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("Bad numbers in args");
            e.printStackTrace();
            return;
        }

        if (passId < 0) {
            System.out.println("passport id must be non-negative");
            return;
        }

        final Bank bank;
        try {
            Registry registry = LocateRegistry.getRegistry(8888);
            bank = (Bank) registry.lookup("bank");
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        }

        Person person = bank.getRemotePerson(passId);
        if (person == null) {
            System.out.println("Creating person");
            bank.createPerson(passId, firstName, lastName);
        } else {
            if (!firstName.equals(person.getFirstName()) || !lastName.equals(person.getLastName())) {
                System.out.println("Invalid name");
                return;
            }
        }

        final String accountId = passId + ":" + subId;
        Account account = bank.getAccount(accountId);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(accountId);
        }

        final int amount = account.getAmount();
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + amount);
        System.out.println("Changing money");
        account.addAmount(diffAmount);
        System.out.println("Money: " + account.getAmount());
    }
}
