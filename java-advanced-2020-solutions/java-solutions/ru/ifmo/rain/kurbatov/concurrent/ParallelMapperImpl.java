package ru.ifmo.rain.kurbatov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Implementation of {@link ParallelMapper}
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> tasks;
    private final List<Thread> threads;
    private final Set<ResultCollector<?>> collectors;
    private boolean closed;

    /**
     * Constructor makes indicated number of {@link Thread} and start them.
     *
     * @param threadsCount is number of {@link Thread} which are using for calculations
     */
    public ParallelMapperImpl(final int threadsCount) {
        if (threadsCount <= 0) {
            throw new IllegalArgumentException("Threads count must be positive");
        }
        collectors = new HashSet<>();
        tasks = new ArrayDeque<>();
        threads = new ArrayList<>();
        closed = false;
        for (int i = 0; i < threadsCount; i++) {
            final Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        pollTask().run();
                    }
                } catch (final InterruptedException ignored) {
                    // pass
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
    }

    private Runnable pollTask() throws InterruptedException {
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            return tasks.poll();
        }
    }

    private void addTask(final Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
    }

    private class ResultCollector<R> {
        private final List<R> res;
        private int done;
        private RuntimeException exception;
        private boolean needFinish;

        ResultCollector(final int size) {
            res = new ArrayList<>(Collections.nCopies(size, null));
            synchronized (ParallelMapperImpl.this) {
                collectors.add(ResultCollector.this);
            }
        }

        synchronized void addException(final RuntimeException e) {
            if (needFinish) {
                return;
            }

            if (exception == null) {
                exception = e;
            } else {
                exception.addSuppressed(e);
            }
        }

        synchronized void shutdown() {
            needFinish = true;
            notify();
        }

        synchronized void set(final int pos, final R data) {
            if (needFinish) {
                return;
            }

            res.set(pos, data);
            if (++done == res.size()) {
                notify();
            }
        }

        synchronized List<R> get() throws InterruptedException {
            while (done < res.size() && !needFinish) {
                wait();
            }
            synchronized (ParallelMapperImpl.this) {
                collectors.remove(ResultCollector.this);
            }
            if (exception != null) {
                throw exception;
            }
            return res;
        }
    }

    /**
     * Apply given {@link Function} to given arguments. Calculations are spread on several parallel threads.
     *
     * @param f is given {@link Function} to apply
     * @param args is {@link List} of arguments to apply {@code f} on
     * @param <T> is type of given arguments
     * @param <R> is type of mapped arguments via {@code f}
     * @return {@link List} of results
     * @throws InterruptedException if thread-workers were interrupted
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        synchronized (this) {
            if (closed) {
                throw new RuntimeException("Mapper is closed");
            }
        }
        final ResultCollector<R> collector = new ResultCollector<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int pos = i;
            addTask(() -> {
                try {
                    collector.set(pos, f.apply(args.get(pos)));
                } catch (final RuntimeException e) {
                    collector.addException(e);
                }
            });
        }
        return collector.get();
    }

    /**
     * Stops all calculations. Threads are made interrupted.
     */
    @Override
    synchronized public void close() {
        closed = true;
        threads.forEach(Thread::interrupt);
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (final InterruptedException ignored) {
                i--;
            }
        }
        List.copyOf(collectors).forEach(ResultCollector::shutdown);
    }
}