package com.angrysurfer.core.api;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dedicated real-time dispatcher for timing events.
 * <p>
 * Unlike CommandBus, this has no string maps, no wildcard support,
 * and minimal allocation overhead — optimized for ultra-high-frequency
 * deterministic timing updates.
 */
public class TimingBus {

    private static TimingBus instance;

    private final CopyOnWriteArrayList<IBusListener> listeners = new CopyOnWriteArrayList<>();

    private TimingBus() {
    }

    public static TimingBus getInstance() {
        if (instance == null) {
            instance = new TimingBus();
        }
        return instance;
    }

    public void register(IBusListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void unregister(IBusListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public boolean isRegistered(IBusListener listener) {
        return listeners.contains(listener);
    }

    /**
     * Publish a timing command to all registered listeners.
     *
     * @param cmd the command to publish
     */
    public void publish(Command cmd) {
        if (cmd == null || cmd.getCommand() == null) {
            return;
        }
        for (IBusListener listener : listeners) {
            if (listener != cmd.getSender()) {
                try {
                    listener.onAction(cmd);
                } catch (Exception e) {
                    System.err.println("Error in timing listener " + listener.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Convenience method to publish a timing command with sender and data.
     *
     * @param command the timing command name
     * @param sender  the sender of the command
     * @param data    the command data
     */
    public void publish(String command, Object sender, Object data) {
        publish(new Command(command, sender, data));
    }

    public void shutdown() {
        listeners.clear();
    }
}
