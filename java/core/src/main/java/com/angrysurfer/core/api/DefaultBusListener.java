package com.angrysurfer.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBusListener implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBusListener.class);

    // IBusListener implementation
    @Override
    public void onAction(Command action) {
        // Let the interface try to handle it with our registered handlers
        if (!tryHandleCommand(action)) {
            // Handle any commands not registered with lambdas
            logger.warn("Unhandled command: {}", action.getCommand());
        }
    }

}
