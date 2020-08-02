package ru.ifmo.rain.kurbatov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link Crawler} for downloading web-sites
 */
public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final Map<String, HostQueue> hostQueueMap;
    private final Map<Document, String> docsUrl;
    private final static int AWAIT_TERM_SEC = 60;

    /**
     * Constructor-method
     * @param downloader is {@link Downloader}
     * @param downloaders is limit of simultaneous downloadings
     * @param extractors is limit of simultaneous extracting links
     * @param perHost is limit of simultaneous downloadings from one host
     */
    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        hostQueueMap = new ConcurrentHashMap<>();
        docsUrl = new ConcurrentHashMap<>();
        this.perHost = perHost;
    }

    private class ResultCollector {
        private final Set<String> downloaded;
        private final Map<String, IOException> errors;
        private final Set<String> visited;
        private final Queue<Future<?>> running;
        private final Phaser phaser;

        ResultCollector() {
            downloaded = Collections.newSetFromMap(new ConcurrentHashMap<>());
            errors = new ConcurrentHashMap<>();
            visited = Collections.newSetFromMap(new ConcurrentHashMap<>());
            running = new ConcurrentLinkedQueue<>();
            phaser = new Phaser(0);
        }

        public Queue<Future<?>> getRunning() {
            return running;
        }

        public Set<String> getVisited() {
            return visited;
        }

        public void addPage(final String url) {
            downloaded.add(url);
        }

        public void addError(final String url, final IOException e) {
            errors.put(url, e);
        }

        public Result getResult() {
            return new Result(new ArrayList<>(downloaded), errors);
        }

        public Phaser getPhaser() {
            return phaser;
        }

        public void downloadPage(final String url, final Collection<Document> downloadedDocs) {
            try {
                final Document doc = downloader.download(url);
                downloadedDocs.add(doc);
                docsUrl.put(doc, url);
                addPage(url);
            } catch (final IOException e) {
                addError(url, e);
            }
        }

        public void addToDownload(final String url, final Queue<Document> downloadedDocs) {
            final String host;
            try {
                host = URLUtils.getHost(url);
            } catch (final MalformedURLException e) {
                addError(url, e);
                return;
            }
            hostQueueMap.computeIfAbsent(host, k -> new HostQueue())
                    .addAndProcessTask(() -> downloadPage(url, downloadedDocs), this);
        }

        public List<String> extractLinks(final Document doc) {
            List<String> res = Collections.emptyList();
            try {
                res =  doc.extractLinks();
            } catch (final IOException e) {
                addError(docsUrl.get(doc), e);
            }
            return res;
        }

        public Runnable createProcessTask(final Document doc, final Queue<Document> nextLayer) {
            return () -> extractLinks(doc).forEach(u -> {
                if (!visited.contains(u)) {
                    visited.add(u);
                    phaser.register();
                    addToDownload(u, nextLayer);
                }
            });
        }

        private Result download(final String url, final int depth) {
            final List<Document> layer = new ArrayList<>();
            final Queue<Document> nextLayer = new ConcurrentLinkedQueue<>();

            downloadPage(url, layer);
            getVisited().add(url);
            for (int curDepth = 1; !layer.isEmpty() && curDepth < depth; curDepth++) {
                layer.stream().map(doc -> extractorsPool.submit(createProcessTask(doc, nextLayer)))
                        .collect(Collectors.toList())
                        .forEach(WebCrawler::getFromFuture);
                phaser.awaitAdvance(0);
                running.forEach(WebCrawler::getFromFuture);
                running.clear();
                layer.clear();
                layer.addAll(nextLayer);
                nextLayer.clear();
                layer.forEach(docsUrl::remove);
            }
            return getResult();
        }
    }

    private class HostQueue {
        final Queue<Runnable> queue;
        final Queue<ResultCollector> collectors;
        int free;

        public HostQueue() {
            queue = new ArrayDeque<>();
            collectors = new ArrayDeque<>();
            free = perHost;
        }

        public synchronized void addAndProcessTask(final Runnable task, final ResultCollector collector) {
            queue.add(() -> {
                task.run();
                collector.getPhaser().arriveAndDeregister();
                synchronized (HostQueue.this) {
                    free++;
                    processTasks();
                }
            });
            collectors.add(collector);
            processTasks();
        }

        public synchronized void processTasks() {
            if (free > 0 && !queue.isEmpty() && !collectors.isEmpty()) {
                free--;
                collectors.poll().getRunning().add(downloadersPool.submit(Objects.requireNonNull(queue.poll())));
            }
        }
    }

    private static <T> void getFromFuture(final Future<T> elem) {
        try {
            elem.get();
        } catch (final InterruptedException | ExecutionException e) {
            // pass
        }
    }

    /**
     * Download web-sites
     * @param url is site which download starts from.
     * @param depth download depth.
     * @return a {@link Result} of downloading
     */
    @Override
    public Result download(final String url, final int depth) {
        return new ResultCollector().download(url, depth);
    }

    private void awaitTerm(final ExecutorService executorService) {
        try {
            executorService.awaitTermination(AWAIT_TERM_SEC, TimeUnit.SECONDS);
        } catch (final InterruptedException ignored) {
            // pass
        }
    }

    /**
     * Interrupt threads.
     */
    @Override
    public void close() {
        downloadersPool.shutdown();
        extractorsPool.shutdown();

        awaitTerm(downloadersPool);
        awaitTerm(extractorsPool);
    }

    public static void main(final String[] args) {
        if (args == null) {
            System.out.println("Bad args");
            return;
        }
        if (args.length < 1 || args.length > 5) {
            System.out.println("Bad args count");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                System.out.println(i + " arg is null");
                return;
            }
        }

        final int[] params = new int[4];
        params[0] = 1; // depth
        params[1] = 10; // downloaders
        params[2] = 10; // extractors
        params[3] = 3; // perHost

        try {
            for (int i = 1; i < args.length; i++) {
                params[i - 1] = Integer.parseInt(args[i]);
            }
        } catch (final NumberFormatException e) {
            System.out.println("Bad numbers in arguments: " + e.getMessage());
            return;
        }

        try (final WebCrawler crawler = new WebCrawler(new CachingDownloader(), params[1], params[2], params[3])) {
            crawler.download(args[0], params[0]);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
