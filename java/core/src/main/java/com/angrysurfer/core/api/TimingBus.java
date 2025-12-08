package com.angrysurfer.core.api;

import com.angrysurfer.core.sequencer.TimingUpdate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-performance timing bus optimized for frequent timing updates.
 * Uses direct synchronous dispatch for minimal latency.
 */
public class TimingBus extends AbstractBus {
    private static TimingBus instance;
    
    private final List<IBusListener> timingListeners = new CopyOnWriteArrayList<>();

    private TimingBus() {
        super();
        logger.debug("TimingBus initialized");
    }

    public static TimingBus getInstance() {
        if (instance == null) {
            instance = new TimingBus();
        }
        return instance;
    }

    public void register(IBusListener listener) {
        if (listener == null) {
            logger.warn("Attempted to register null listener");
            return;
        }
        
        if (!timingListeners.contains(listener)) {
            timingListeners.add(listener);
            logger.debug("Registered timing listener: {}", listener.getClass().getSimpleName());
        }
    }

    @Override
    public void register(IBusListener listener, String[] commands) {
        register(listener);
        super.register(listener, commands);
    }

    @Override
    public void unregister(IBusListener listener) {
        if (listener != null) {
            timingListeners.remove(listener);
            super.unregister(listener);
            logger.debug("Unregistered timing listener: {}", listener.getClass().getSimpleName());
        }
    }

    public boolean isRegistered(IBusListener listener) {
        return timingListeners.contains(listener);
    }

    @Override
    public void publish(String command, Object sender, Object data) {
        // Fast path for timing updates - direct dispatch
        if (Commands.TIMING_UPDATE.equals(command) && data instanceof TimingUpdate update) {
            publishTimingUpdate(update);
        } else {
            // For other commands, use parent implementation
            super.publish(command, sender, data);
        }
    }

    public void publishTimingUpdate(TimingUpdate update) {
        if (update == null) {
            return;
        }
        
        Command command = new Command(Commands.TIMING_UPDATE, this, update);
        
        // Direct dispatch - no reactive overhead
        for (IBusListener listener : timingListeners) {
            try {
                listener.onAction(command);
            } catch (Exception e) {
                // Minimal error handling for performance
                logger.error("Error in timing listener {}: {}", 
                    listener.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down TimingBus");
        timingListeners.clear();
        super.shutdown();
    }
}
