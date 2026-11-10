package com.charliestenacity.loadtest;

final class RateGate {
    private final long intervalNanos;
    private long next;

    RateGate(int requestsPerSecond) {
        this.intervalNanos = 1_000_000_000L / Math.max(1, requestsPerSecond);
        this.next = System.nanoTime();
    }

    synchronized void acquire() throws InterruptedException {
        long now = System.nanoTime();
        long wait = next - now;
        if (wait > 0) {
            long millis = wait / 1_000_000L;
            int nanos = (int) (wait % 1_000_000L);
            Thread.sleep(millis, nanos);
            now = System.nanoTime();
        }
        next = Math.max(next, now) + intervalNanos;
    }
}

