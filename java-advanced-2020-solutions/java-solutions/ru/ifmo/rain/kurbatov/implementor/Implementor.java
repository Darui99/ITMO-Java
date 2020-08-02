package ru.ifmo.rain.kurbatov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


/**
 * Implementation of {@link JarImpler}
 *
 * @author Darui99
 */
public class Implementor implements JarImpler {

    /**
     * {@link String} which is equal to End of line
     */
    private final static String ENDL = System.lineSeparator();

    /**
     * Wrapper of {@link Method} with good {@code equals} and {@code hashCode}
     */
    private class MethodwithEq {
        /**
         * Instance of {@link Method}
         */
        private Method method;

        /**
         * Constructor accepts {@link Method} for wrapping
         * @param method is {@link Method}
         */
        MethodwithEq(final Method method) {
            this.method = method;
        }

        /**
         * Compares object with {@code this} for equality. Wrappers are equal, if their wrapped
         * methods have equal name, return type and parameters' types.
         *
         * @param obj is {@link Object} which need to be compared with {@code this}
         * @return true if obj is equal to {@code this}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof MethodwithEq) {
                final MethodwithEq tmp = (MethodwithEq) obj;
                return Arrays.equals(method.getParameterTypes(), tmp.method.getParameterTypes())
                        && method.getName().equals(tmp.method.getName());
            }
            return false;
        }


        /**
         * Getter of {@code method} field
         * @return wrapped {@link Method}
         */
        Method getMethod() {
            return method;
        }

        /**
         * Calculate hashcode which depends on name, return type and parameters' types
         * @return hashCode of wrapped {@link Method}
         */
        @Override
        public int hashCode() {
            return Objects.hash(method.getName().hashCode(),
                    Arrays.hashCode(method.getParameterTypes()));
        }
    }

    /**
     * Returns name of implementation class
     *
     * @param token type-token ({@link Class}) of interface or abstract class
     * @return {@link String} which contains type-token's name plus "Impl" suffix
     */
    private String getClassName(final Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Generate package string for header of result file
     *
     * @param token type-token ({@link Class}) of interface or abstract class
     * @return {@link String} which contains package of type-token
     */
    private String getPackage(final Class<?> token) {
        String res = "";
        if (!token.getPackageName().equals("")) {
            res += "package " + token.getPackageName() + ";" + ENDL;
        }
        return res;
    }

    /**
     * Generate declaration of implementation class
     *
     * @param token type-token ({@link Class}) of interface or abstract class
     * @return {@link String} which contains modifier, class name and parent (token)
     */
    private String getClassDeclaration(final Class<?> token) {
        return getPackage(token) + "public class " + getClassName(token)
                + " " + (token.isInterface() ? "implements" : "extends") + " "
                + token.getCanonicalName() + " {" + ENDL;
    }

    /**
     * Generate string with argument's name and optionally his type
     *
     * @param param is {@link Parameter} which name we need
     * @param needType result contains {@code param}'s type if true
     * @return {@link String} which contains Type(optionally) plus argument's name
     */
    private String getArg(final Parameter param, final boolean needType) {
        return (needType ? param.getType().getCanonicalName() + " " : "") + param.getName();
    }


    /**
     * Generate string of arguments of {@link Constructor} or {@link Method} (optionally with their types)
     *
     * @param exec is {@link Executable} to get arguments from
     * @param needType result contains params' types if true
     * @return {@link String} which contains arguments' names and optionally types
     */
    private String getArgs(final Executable exec, final boolean needType) {
        return Arrays.stream(exec.getParameters())
                .map(param -> getArg(param, needType))
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Generate string with default value of given class:
     *
     * @param token is type-token ({@link Class}) which default value we need
     * @return {@link String} with default value
     */
    private String getDefaultValue(final Class<?> token) {
        if (token.equals(boolean.class)) {
            return" false";
        } else if (token.equals(void.class)) {
            return "";
        } else if (token.isPrimitive()) {
            return " 0";
        } else {
            return " null";
        }
    }

    /**
     * Add brackets and ends of line to main string of body
     *
     * @param main is main part of body
     * @return {@link String} with full body of some {@link Executable}
     */
    private String getExecFullBody(final String main) {
        return " {" + ENDL + main + ";" + ENDL + "}" + ENDL;
    }

    /**
     * Generate return type and name of given {@link Method}
     *
     * @param method is {@link Method} which return type and name we need of
     * @return {@link String} with return type and name
     */
    private String getMethodTypeAndName(final Method method) {
        return method.getReturnType().getCanonicalName() + " " + method.getName();
    }

    /**
     * Generate string with exceptions which param throws
     *
     * @param exec is given {@link Constructor} or {@link Method} which exceptions we need of
     * @return {@link String} which contains list of exceptions
     */
    private static String getExceptions(final Executable exec) {
        final Class<?>[] exceptions = exec.getExceptionTypes();
        if (exceptions.length == 0) {
            return "";
        }
        return " throws " + Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Generate declaration and definition of given {@link Executable}
     *
     * @param exec is {@link Executable} which we need to implement
     * @param typeAndName is {@link String} with return type and name of {@code exec}
     * @param body is {@link String} with body of {@code exec}
     * @return {@link String} with full implementation of given {@link Executable}
     */
    private String getExecutable(final Executable exec, final String typeAndName, final String body) {
        final int mods = exec.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT;
        return Modifier.toString(mods) + " " +
                typeAndName +
                getArgs(exec, true) +
                getExceptions(exec) +
                body;
    }

    /**
     * Return Unicode representation of given {@link String}
     *
     * @param text is {@link String} with source
     * @return {@link String} with UNICODE format of given string
     */
    private static String toUnicode(final String text) {
        final StringBuilder b = new StringBuilder();
        for (final char c : text.toCharArray()) {
            if (c >= 128) {
                b.append(String.format("\\u%04X", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Write given text. Wrap {@link IOException} into {@link ImplerException}
     *
     * @param writer is {@link BufferedWriter}
     * @param text is {@link String} which contains given text
     * @throws ImplerException if we catch {@link IOException} while writing
     */
    private void write(final BufferedWriter writer, final String text) throws ImplerException {
        try {
            writer.write(toUnicode(text));
        } catch (final IOException e) {
            throw new ImplerException("Error while writing to output file", e);
        }
    }

    /**
     * Filter non-private declared {@link Constructor}s and write their implementations
     *
     * @param token is {@link Class} {@link Constructor}s are taken from
     * @param writer is {@link BufferedWriter}
     * @throws ImplerException if there is no non-private constructors
     */
    private void implementConstructors(final Class<?> token, final BufferedWriter writer) throws ImplerException {
        final Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(x -> !Modifier.isPrivate(x.getModifiers()))
                .toArray(Constructor<?>[]::new);
        if (constructors.length == 0) {
            throw new ImplerException("There is no non-private constructors");
        }
        for (final Constructor<?> constructor : constructors) {
            write(writer, getExecutable(constructor, getClassName(token),
                    getExecFullBody("super" + getArgs(constructor, false))));
        }
    }

    /**
     * Wrap all methods into {@link MethodwithEq} and add its to a {@link HashSet}
     *
     * @param methods is {@link Method} array which we wrap and add
     * @param methodsSet is {@link HashSet} all methods add to
     */
    private void addMethods(final Method[] methods, final HashSet<MethodwithEq> methodsSet) {
        Arrays.stream(methods)
                .map(MethodwithEq::new)
                .forEach(methodsSet::add);
    }

    /**
     * Take abstract {@link Method}s of given {@link Class} and his parents and write their implementations
     *
     * @param token is {@link Class} which abstract {@link Method}s we need to implement
     * @param writer is {@link BufferedWriter}
     * @throws ImplerException if there are troubles with writing
     */
    private void implementAbstractMethods(Class<?> token, final BufferedWriter writer) throws ImplerException {
        HashSet<MethodwithEq> methods = new HashSet<>();
        addMethods(token.getMethods(), methods);
        while (token != null) {
            addMethods(token.getDeclaredMethods(), methods);
            token = token.getSuperclass();
        }
        methods = methods.stream()
                .filter(x -> Modifier.isAbstract(x.getMethod().getModifiers()))
                .collect(Collectors.toCollection(HashSet::new));
        for (final MethodwithEq method : methods) {
            final Method cur = method.getMethod();
            write(writer, getExecutable(cur, getMethodTypeAndName(cur),
                    getExecFullBody("return " + getDefaultValue(cur.getReturnType()))));
        }
    }

    /**
     * Return path to file including packages with specific file extension
     *
     * @param path is {@link Path} of given {@code token}
     * @param token is given {@link Class}
     * @param end is {@link String} of extension
     * @return {@link Path}
     */
    private Path getFilePath(final Path path, final Class<?> token, final String end) {
        return path.resolve(token.getPackage().getName().replace('.', File.separatorChar))
                .resolve(getClassName(token) + end);
    }

    /**
     * Create parent directories to given {@link Path}
     *
     * @param path is {@link Path} to create parent directory
     * @throws ImplerException if there is error with creating directories
     */
    private void createDirectories(final Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (final IOException e) {
                throw new ImplerException("Error with creating directories to output", e);
            }
        }
    }

    /**
     * Create implementation class of given {@link Class} and put it near
     *
     * @param token type token ({@link Class}) to create implementation for.
     * @param root is {@link Path} which is root directory.
     * @throws ImplerException with message of error
     */
    @Override
    public void implement(final Class<?> token, Path root) throws ImplerException {
        if (token == null) {
            throw new NullPointerException("Null type-token");
        }
        if (root == null) {
            throw new NullPointerException("Null path to type-token");
        }

        if (token.isPrimitive() || token.isArray()
                || Modifier.isFinal(token.getModifiers()) || Modifier.isPrivate(token.getModifiers())
                || token == Enum.class) {
            throw new ImplerException("Incorrect class token");
        }

        root = getFilePath(root, token, ".java");
        createDirectories(root);

        try (final BufferedWriter writer = Files.newBufferedWriter(root)) {
            write(writer, getClassDeclaration(token));
            if (!token.isInterface()) {
                implementConstructors(token, writer);
            }
            implementAbstractMethods(token, writer);
            write(writer, "}" + ENDL);
        } catch (final IOException e) {
            throw new ImplerException("Error with creating output", e);
        }
    }

    //---------------------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------------------
    //---------------------------------------------------------------------------------------------------------


    /**
     * File visitor which clears all files and directories
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        /**
         * Delete file after visiting
         */
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Delete directory after processing inside files
         */
        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Delete all files and directories in given {@link Path}
     *
     * @param root is {@link Path} which need to be cleaned
     * @throws IOException if there is error while deleting or visiting files or directories
     */
    private static void clean(final Path root) throws IOException {
        if (Files.exists(root)) {
            Files.walkFileTree(root, DELETE_VISITOR);
        }
    }

    /**
     * Get name of {@link ZipEntry} for given {@link Class}.
     *
     * @param token class token to get name of {@link ZipEntry}
     * @return {@link String} representation of name of {@link ZipEntry}
     */
    private String getZipEntryName(final Class<?> token) {
        return token.getPackageName().replace('.', '/')
                + "/" + getClassName(token)
                + ".class";
    }

    /**
     * Generate path to given {@link Class}
     *
     * @param token is type token ({@link Class})
     * @return {@link String} of path to {@code token}
     */
    private static String getClassPath(final Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Makes jar file of implementation of given {@link Class}
     *
     * @param token is {@link Class} to create implementation for.
     * @param output destination of {@code .jar} file.
     * @throws ImplerException with message of error
     */
    @Override
    public void implementJar(final Class<?> token, final Path output) throws ImplerException {
        if (token == null) {
            throw new NullPointerException("Null type-token");
        }
        if (output == null) {
            throw new NullPointerException("Null path to type-token");
        }
        createDirectories(output);
        final Path tempDir;
        try {
            tempDir = Files.createTempDirectory(output.toAbsolutePath().getParent(), "tmp");
        } catch (final IOException e) {
            throw new ImplerException("Error while creating temp directory", e);
        }
        try {
            implement(token, tempDir);
            compile(token, tempDir);
            createJar(token, output, tempDir);
        } finally {
            try {
                clean(tempDir);
            } catch (final IOException e) {
                System.out.println("Error with deleting temp directory: " + e.getMessage());
            }
        }
    }

    /**
     * Compile implementation of given {@link Class}
     *
     * @param token is {@link Class} to compile implementation for.
     * @param tempDir is {@link Path} to temp directory where files will be compiled
     * @throws ImplerException if there is error with compilation
     */
    private void compile(final Class<?> token, final Path tempDir) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final ArrayList<String> args = new ArrayList<>();
        args.add("-cp");
        args.add(tempDir.toString() + File.pathSeparator + System.getProperty("java.class.path")
                + File.pathSeparator + getClassPath(token));
        args.add(getFilePath(tempDir, token, ".java").toString());
        if (compiler == null || compiler.run(null, null, null, args.toArray(String[]::new)) != 0) {
            throw new ImplerException("Error with compiling generated files");
        }
    }

    /**
     * Create {@code .jar} file of implementation of given {@link Class}
     *
     * @param token is {@link Class} to create implementation {@code jar} for.
     * @param output is {@link Path} to result file
     * @param tempDir is {@link Path} to compiled files
     * @throws ImplerException if there is error with writing to {@code jar} file
     */
    private void createJar(final Class<?> token, final Path output, final Path tempDir) throws ImplerException {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Egor Kurbatov");
        try (final JarOutputStream writer = new JarOutputStream(Files.newOutputStream(output), manifest)) {
            writer.putNextEntry(new ZipEntry(getZipEntryName(token)));
            Files.copy(getFilePath(tempDir, token, ".class"), writer);
        } catch (final IOException e) {
            throw new ImplerException("Error while writing to JAR file", e);
        }
    }

    /**
     * Check arguments for null
     *
     * @param args is array of arguments
     * @return false if arguments are all not-null
     */
    static boolean checkArgs(final String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                System.out.println(i + " arg is null");
                return true;
            }
        }
        return false;
    }

    /**
     * Generate implementation of given {@link Class}
     *
     * @param args: 2 arguments (class name and path) for generating implementation
     */
    public static void main(final String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Bad args");
            return;
        }
        if (checkArgs(args)) {
            return;
        }
        try {
            final JarImpler impl = new Implementor();
            impl.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (final ImplerException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
