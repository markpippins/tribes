package com.angrysurfer.core.api;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractBus {

    public static String WILDCARD = "*";
    static Logger logger = LoggerFactory.getLogger(Player.class.getCanonicalName());
    private final Map<String, List<IBusListener>> listenerMap = new ConcurrentHashMap<>();

    private final List<IBusListener> listeners = new CopyOnWriteArrayList<>();
    private final LogManager logManager = LogManager.getInstance();

    // Thread pool for asynchronous command processing
    private final ExecutorService commandExecutor;
    private final boolean asyncProcessing;

    protected AbstractBus() {
        this(true, Runtime.getRuntime().availableProcessors());
        // DON'T call register(this) here - it's unsafe during initialization
        // If subclasses need to register with themselves, they should do it explicitly
        // after their fields are initialized
    }

    public AbstractBus(boolean asyncProcessing, int threadPoolSize) {
        this.asyncProcessing = asyncProcessing;

        // Create named thread factory for better debugging
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, getClass().getSimpleName() + "-" + threadNumber.getAndIncrement());
                t.setDaemon(true); // Don't prevent JVM from exiting
                return t;
            }
        };

        // Create thread pool if using async processing
        if (asyncProcessing) {
            commandExecutor = Executors.newFixedThreadPool(threadPoolSize, threadFactory);
            logManager.info("AbstractBus", "Created async command bus with " + threadPoolSize + " threads");
        } else {
            commandExecutor = null;
            logManager.info("AbstractBus", "Created synchronous command bus");
        }

        // Register self to handle logging commands
        // register(this);
    }

    public void register(IBusListener listener, String[] commands) {
        for (String action : commands) {
            listenerMap
                    .computeIfAbsent(action, k -> new ArrayList<>())
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
    // logger.debug("AbstractBus: Publishing command {} to {} listeners", command, listeners.size());
        Command cmd = new Command(command, sender, data);

        List<IBusListener> listeners = listenerMap.get(command);
        if (listeners != null) {
            for (IBusListener listener : listeners) {
                // logger.info("Notifying: " + listener.getClass().getSimpleName());
                if (!listener.equals(sender))
                    listener.onAction(cmd);
            }
        }

        listeners = listenerMap.get("*");
        if (listeners != null) {
            for (IBusListener listener : listeners) {
                // logger.info("Notifying: " + listener.getClass().getSimpleName());
                if (!listener.equals(sender))
                    listener.onAction(cmd);
            }
        }
    }

    /**
     * Publishes a command to be processed by all registered listeners.
     * Depending on the bus configuration, processing will happen either
     * synchronously in the current thread or asynchronously in the thread pool.
     *
     * @param action The command to publish
     */
    public void publish(Command action) {
        if (action == null) {
            logManager.error("CommandBus", "Attempted to publish null action");
            return;
        }

        // String sender = action.getSender() != null ? action.getSender().getClass().getSimpleName() : "unknown";
        // String dataType = action.getData() != null ? action.getData().getClass().getSimpleName() : "null";

        // logManager.debug("CommandBus",
        //         String.format("Publishing command: %s from: %s data: %s",
        //                 action.getCommand(), sender, dataType));

        // Use executor service if running async, otherwise process in current thread
        if (asyncProcessing && commandExecutor != null) {
            commandExecutor.submit(() -> processCommand(action));
        } else {
            processCommand(action);
        }
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
    private void processCommand(Command action) {
        if (action == null || action.getCommand() == null) {
            logManager.error("CommandBus", "Attempted to process null action or command");
            return;
        }

        // Get listeners for this specific command
        List<IBusListener> listeners = new ArrayList<>();

        List<IBusListener> commandListeners = listenerMap.get(action.getCommand());
        if (Objects.nonNull(commandListeners))
            listeners.addAll(commandListeners);

        // Get listeners registered for all commands (wildcard)
        List<IBusListener> wildcardListeners = listenerMap.get(WILDCARD);
        if (Objects.nonNull(wildcardListeners))
            listeners.addAll(wildcardListeners);

        // Process specific command listeners
        if (commandListeners != null) {
            for (IBusListener listener : listeners) {
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
                    logger.error("Error in listener {} handling command {}", listener.getClass().getSimpleName(), action.getCommand(), e);
                }
            }
        }

        // Process wildcard listeners
        if (wildcardListeners != null) {
            for (IBusListener listener : wildcardListeners) {
                try {
                    if (listener != action.getSender()) {
                        listener.onAction(action);
                    }
                } catch (Exception e) {
                    logManager.error("CommandBus",
                            String.format("Error in wildcard listener %s handling command %s: %s",
                                    listener.getClass().getSimpleName(),
                                    action.getCommand(),
                                    e.getMessage()));
                    logger.error("Error in wildcard listener {} handling command {}", listener.getClass().getSimpleName(), action.getCommand(), e);
                }
            }
        }

    // Remove debug log that was causing console spam
    // logger.debug("CommandBus: processing {} listeners: {}", action.toString(), listeners.size());
    }

    /**
     * Shutdown the command executor gracefully
     */
    public void shutdown() {
        if (commandExecutor != null && !commandExecutor.isShutdown()) {
            logManager.info("AbstractBus", "Shutting down command executor");
            commandExecutor.shutdown();
            try {
                // Wait for existing tasks to terminate
                if (!commandExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logManager.warn("AbstractBus", "Command executor did not terminate in time, forcing shutdown");
                    commandExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logManager.error("AbstractBus", "Command executor shutdown was interrupted", e);
                commandExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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
