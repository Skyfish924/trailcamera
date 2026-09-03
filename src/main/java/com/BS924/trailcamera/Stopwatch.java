package com.BS924.trailcamera;

public class Stopwatch {
    private long startTime;

    public void start() {
        startTime = System.nanoTime();
    }

    public long stop() {
        return System.nanoTime() - startTime;
    }
}
