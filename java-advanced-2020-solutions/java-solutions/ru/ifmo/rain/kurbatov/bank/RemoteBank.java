package ru.ifmo.rain.kurbatov.bank;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ru.ifmo.rain.kurbatov.bank.Utils.*;

public class RemoteBank implements Bank {

    private final int port;
    private final ConcurrentMap<Integer, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    private interface PersonMethod {
        Account apply(Person person, String id) throws RemoteException;
    }

    private Account processViaPerson(final String id, PersonMethod f) throws RemoteException {
        if (isBadArg(id, "id")) {
            return null;
        }
        try {
            String[] parts = id.split(":");
            return f.apply(persons.get(Integer.parseInt(parts[0])), parts[1]);
        } catch (final NumberFormatException | IndexOutOfBoundsException e) {
            log("Invalid account-id", e);
            return null;
        } catch (final NullPointerException e) {
            log("There is no person with such passport", e);
            return null;
        }
    }

    public Account createAccount(final String id) throws RemoteException {
        log("Creating account " + id);
        return processViaPerson(id, Person::addAccount);
    }

    public Account getAccount(final String id) throws RemoteException {
        log("Retrieving account " + id);
        return processViaPerson(id, Person::getAccount);
    }

    @Override
    public Person createPerson(final int id, final String firstName, final String lastName) throws RemoteException {
        log("Creating person with passport " + id);
        if (id < 0) {
            log("passport must be non-negative");
            return null;
        }
        if (isBadArg(firstName, "firstName") || isBadArg(lastName, "lastName")) {
            return null;
        }
        return createIfAbsent(persons, id, () -> new RemotePerson(id, firstName, lastName, port));
    }

    @Override
    public Person getLocalPerson(final int id) throws RemoteException {
        Person person = persons.get(id);
        if (person == null) {
            return null;
        }
        final int passport = person.getPassportId();
        final ConcurrentMap<String, Account> map = new ConcurrentHashMap<>();
        for (Map.Entry<String, Account> elem : person.getAccounts()) {
            map.put(elem.getKey(), new LocalAccount(passport + ":" + elem.getKey(), elem.getValue().getAmount()));
        }
        return new LocalPerson(id, person.getFirstName(), person.getLastName(), map);
    }

    @Override
    public Person getRemotePerson(final int id) {
        return persons.get(id);
    }
}
