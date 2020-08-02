package ru.ifmo.rain.kurbatov.bank;

import java.io.UncheckedIOException;
import java.rmi.RemoteException;
import java.util.Map;

public class Utils {
    public static void log(String message) {
        System.out.println(message);
    }

    public static void log(String message, Exception e) {
        System.out.println(message);
        //e.printStackTrace();
    }

    public static boolean isBadArg(Object arg, String name) {
        if (arg == null) {
            log(name + " is null");
            return true;
        }
        if (arg instanceof String) {
            if (((String) arg).isEmpty()) {
                log(name + " is empty");
                return true;
            }
        }
        return false;
    }

    public interface BankSupplier<R> {
        R get() throws RemoteException;
    }

    public static <K, V> V createIfAbsent(final Map<K, V> map, final K key,
                                          final BankSupplier<V> supplier) throws RemoteException {
        try {
            return map.computeIfAbsent(key, x -> {
                try {
                    return supplier.get();
                } catch (RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw (RemoteException) e.getCause();
        }
    }
}
