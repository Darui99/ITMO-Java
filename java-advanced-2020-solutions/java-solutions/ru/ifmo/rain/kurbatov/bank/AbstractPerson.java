package ru.ifmo.rain.kurbatov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static ru.ifmo.rain.kurbatov.bank.Utils.*;

public abstract class AbstractPerson implements Person, Serializable {
    protected final int id;
    private final String firstName;
    private final String lastName;
    protected final Map<String, Account> accountsMap;

    public AbstractPerson(final int id, final String firstName, final String lastName) {
        this(id, firstName, lastName, new ConcurrentHashMap<>());
    }

    public AbstractPerson(final int id, final String firstName, final String lastName, final Map<String, Account> accountsMap) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accountsMap = new ConcurrentHashMap<>(accountsMap);
    }

    @Override
    public int getPassportId() {
        return id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public Set<Map.Entry<String, Account>> getAccounts() {
        return Collections.unmodifiableSet(accountsMap.entrySet());
    }

    @Override
    public abstract Account addAccount(final String subId) throws RemoteException;

    @Override
    public Account getAccount(final String subId) {
        if (isBadArg(subId, "subId")) {
            return null;
        }
        return accountsMap.get(subId);
    }
}
