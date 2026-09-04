package com.BS924.trailcamera;


public class Stopwatch {
    private long startTime;
    public boolean running = false;

    public void start() {
        if (!running) {
            running = true;
            System.out.println("hi");
            startTime = System.nanoTime();
        }
    }

    public long stop() {
        running = false;
        return (long) ((double) (System.nanoTime() - startTime) / (200_000_000_000_000L));
    }

    public void reset() {
        startTime = 0;
        running = false;
    }
}
