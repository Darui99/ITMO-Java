package ru.ifmo.rain.kurbatov.bank;

import java.io.Serializable;

import static ru.ifmo.rain.kurbatov.bank.Utils.*;

public abstract class AbstractAccount implements Account, Serializable {
    private final String id;
    private int amount;

    public AbstractAccount(final String id) {
        this(id, 0);
    }

    public AbstractAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        log("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(int amount) {
        log("Setting amount of money for account " + id);
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(int amount) {
        log("Adding amount of money for account " + id);
        if (this.amount < -amount) {
            log("Not enough money for this operation");
            return;
        }
        this.amount += amount;
    }
}
