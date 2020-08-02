package ru.ifmo.rain.kurbatov.bank;

public class LocalAccount extends AbstractAccount {

    public LocalAccount(final String id) {
        super(id, 0);
    }

    public LocalAccount(final String id, final int amount) {
        super(id, amount);
    }
}
