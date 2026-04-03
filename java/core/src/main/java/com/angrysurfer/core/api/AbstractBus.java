package com.angrysurfer.core.api;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.LogManager;

public abstract class AbstractBus {

    public static String WILDCARD = "*";
    static Logger logger = LoggerFactory.getLogger(Player.class.getCanonicalName());
    private final Map<String, List<IBusListener>> listenerMap = new ConcurrentHashMap<>();
    private final LogManager logManager = LogManager.getInstance();

    protected AbstractBus() {}

    public AbstractBus(boolean asyncProcessing, int threadPoolSize) {}

    public void register(IBusListener listener, String[] commands) {
        for (String action : commands) {
            listenerMap
                    .computeIfAbsent(action, k -> new CopyOnWriteArrayList<>())
                    .add(listener);
        }
    }

    public void unregister(IBusListener listener) {
        Iterator<Map.Entry<String, List<IBusListener>>> iterator = listenerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<IBusListener>> entry = iterator.next();
            entry.getValue().remove(listener);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    public void publish(String command) {
        publish(new Command(command, this, this));
    }

    public void publish(String command, Object sender) {
        Command cmd = new Command(command, sender, null);
        publish(cmd);
    }

    public void publish(String command, Object sender, Object data) {
        publish(new Command(command, sender, data));
    }

    /**
     * Publishes a command synchronously on the calling thread.
     * UI listeners should marshal to EDT themselves if needed.
     */
    public void publish(Command action) {
        if (action == null) {
            logManager.error("CommandBus", "Attempted to publish null action");
            return;
        }
        processCommand(action);
    }

    /**
     * Publish a command with immediate execution regardless of async setting.
     * Use this for commands that must be processed immediately.
     *
     * @param action The command to publish immediately
     */
    public void publishImmediate(Command action) {
        if (action == null) {
            logManager.error("CommandBus", "Attempted to publish null action");
            return;
        }

        processCommand(action);
    }

    /**
     * Process the command by notifying all listeners
     */
    protected void processCommand(Command action) {
        if (action == null || action.getCommand() == null) {
            logManager.error("CommandBus", "Attempted to process null action or command");
            return;
        }

        Set<IBusListener> toNotify = new LinkedHashSet<>();

        List<IBusListener> commandListeners = listenerMap.get(action.getCommand());
        if (commandListeners != null) {
            toNotify.addAll(commandListeners);
        }

        List<IBusListener> wildcardListeners = listenerMap.get(WILDCARD);
        if (wildcardListeners != null) {
            toNotify.addAll(wildcardListeners);
        }

        for (IBusListener listener : toNotify) {
            try {
                if (listener != action.getSender()) {
                    listener.onAction(action);
                }
            } catch (Exception e) {
                logManager.error("CommandBus",
                        String.format("Error in listener %s handling command %s: %s",
                                listener.getClass().getSimpleName(),
                                action.getCommand(),
                                e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    /**
     * Shutdown the command executor gracefully
     */
    public void shutdown() {
        // no-op: synchronous bus has no executor to shut down
    }

//    @Override
//    public void onAction(Command action) {
//        // Handle logging commands
//        if (action.getData() instanceof LogMessage msg) {
//            switch (action.getCommand()) {
//                case Commands.LOG_DEBUG -> logManager.debug(msg.source(), msg.message());
//                case Commands.LOG_INFO -> logManager.info(msg.source(), msg.message());
//                case Commands.LOG_WARN -> logManager.warn(msg.source(), msg.message());
//                case Commands.LOG_ERROR -> {
//                    if (msg.throwable() != null) {
//                        logManager.error(msg.source(), msg.message(), msg.throwable());
//                    } else {
//                        logManager.error(msg.source(), msg.message());
//                    }
//                }
//            }
//        }
//    }

    // Helper methods to publish log messages
    public void debug(String source, String message) {
        publish(Commands.LOG_DEBUG, this, new LogMessage(source, message));
    }

    public void info(String source, String message) {
        publish(Commands.LOG_INFO, this, new LogMessage(source, message));
    }

    public void warn(String source, String message) {
        publish(Commands.LOG_WARN, this, new LogMessage(source, message));
    }

    public void error(String source, String message) {
        publish(Commands.LOG_ERROR, this, new LogMessage(source, message));
    }

    public void error(String source, String message, Throwable e) {
        publish(Commands.LOG_ERROR, this, new LogMessage(source, message, e));
    }

    // Helper record for log messages
    public record LogMessage(String source, String message, Throwable throwable) {
        public LogMessage(String source, String message) {
            this(source, message, null);
        }
    }
}
