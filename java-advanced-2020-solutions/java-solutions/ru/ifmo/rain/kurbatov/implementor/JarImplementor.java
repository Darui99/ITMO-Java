package ru.ifmo.rain.kurbatov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.nio.file.Paths;

/**
 * {@link Implementor} with own {@code main}
 */
public class JarImplementor extends Implementor {
    /**
     * Generate {@code .jar} file of implementation of given {@link Class}
     *
     * @param args: 3 arguments (-jar flag, class name and dst) for generating {@code .jar} file
     */
    public static void main(final String[] args) {
        if (args == null || args.length != 3) {
            System.out.println("Bad args");
            return;
        }
        if (checkArgs(args)) {
            return;
        }
        try {
            final JarImpler impl = new JarImplementor();
            if (!args[0].equals("-jar")) {
                System.out.println("Wrong modifier");
                return;
            }
            impl.implementJar(Class.forName(args[1]), Paths.get(args[2]));
        } catch (final ImplerException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
