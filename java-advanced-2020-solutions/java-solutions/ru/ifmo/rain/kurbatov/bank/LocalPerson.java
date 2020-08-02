package ru.ifmo.rain.kurbatov.bank;

import java.util.Map;

import static ru.ifmo.rain.kurbatov.bank.Utils.isBadArg;

public class LocalPerson extends AbstractPerson {

    public LocalPerson(final int id, final String firstName, final String lastName, final Map<String, Account> accountsMap) {
       super(id, firstName, lastName, accountsMap);
    }

    @Override
    public Account addAccount(final String subId) {
        if (isBadArg(subId, "subId")) {
            return null;
        }
        return accountsMap.computeIfAbsent(subId, x -> new LocalAccount(id + ":" + x));
    }
}
