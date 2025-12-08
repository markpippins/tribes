package com.angrysurfer.core.api;

import com.angrysurfer.core.service.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-performance event bus with direct synchronous dispatch.
 * Optimized for low latency and minimal overhead.
 */
public abstract class AbstractBus {

    public static final String WILDCARD = "*";
    protected static final Logger logger = LoggerFactory.getLogger(AbstractBus.class);
    
    private final Map<String, List<IBusListener>> listenerMap = new ConcurrentHashMap<>();
    private final LogManager logManager = LogManager.getInstance();

    protected AbstractBus() {
        logger.debug("Created event bus");
    }

    public void register(IBusListener listener, String[] commands) {
        if (listener == null) {
            logger.warn("Attempted to register null listener");
            return;
        }
        
        for (String action : commands) {
            if (action == null || action.isEmpty()) {
                logger.warn("Skipping null or empty command for listener: {}", 
                    listener.getClass().getSimpleName());
                continue;
            }
            
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
        Command cmd = new Command(command, sender, data);
        publish(cmd);
    }

    public void publish(Command action) {
        if (action == null) {
            logManager.error("AbstractBus", "Attempted to publish null action");
            return;
        }

        processCommand(action);
    }

    protected void processCommand(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        List<IBusListener> commandListeners = listenerMap.get(action.getCommand());
        List<IBusListener> wildcardListeners = listenerMap.get(WILDCARD);

        if (commandListeners != null) {
            notifyListeners(commandListeners, action);
        }

        if (wildcardListeners != null) {
            notifyListeners(wildcardListeners, action);
        }
    }

    private void notifyListeners(List<IBusListener> listeners, Command action) {
        for (IBusListener listener : listeners) {
            if (listener == action.getSender()) {
                continue;
            }
            
            try {
                listener.onAction(action);
            } catch (Exception e) {
                logManager.error("AbstractBus",
                    String.format("Error in listener %s handling command %s",
                        listener.getClass().getSimpleName(),
                        action.getCommand()),
                    e);
            }
        }
    }

    public void shutdown() {
        logManager.info("AbstractBus", "Shutting down event bus");
        listenerMap.clear();
    }

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

    public record LogMessage(String source, String message, Throwable throwable) {
        public LogMessage(String source, String message) {
            this(source, message, null);
        }
    }
}
