package com.angrysurfer.core.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreciseTimer implements Timer {
    private static final Logger logger = LoggerFactory.getLogger(PreciseTimer.class);
    private volatile int bpm;
    private volatile int ppq;
    private final AtomicInteger pendingBpm = new AtomicInteger(-1);
    private final AtomicInteger pendingPpq = new AtomicInteger(-1);
    private final AtomicLong ticks = new AtomicLong(0);
    private volatile boolean running = true;
    private Runnable tickCallback;
    private Runnable beatCallback;
    private static final long SPIN_YIELD_THRESHOLD = 1_000_000; // 1ms in nanos

    public PreciseTimer(int bpm, int ppq) {
        this.bpm = bpm;
        this.ppq = ppq;
    }

    @Override
    public void addTickCallback(Runnable callback) {
        this.tickCallback = callback;
    }

    @Override
    public void addBeatCallback(Runnable callback) {
        this.beatCallback = callback;
    }

    @Override
    public void stop() {
        logger.info("stop() - stopping timer");
        running = false;
    }

    @Override
    public synchronized void setBpm(int newBpm) {
        logger.info("setBpm() - setting new BPM: {}", newBpm);
        pendingBpm.set(newBpm);
    }

    @Override
    public synchronized void setPpq(int newPpq) {
        logger.info("setPpq() - setting new PPQ: {}", newPpq);
        pendingPpq.set(newPpq);
    }

    @Override
    public int getBpm() {
        return bpm;
    }

    @Override
    public int getPpq() {
        return ppq;
    }

    @Override
    public void run() {
        logger.info("run() - starting timer with BPM: {}, PPQ: {}", bpm, ppq);
        long intervalNanos = (long) ((60.0 / bpm / ppq) * 1_000_000_000);
        long nextTick = System.nanoTime();

        while (running) {
            long currentTime = System.nanoTime();
            long waitTime = nextTick - currentTime;
            
            if (waitTime <= 0) {
                // Time to tick
                long tickNum = ticks.incrementAndGet();
                logger.debug("Tick: {} at time: {}", tickNum, System.nanoTime());
                
                if (tickCallback != null) {
                    tickCallback.run();
                }
                
                // Check for pending changes and beat callback at beat boundaries
                if (tickNum % ppq == 0) {
                    updateTimingIfNeeded(tickNum);
                    if (beatCallback != null) {
                        beatCallback.run();
                    }
                }
                
                nextTick += intervalNanos;
            } else {
                handleWaiting(waitTime);
            }
        }
    }

    private void updateTimingIfNeeded(long tickNum) {
        int newBpm = pendingBpm.get();
        if (newBpm > 0) {
            logger.info("Applying new BPM: {} at tick: {}", newBpm, tickNum);
            bpm = newBpm;
            pendingBpm.set(-1);
        }
        
        int newPpq = pendingPpq.get();
        if (newPpq > 0) {
            logger.info("Applying new PPQ: {} at tick: {}", newPpq, tickNum);
            ppq = newPpq;
            pendingPpq.set(-1);
        }
    }

    private void handleWaiting(long waitTime) {
        if (waitTime > SPIN_YIELD_THRESHOLD) {
            try {
                Thread.sleep(waitTime / 1_000_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            Thread.onSpinWait();
        }
    }

    @Override
    public long getCurrentTick() {
        return ticks.get();
    }

    public static void main(String[] args) {
        // Create a timer at 120 BPM with 24 pulses per quarter note
        PreciseTimer timer = new PreciseTimer(120, 24);

        // Add a tick listener that prints the current tick count
        timer.addTickCallback(() -> {
            if (logger.isDebugEnabled()) logger.debug("Tick: {}", timer.getCurrentTick());
        });

        // Add a beat listener that prints when a beat occurs
        timer.addBeatCallback(() -> {
            if (logger.isDebugEnabled()) logger.debug("Beat! Current BPM: {}", timer.getBpm());
        });

        // Start the timer in a separate thread
        Thread timerThread = new Thread(timer);
        timerThread.start();

        // Demonstrate BPM changes after a few seconds
        try {
            Thread.sleep(2000);  // Run for 2 seconds
            if (logger.isDebugEnabled()) logger.debug("Changing BPM to 140...");
            timer.setBpm(140);
            
            Thread.sleep(2000);  // Run for 2 more seconds
            if (logger.isDebugEnabled()) logger.debug("Changing BPM to 100...");
            timer.setBpm(100);
            
            Thread.sleep(2000);  // Run for 2 final seconds
            timer.stop();  // Stop the timer
            
            // Wait for the timer thread to finish
            timerThread.join();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("PreciseTimer main interrupted", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}