package com.angrysurfer.core.api;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.TimingUpdate;


public class TimingBus extends AbstractBus {
    private static final String[] EMPTY = new String[]{};
    private static TimingBus instance;
    private static final Logger LOG = LoggerFactory.getLogger(TimingBus.class);
    // Initialize field BEFORE constructor is called
    private final ConcurrentLinkedQueue<IBusListener> timingListeners = new ConcurrentLinkedQueue<>();
    // removed unused shared command and diagnostic fields

    // Constructor must be after field initialization
    private TimingBus() {
        // We'll handle registration ourselves instead of relying on parent
        // Don't call super() which calls register() before fields are initialized

        // Log initialization at debug level
        logger.debug("TimingBus initialized with {} listeners", timingListeners.size());

        // Diagnostic thread removed; use logging if needed
    }

    public static TimingBus getInstance() {
        if (instance == null) {
            instance = new TimingBus();
        }
        return instance;
    }

    @Override
    public void publish(String commandName, Object source, Object data) {
    // Add immediate diagnostic output
    // logger.debug("TimingBus: Publishing {} listeners: {}", commandName, timingListeners.size());

        if (Commands.TIMING_UPDATE.equals(commandName)) {
            // DON'T reuse the shared command for timing - create a new one for thread
            // safety
            Command cmd = new Command(commandName, source, data);

            // Fast path for timing updates - call listeners directly
            for (IBusListener listener : timingListeners) {
                if (listener != source) { // Avoid sending to self
                    try {
                        listener.onAction(cmd);
                    } catch (Exception e) {
                        LOG.error("Error in timing listener: {}", e.getMessage(), e);
                    }
                }
            }
            // return;
        }

        // For non-timing events, use parent implementation
        // super.publish(commandName, source, data);
    }

    // Add a method to check registration
    public boolean isRegistered(IBusListener listener) {
        return timingListeners.contains(listener);
    }

    // previously had an updateSharedCommand helper; removed as unused

    // Add a specialized method for highest performance timing events
    public void publishTimingUpdate(TimingUpdate update) {
        // Even more optimized path for timing updates
        Command command = new Command(Commands.TIMING_UPDATE, this, update);
        for (IBusListener listener : timingListeners) {
            try {
                ((Player) listener).onTick(update); // Direct call to avoid command overhead
            } catch (ClassCastException e) {
                // Fall back to standard method if not a Player
                listener.onAction(command);
            } catch (Exception e) {
                // Minimal error handling
            }
        }
    }

    public void register(IBusListener listener) {
        register(listener, EMPTY);
    }

    @Override
    public void register(IBusListener listener, String[] commands) {
        if (listener != null) {
            if (timingListeners == null) {
                LOG.error("TimingBus: timingListeners is null!");
                return;
            }

            if (!timingListeners.contains(listener)) {
                timingListeners.add(listener);
                LOG.debug("TimingBus: Registered listener: {}", listener.getClass() != null ? listener.getClass().getSimpleName() + ": " + listener : "null");
            }
        }
    }

    @Override
    public void unregister(IBusListener listener) {
        if (listener != null && timingListeners != null) {
            timingListeners.remove(listener);
            // logger.debug("TimingBus: Unregistered listener: {}",
            //         (listener.getClass() != null ? listener.getClass().getSimpleName() : "null"));
        }
    }
}
