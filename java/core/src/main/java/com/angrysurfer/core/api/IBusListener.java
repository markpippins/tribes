package com.angrysurfer.core.api;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@FunctionalInterface
public interface IBusListener {
    // Original method
    void onAction(Command action);
    
    // Static map to store handlers for each instance
    static final Map<IBusListener, Map<String, Consumer<Command>>> COMMAND_HANDLERS = new WeakHashMap<>();
    
    /**
     * Register a command handler and store the lambda for direct invocation
     * 
     * @param command The command to register for
     * @param handler The lambda function to handle the command
     */
    default void registerCommand(String command, Consumer<Command> handler) {
        // Register with CommandBus for this command
        CommandBus.getInstance().register(this, new String[]{command});
        
        // Get or create the map for this listener instance
        Map<String, Consumer<Command>> handlers = COMMAND_HANDLERS.computeIfAbsent(this, k -> new HashMap<>());
        
        // Store the handler lambda
        handlers.put(command, handler);
    }
    
    /**
     * Manually invoke a registered command handler
     * 
     * @param command The command to handle
     */
    default void handleCommand(String command) {
        Map<String, Consumer<Command>> handlers = COMMAND_HANDLERS.get(this);
        if (handlers != null) {
            Consumer<Command> handler = handlers.get(command);
            if (handler != null) {
                // Create a dummy command to pass to the handler
                Command dummyCommand = new Command(command, this, null);
                handler.accept(dummyCommand);
            }
        }
    }

    /**
     * Try to handle a command using the registered handlers
     * Useful in onAction implementations
     * 
     * @param action The command to handle
     * @return true if the command was handled
     */
    default boolean tryHandleCommand(Command action) {
        if (action == null) return false;
        
        Map<String, Consumer<Command>> handlers = COMMAND_HANDLERS.get(this);
        if (handlers != null && action.getCommand() != null) {
            Consumer<Command> handler = handlers.get(action.getCommand());
            if (handler != null) {
                handler.accept(action);
                return true;
            }
        }
        return false;
    }
}

// Example usage in a class
// public class MyListener implements IBusListener {
    
//     public MyListener() {
//         // Register a command with a lambda handler
//         registerCommand(Commands.SYSTEM_READY, this::handleSystemReady);
//         registerCommand(Commands.PLAYER_UPDATED, this::handlePlayerUpdated);
//     }
    
//     // Handler methods
//     private void handleSystemReady(Command cmd) {
//         logger.debug("System is ready!");
//     }
//     
//     private void handlePlayerUpdated(Command cmd) {
//         logger.debug("Player updated: {}", cmd.getData());
//     }
    
//     // IBusListener implementation
//     @Override
//     public void onAction(Command action) {
//         // Let the interface try to handle it with our registered handlers
//         if (!tryHandleCommand(action)) {
//             // Handle any commands not registered with lambdas
//             logger.debug("Unhandled command: {}", action.getCommand());
//         }
//     }
// }