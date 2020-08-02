package ru.ifmo.rain.kurbatov.bank;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class BankTests {

    private static final int PORT = 8888;
    private static Registry registry;
    private static Bank bank;

    @BeforeAll
    public static void init() throws RemoteException {
        registry = LocateRegistry.createRegistry(PORT);
    }

    @BeforeEach
    public void reloadBank() throws RemoteException {
        bank = new RemoteBank(PORT);
        Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, PORT);
        registry.rebind("bank", stub);
    }

    private void initAcc(final Bank bank) throws RemoteException {
        bank.createPerson(1, "Egor", "Kurbatov");
        bank.createAccount("1:a");
    }

    private void checkAmount(final int expected, final String id) throws RemoteException {
        assertEquals(expected, bank.getAccount(id).getAmount());
    }

    private void checkAmount(final int expected, final Person person, final String id) throws RemoteException {
        assertEquals(expected, person.getAccount(id).getAmount());
    }

    private void checkAmount(final int expected, final int passport, final String id) throws RemoteException {
        assertEquals(expected, bank.getRemotePerson(passport).getAccount(id).getAmount());
    }

    @Test
    public void test1_setAndGet() throws RemoteException {
        initAcc(bank);
        Account acc = bank.getAccount("1:a");
        acc.setAmount(100);
        checkAmount(100, "1:a");
        acc.setAmount(0);
        checkAmount(0, "1:a");
    }

    @Test
    public void test2_unusedPassport() throws RemoteException {
        assertNull(bank.createAccount("2:a"));
        assertNull(bank.getAccount("2:a"));
    }

    @Test
    public void test3_rewrite() throws RemoteException {
        initAcc(bank);
        bank.createPerson(1, "Pavel", "Burmatov");
        assertEquals("Kurbatov", bank.getRemotePerson(1).getLastName());
        assertNull(bank.getRemotePerson(2));
        bank.getAccount("1:a").addAmount(10);
        bank.createAccount("1:a");
        checkAmount(10, "1:a");
    }

    @Test
    public void test4_multiAccs() throws RemoteException {
        initAcc(bank);
        bank.createAccount("1:a");
        bank.createAccount("1:b");
        bank.createAccount("1:b");
        bank.createAccount("1:c");
        bank.createAccount("1:c");
        assertEquals(3, bank.getRemotePerson(1).getAccounts().size());
    }

    private interface BankTask {
        void run() throws RemoteException;
    }

    private void processParallel(int threadsCnt, int all, BankTask task) throws RemoteException {
        final Queue<RemoteException> exceptions = new ConcurrentLinkedQueue<>();
        ExecutorService threadPool = Executors.newFixedThreadPool(threadsCnt);
        IntStream.range(0, all).mapToObj(x -> threadPool.submit(() -> {
            try {
                task.run();
            } catch (RemoteException e) {
                exceptions.add(e);
            }
        })).collect(Collectors.toList()).forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ignored) {
                // pass
            }
        });
        if (!exceptions.isEmpty()) {
            final RemoteException exception = exceptions.poll();
            while (!exceptions.isEmpty()) {
                exception.addSuppressed(exceptions.poll());
            }
            throw exception;
        }
    }

    @Test
    public void test5_parallelAdd() throws RemoteException {
        initAcc(bank);
        processParallel(15, 225, () -> bank.getAccount("1:a").addAmount(10));
        checkAmount(2250, "1:a");
    }

    @Test
    public void test6_parallelAddAccs() throws RemoteException {
        bank.createPerson(15, "Egor", "Kurbatov");
        final AtomicInteger numeric = new AtomicInteger(1);
        processParallel(10, 100, () ->  {
            final int num = numeric.getAndIncrement();
            Person person = bank.createPerson(num % 10, "X", "AE-12");
            person.addAccount(Integer.toString(num / 10 % 5)).addAmount(50);
            bank.createAccount("15:" + num);
        });
        assertEquals(100, bank.getRemotePerson(15).getAccounts().size());
        for (int i = 0; i < 10; i++) {
            Person person = bank.getRemotePerson(i);
            assertNotNull(person);
            assertEquals(5, person.getAccounts().size());
            for (Map.Entry<String, Account> elem : person.getAccounts()) {
                checkAmount(100, person, elem.getKey());
            }
        }
    }

    @Test
    public void test7_parallelCreates() throws RemoteException {
        final AtomicInteger numeric = new AtomicInteger(1);
        processParallel(50, 5000, () -> {
            final int num = numeric.getAndIncrement() % 10;
            Person person = bank.createPerson(num, Integer.toString(num), Integer.toString(num));
            bank.createAccount(num + ":" + numeric.getAndIncrement() % 7);
            bank.getAccount(person.getPassportId() + ":" + person.getAccounts().iterator().next().getKey())
                    .addAmount(1);
        });
        for (int i = 0; i < 10; i++) {
            Person person = bank.getRemotePerson(i);
            assertNotNull(person);
            assertEquals(7, person.getAccounts().size());
            for (Map.Entry<String, Account> elem : person.getAccounts()) {
                assertNotNull(elem.getKey());
            }
            assertNotEquals(0, person.getAccount("0").getAmount());
        }
    }

    private void tryAddInvalidAcc(final String id) throws RemoteException {
        assertNull(bank.createAccount(id));
        assertNull(bank.getAccount(id));
    }

    @Test
    public void test8_invalidAccs() throws RemoteException {
        tryAddInvalidAcc("abc");
        tryAddInvalidAcc("123");
        tryAddInvalidAcc("123:");
        tryAddInvalidAcc(":abc");
        tryAddInvalidAcc("");
        tryAddInvalidAcc(null);
    }

    private void tryAddInvalidPerson(final int id, final String firstName, final String lastName) throws RemoteException {
        assertNull(bank.createPerson(id, firstName, lastName));
        assertNull(bank.getRemotePerson(id));
    }

    @Test
    public void test9_invalidPersons() throws RemoteException {
        tryAddInvalidPerson(-1, "A", "B");
        tryAddInvalidPerson(1, "", "B");
        tryAddInvalidPerson(2, "A", "");
        tryAddInvalidPerson(3, null, "B");
        tryAddInvalidPerson(4, "A", null);
    }

    @Test
    public void test10_getAccountViaPerson() throws RemoteException {
        initAcc(bank);
        bank.getAccount("1:a").addAmount(100);
        checkAmount(100, 1, "a");
        checkAmount(bank.getRemotePerson(1).getAccount("a").getAmount(), "1:a");
    }

    @Test
    public void test11_multiCreateAndGet() throws RemoteException {
        initAcc(bank);
        bank.getRemotePerson(1).getAccount("a").addAmount(100);
        bank.createAccount("1:b").addAmount(50);
        bank.createAccount("1:a");
        checkAmount(100, 1, "a");
        bank.createAccount("1:a").addAmount(50);
        checkAmount(150, 1, "a");
        checkAmount(50, 1, "b");
        assertEquals(2, bank.getRemotePerson(1).getAccounts().size());
    }

    @Test
    public void test12_simpleLocalTest() throws RemoteException {
        initAcc(bank);
        Person person = bank.getLocalPerson(1);
        person.getAccount("a").setAmount(100);
        person.getAccount("a").addAmount(50);
        checkAmount(150, person, "a");
        assertEquals(1, person.getAccounts().size());
        checkAmount(0, "1:a");
    }

    @Test
    public void test13_twoLocalCompare() throws RemoteException {
        initAcc(bank);
        Person person1 = bank.getLocalPerson(1);
        person1.getAccount("a").setAmount(100);
        Person person2 = bank.getLocalPerson(1);
        person2.getAccount("a").setAmount(50);
        bank.getRemotePerson(1).getAccount("a").addAmount(30);
        checkAmount(100, person1, "a");
        checkAmount(50, person2, "a");
        checkAmount(30, "1:a");
    }

    @Test
    public void test14_LocalDiffVerCompare() throws RemoteException {
        initAcc(bank);
        Person person1 = bank.getLocalPerson(1);
        bank.getRemotePerson(1).getAccount("a").addAmount(15);
        Person person2 = bank.getLocalPerson(1);
        bank.getRemotePerson(1).getAccount("a").addAmount(15);
        checkAmount(0, person1, "a");
        checkAmount(15, person2, "a");
        checkAmount(30, "1:a");
    }

    @Test
    public void test15_twoLocalAdvancedCompare() throws RemoteException {
        initAcc(bank);
        Person person1 = bank.getLocalPerson(1);
        person1.getAccount("a").setAmount(100);
        bank.getRemotePerson(1).getAccount("a").addAmount(50);
        Person person2 = bank.getLocalPerson(1);
        person2.addAccount("b");
        person2.getAccount("b").setAmount(300);
        person2.getAccount("a").addAmount(70);
        checkAmount(100, person1, "a");
        assertNull(person1.getAccount("b"));
        checkAmount(120, person2, "a");
        checkAmount(300, person2, "b");
        checkAmount(50, "1:a");
        assertNull(bank.getAccount("1:b"));
    }

    @Test
    public void test16_addAccountViaPerson() throws RemoteException {
        initAcc(bank);
        Person person1 = bank.getRemotePerson(1);
        person1.addAccount("b");
        assertNotNull(bank.getAccount("1:b"));
        Person person2 = bank.getRemotePerson(1);
        assertNotNull(person2.getAccount("b"));
    }

    @Test
    public void test17_addAccountProcessing() throws RemoteException {
        initAcc(bank);
        Person person = bank.getRemotePerson(1);
        person.addAccount("b");
        bank.getAccount("1:b").addAmount(100);
        checkAmount(100, person, "b");
        person.addAccount("b");
        checkAmount(100, person, "b");
        bank.createAccount("1:b");
        checkAmount(100, person, "b");
        checkAmount(100, "1:b");
        assertEquals(2, person.getAccounts().size());
    }

    @Test
    public void test18_severalRemotes() throws RemoteException {
        initAcc(bank);
        Person person1 = bank.getRemotePerson(1);
        Person person2 = bank.getRemotePerson(1);
        bank.createAccount("1:b");
        bank.getAccount("1:b").addAmount(100);
        checkAmount(100, person1, "b");
        checkAmount(100, person2, "b");
        person1.getAccount("b").addAmount(50);
        checkAmount(150, "1:b");
        checkAmount(150, person2, "b");
    }

    @Test
    public void test19_addViaBankAndCheckGet() throws RemoteException {
        initAcc(bank);
        bank.createAccount("1:b");
        bank.createAccount("1:c");
        assertNotNull(bank.getRemotePerson(1).getAccount("a"));
        assertNotNull(bank.getRemotePerson(1).getAccount("b"));
        assertNotNull(bank.getRemotePerson(1).getAccount("c"));
        bank.getRemotePerson(1).addAccount("d");
        assertNotNull(bank.getAccount("1:d"));
        assertEquals(4, bank.getRemotePerson(1).getAccounts().size());
    }

    @Test
    public void test20_baseGetters() throws RemoteException {
        initAcc(bank);
        Person person1 = bank.getRemotePerson(1);
        assertEquals(1, person1.getPassportId());
        assertEquals("Egor", person1.getFirstName());
        assertEquals("Kurbatov", person1.getLastName());

        Person person2 = bank.getLocalPerson(1);
        assertEquals(1, person2.getPassportId());
        assertEquals("Egor", person2.getFirstName());
        assertEquals("Kurbatov", person2.getLastName());
        person2.addAccount("b");
        assertEquals(Set.of(new String[]{"a", "b"}), person2.getAccounts()
                                                    .stream().map(Map.Entry::getKey).collect(Collectors.toSet()));
        assertNotNull(person2.getAccount("a"));
        assertNotNull(person2.getAccount("b"));
        assertEquals(person1.getAccount("a").getId(), person2.getAccount("a").getId());
    }
}