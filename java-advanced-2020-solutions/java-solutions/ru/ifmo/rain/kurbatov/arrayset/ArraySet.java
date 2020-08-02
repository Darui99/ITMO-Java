package ru.ifmo.rain.kurbatov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        data = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(final Collection<? extends T> st) {
        comparator = null;
        data = List.copyOf((new TreeSet<>(st)));
    }

    public ArraySet(final Collection<? extends T> st, final Comparator<? super T> cmp) {
        comparator = cmp;
        TreeSet<T> tmp = new TreeSet<>(cmp);
        tmp.addAll(st);
        data = List.copyOf(tmp);
    }

    private ArraySet(List<T> arr, Comparator<? super T> cmp) {
        comparator = cmp;
        data = arr;
    }

    private T getElem(int pos) {
        return (pos == -1 || pos == size() ? null : data.get(pos));
    }

    @Override
    public T lower(T elem) {
        return getElem(getBound(elem, false, false));
    }

    @Override
    public T floor(T elem) {
        return getElem(getBound(elem, true, false));
    }

    @Override
    public T ceiling(T elem) {
        return getElem(getBound(elem, true, true));
    }

    @Override
    public T higher(T elem) {
        return getElem(getBound(elem, false, true));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("Set is immutable");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("Set is immutable");
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversableList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (comparator == null) {
            if (fromElement instanceof Comparable) {
                if (((Comparable<? super T>) fromElement).compareTo(toElement) > 0) {
                    throw new IllegalArgumentException("Incorrect args order");
                }
            } else {
                throw new IllegalArgumentException("fromElem is incomparable");
            }
        } else if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Incorrect args order");
        }
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    private int getBound(T elem, boolean inclusive, boolean greater) {
        int pos = Collections.binarySearch(data, elem, comparator);
        if (pos >= 0) {
            if (inclusive) {
                return pos;
            } else {
                return (greater ? ++pos : --pos);
            }
        }
        return (greater ? ~pos : ~pos - 1);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (toElement == null) {
            throw new IllegalArgumentException("toElem is null");
        }
        return new ArraySet<>(data.subList(0, getBound(toElement, inclusive, false) + 1), comparator);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (fromElement == null) {
            throw new IllegalArgumentException("fromElem is null");
        }
        return new ArraySet<>(data.subList(getBound(fromElement, inclusive, true), size()), comparator);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException("Collection is empty");
        }
    }

    @Override
    public T first() {
        checkEmpty();
        return data.get(0);
    }

    @Override
    public T last() {
        checkEmpty();
        return data.get(data.size() - 1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object elem) {
        return (Collections.binarySearch(data, (T) Objects.requireNonNull(elem), comparator) >= 0);
    }

    private static class ReversableList<T> extends AbstractList<T> {
        private boolean rev;
        private final List<T> data;

        public int size() {
            return data.size();
        }

        private ReversableList(final List<T> arr) {
            if (arr instanceof ReversableList) {
                data = ((ReversableList<T>)arr).data;
                rev = !((ReversableList<T>)arr).rev;
            } else {
                data = arr;
                rev = true;
            }
        }

        @Override
        public T get(int pos) {
            return data.get(rev ? size() - pos - 1 : pos);
        }
    }
}
