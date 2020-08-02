package ru.ifmo.rain.kurbatov.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    private static final int BUF_SIZE = 4096;

    private static final String FORMAT = "%08x";

    private static void writeHash(BufferedWriter out, int hash, String path) throws IOException {
        out.write(String.format(FORMAT, hash) + " " + path);
        out.newLine();
    }

    public static class FileHashVisitor extends SimpleFileVisitor<Path> {
        private final BufferedWriter out;

        FileHashVisitor (BufferedWriter writer) {
            out = writer;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            String fileName = path.toString();
            int fileHash = calcFileHash(fileName);
            writeHash(out, fileHash, fileName);
            return FileVisitResult.CONTINUE;
        }
    }

    private static int calcFileHash(String name) {
        try (InputStream in = Files.newInputStream(Paths.get(name))) {
            int h = 0x811c9dc5, bytesRead = 0;
            byte[] buffer = new byte[BUF_SIZE];
            while (true) {
                while ((bytesRead = in.read(buffer)) == 0) {}
                if (bytesRead == -1)
                    break;
                for (int i = 0; i < bytesRead; i++) {
                    h = (h * 0x01000193) ^ (buffer[i] & 0xff);
                }
            }
            return h;
        } catch (InvalidPathException e) {
            log(name + ": Invalid path. Hash set to zero", e);
            return 0;
        } catch (IOException e) {
            log("Error while hashing " + name + ": Hash set to zero", e);
            return 0;
        }
    }

    private static class FileHasherException extends Exception {
        String message;

        FileHasherException(final String message, Exception anc) {
            super(anc);
            this.message = anc.getMessage() + ". " + message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    private static Path createPath(String name, String argName) throws FileHasherException {
        try {
            return Paths.get(name);
        } catch (InvalidPathException e) {
            throw new FileHasherException(argName + " " +  "has invalid path.", e);
        }
    }

    private static void log(String message, Exception e) {
        System.err.println(e.getMessage() + ". " + message);
    }

    private static void processFileHashing(final String input, final String output) throws FileHasherException {
        Path inPath = createPath(input, "Input");
        Path outPath = createPath(output, "Output");

        try {
            Path parent = outPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new FileHasherException("Error creating directories to output", e);
        }

        try (BufferedWriter out = Files.newBufferedWriter(outPath)) {
            try (BufferedReader in = Files.newBufferedReader(inPath)) {
                String path;
                while ((path = in.readLine()) != null) {
                    try {
                        Files.walkFileTree(Paths.get(path), new FileHashVisitor(out));
                    } catch (InvalidPathException e) {
                        writeHash(out, 0, path);
                        log(path + ": Invalid path.", e);
                    } catch (IOException e) {
                        writeHash(out, 0, path);
                        log("Error while reading " + path, e);
                    }
                }
            } catch (InvalidPathException e) {
                throw new FileHasherException(input + "Invalid path.", e);
            } catch (IOException e) {
                throw new FileHasherException("Error with creating reader", e);
            }
        } catch (InvalidPathException e) {
            throw new FileHasherException(output + ": Invalid path.", e);
        } catch (IOException e) {
            throw new FileHasherException("Error with creating writer", e);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.print("Bad args count");
            return;
        }

        if (args[0] == null || args[1] == null) {
            System.out.print("Bad args");
            return;
        }

        try {
            processFileHashing(args[0], args[1]);
        } catch (FileHasherException e) {
            System.out.println(e.getMessage());
        }
    }
}
