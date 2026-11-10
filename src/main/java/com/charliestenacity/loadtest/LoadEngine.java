package com.charliestenacity.loadtest;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

final class LoadEngine {
    record Config(String baseUrl, String username, char[] password, List<String> endpoints,
                  int users, int durationSeconds, int rampSeconds, int maxRps, int thinkMillis) {}

    private final Config config;
    private final Consumer<LoadSample> sink;
    private final Consumer<String> log;
    private final AtomicBoolean stop = new AtomicBoolean();
    private final AtomicLong cursor = new AtomicLong();
    private ExecutorService pool;

    LoadEngine(Config config, Consumer<LoadSample> sink, Consumer<String> log) {
        this.config = config; this.sink = sink; this.log = log;
    }

    void run() throws InterruptedException {
        pool = Executors.newFixedThreadPool(config.users());
        RateGate gate = new RateGate(config.maxRps());
        CountDownLatch finished = new CountDownLatch(config.users());
        long deadline = System.nanoTime() + Duration.ofSeconds(config.durationSeconds()).toNanos();
        long launchDelay = config.users() <= 1 ? 0 : Duration.ofSeconds(config.rampSeconds()).toMillis() / config.users();

        for (int i = 0; i < config.users() && !stop.get(); i++) {
            int userNumber = i + 1;
            pool.submit(() -> { try { runUser(userNumber, deadline, gate); } finally { finished.countDown(); } });
            if (launchDelay > 0) Thread.sleep(launchDelay);
        }
        long remaining = Math.max(1, config.durationSeconds() + config.rampSeconds() + 35L);
        finished.await(remaining, TimeUnit.SECONDS);
        stop.set(true); pool.shutdownNow(); pool.awaitTermination(5, TimeUnit.SECONDS);
        java.util.Arrays.fill(config.password(), '\0');
    }

    void stop() { stop.set(true); if (pool != null) pool.shutdownNow(); }

    private void runUser(int number, long deadline, RateGate gate) {
        try {
            VirtualUserClient client = new VirtualUserClient(config.baseUrl());
            if (!config.username().isBlank()) {
                VirtualUserClient.LoginResult result = client.login(config.username(), config.password());
                if (result.mfaRequired()) { log.accept("User " + number + ": MFA required; use a dedicated non-MFA test account."); return; }
                if (result.status() != 200) { log.accept("User " + number + ": login returned HTTP " + result.status()); return; }
            }
            while (!stop.get() && System.nanoTime() < deadline) {
                String endpoint = config.endpoints().get((int)(cursor.getAndIncrement() % config.endpoints().size()));
                gate.acquire();
                long start = System.nanoTime();
                try {
                    HttpResponse<Void> response = client.get(endpoint);
                    sink.accept(new LoadSample(System.currentTimeMillis(), endpoint, response.statusCode(), Duration.ofNanos(System.nanoTime()-start).toMillis(), null));
                } catch (Exception e) {
                    sink.accept(new LoadSample(System.currentTimeMillis(), endpoint, 0, Duration.ofNanos(System.nanoTime()-start).toMillis(), e.getClass().getSimpleName()+": "+e.getMessage()));
                }
                if (config.thinkMillis() > 0) Thread.sleep(config.thinkMillis());
            }
        } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        catch (Exception e) { log.accept("User " + number + " stopped: " + e.getMessage()); }
    }
}

