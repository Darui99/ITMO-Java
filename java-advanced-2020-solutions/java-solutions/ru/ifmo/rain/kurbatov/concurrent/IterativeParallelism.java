package ru.ifmo.rain.kurbatov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {

    private final ParallelMapper mapper;

    public IterativeParallelism() {
        mapper = null;
    }

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private void joinThreads(final List<Thread> threads) throws InterruptedException {
        InterruptedException exception = null;
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (final InterruptedException e) {
                if (exception == null) {
                    exception = new InterruptedException("Thread was interrupted");
                    for (int j = i; j < threads.size(); j++) {
                        threads.get(j).interrupt();
                    }
                }
                exception.addSuppressed(e);
                i--;
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private <T> List<List<T>> split(final int cnt, final List<T> arr) {
        final int block = arr.size() / cnt;
        final int rem = arr.size() % cnt;
        int r = 0;

        final List<List<T>> res = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            final int l = r;
            r = l + block + (i < rem ? 1 : 0);
            res.add(arr.subList(l, r));
        }
        return res;
    }

    private <T, R> List<R> processParallelFunc(final List<List<T>> chunks,
                                               final Function<? super Stream<T>, R> func) throws InterruptedException {
        final List<R> res = new ArrayList<>(Collections.nCopies(chunks.size(), null));
        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            final int pos = i;
            threads.add(new Thread(() -> res.set(pos, func.apply(chunks.get(pos).stream()))));
            threads.get(i).start();
        }
        joinThreads(threads);
        return res;
    }

    private <T, R> R getParallelFunc(int threadsCount, final List<T> arr,
                                     final Function<? super Stream<T>, R> func,
                                     final Function<? super Stream<R>, R> merger) throws InterruptedException {
        if (threadsCount <= 0) {
            throw new IllegalArgumentException("Threads count must be positive");
        }
        threadsCount = Math.min(threadsCount, arr.size());

        final List<List<T>> chunks = split(threadsCount, arr);
        final List<R> res = mapper == null ? processParallelFunc(chunks, func) : mapper.map(lst -> func.apply(lst.stream()), chunks);
        return merger.apply(res.stream());
    }

    /**
     * Calculate maximum element via comparator in given {@link List}. Calculations are spread on several parallel threads.
     *
     * @param threads number or concurrent threads.
     * @param values is {@link List} values to get maximum of.
     * @param comparator value {@link Comparator}.
     * @param <T> is type of given values
     * @return maximum value in given {@link List}
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("List is empty");
        }
        final Function<Stream<? extends T>, T> max = st -> st.max(comparator).orElse(null);
        return getParallelFunc(threads, values, max, max);
    }

    /**
     * Calculate minum element via comparator in given {@link List}. Calculations are spread on several parallel threads.
     *
     * @param threads number or concurrent threads.
     * @param values is {@link List} values to get maximum of.
     * @param comparator value {@link Comparator}.
     * @param <T> is type of given values
     * @return minimum value in given {@link List}
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Check that all elements of given {@link List} satisfy given {@link Predicate}. Calculations are spread on several parallel threads.
     *
     * @param threads number or concurrent threads.
     * @param values is {@link List} values to test.
     * @param predicate test {@link Predicate}.
     * @param <T> is type of given values
     * @return true if all elements satisfy {@link Predicate}
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return getParallelFunc(threads, values, st -> st.allMatch(predicate), st -> st.allMatch(Boolean::booleanValue));
    }

    /**
     * Check that any element of given {@link List} satisfies given {@link Predicate}. Calculations are spread on several parallel threads.
     *
     * @param threads number or concurrent threads.
     * @param values is {@link List} values to test.
     * @param predicate test {@link Predicate}.
     * @param <T> is type of given values
     * @return true if any element satisfies {@link Predicate}
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, x -> !predicate.test(x));
    }

    /**
     * Join elements of given {@link List} to a single {@link String}. Calculations are spread on several parallel threads.
     *
     * @param threads number of concurrent threads.
     * @param values is {@link List} values to test.
     * @return {@link String} of joining all elements
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return getParallelFunc(threads, values,
                st -> st.map(Object::toString).collect(Collectors.joining()),
                st -> st.collect(Collectors.joining()));
    }

    private static <T> List<T> collectToList(final Stream<? extends List<T>> st) {
        return st.flatMap(List::stream).collect(Collectors.toList());
    }

    private <T, U> List<U> applyStreamFunc(final int threads, final List<? extends T> values,
                                           final Function<Stream<? extends T>, Stream<? extends U>> f) throws InterruptedException {
        return getParallelFunc(threads, values,
                st -> f.apply(st).collect(Collectors.toList()),
                IterativeParallelism::collectToList);
    }

    /**
     * Filter elements of given {@link List} via given {@link Predicate}. Calculations are spread on several parallel threads.
     *
     * @param threads number of concurrent threads.
     * @param values is {@link List} values to filter.
     * @param predicate filter {@link Predicate}.
     * @param <T> is type of given values
     * @return {@link List} of elements that satisfy given {@link Predicate}
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return applyStreamFunc(threads, values, st -> st.filter(predicate));
    }

    /**
     * Map elements of given {@link List} via given {@link Function}. Calculations are spread on several parallel threads.
     *
     * @param threads number of concurrent threads.
     * @param values  is {@link List} values to map.
     * @param f is mapper {@link Function}.
     * @param <T> is type of given values
     * @param <U> is type of matched values
     * @return {@link List} of mapped elements
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return applyStreamFunc(threads, values, st -> st.map(f));
    }

    private static <T> Function<Stream<T>, T> getReducer(final Monoid<T> monoid) {
        return st -> st.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    /**
     * Reduce elements of given {@link List} via given monoid (contains {@link java.util.function.BinaryOperator} and identity elem).
     * Calculations are spread on several parallel threads.
     *
     * @param threads number of concurrent threads.
     * @param values is {@link List} values to reduce.
     * @param monoid is {@link info.kgeorgiy.java.advanced.concurrent.AdvancedIP.Monoid} monoid to use.
     * @param <T> is type of given values
     * @return reduced value
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        final Function<Stream<T>, T> red = getReducer(monoid);
        return getParallelFunc(threads, values, red, red);
    }

    /**
     * Map-Reduce elements of given {@link List} via given {@link Function} and given monoid (contains {@link java.util.function.BinaryOperator} and identity elem).
     * Calculations are spread on several parallel threads.
     *
     * @param threads number of concurrent threads.
     * @param values is {@link List} values to map-reduce.
     * @param lift is mapping {@link Function}.
     * @param monoid is {@link info.kgeorgiy.java.advanced.concurrent.AdvancedIP.Monoid} monoid to use.
     * @param <T> is type of given values
     * @param <R> is type of matched values
     * @return map-reduced value
     * @throws InterruptedException if some thread was interrupted
     */
    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        final Function<Stream<R>, R> red = getReducer(monoid);
        return getParallelFunc(threads, values,
                st -> red.apply(st.map(lift)), red);
    }
}
