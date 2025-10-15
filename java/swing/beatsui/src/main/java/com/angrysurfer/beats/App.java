package com.angrysurfer.beats;

import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIErrorHandler;
import com.angrysurfer.core.Constants;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.service.SoundbankManager;
import com.angrysurfer.core.service.UserConfigManager;
import com.formdev.flatlaf.FlatLightLaf;

public class App implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());

    private static final CommandBus commandBus = CommandBus.getInstance();

    private static final boolean showSplash = true;
    private static SplashScreen splash;
    private Frame frame;

    public App() {
        // Register for theme changes using the correct method
        CommandBus.getInstance().register(this, new String[]{Commands.CHANGE_THEME});
    }

    public static void main(String[] args) {
        // Configure logging first
        // System.setProperty("java.util.logging.config.file", "src/main/resources/logging.properties");

        try {
            logger.info("Starting application...");

            // Create and initialize splash screen synchronously
            splash = new SplashScreen();
            splash.setTaskCount(8);

            // Show splash screen using SwingUtilities.invokeLater
            SwingUtilities.invokeLater(() -> {
                splash.setVisible(showSplash);
                splash.setStatus("Initializing application...");

                // Only start initialization AFTER splash screen is visible
                // This ensures splash is initialized before background work begins
                startInitialization();
            });
        } catch (Exception e) {
            if (splash != null)
                splash.dispose();
            handleInitializationFailure("Fatal error during application startup", e);
        }
    }

    /**
     * Start the background initialization process
     */
    private static void startInitialization() {
        // Create a separate thread for initialization
        new Thread(() -> {
            try {
                // Initialize services in background
                initializeServices();

                // Setup UI on EDT when services are ready
                SwingUtilities.invokeLater(() -> {
                    try {
                        splash.setStatus("Setting up Look and Feel...");
                        setupLookAndFeel();

                        splash.setStatus("Creating main window...");
                        App app = new App();

                        splash.setStatus("Loading application...");
                        app.createAndShowGUI();

                        // Dispose splash screen after short delay
                        splash.setProgress(100);
                        splash.setStatus("Ready!");

                        // Small delay before closing splash screen
                        try {
                            Thread.sleep(600);
                        } catch (InterruptedException e) {
                            // Ignore
                        }

                        splash.dispose();
                        logger.info("Application started successfully");
                    } catch (Exception e) {
                        if (splash != null)
                            splash.dispose();
                        handleInitializationFailure("Failed to create application window", e);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (splash != null)
                        splash.dispose();
                    handleInitializationFailure("Fatal error during application startup", e);
                });
            }
        }).start();
    }

    private static void setupLookAndFeel() {
        try {
            // Add logging
            logger.info("Loading frame state for Look and Feel");
            FrameState state = RedisService.getInstance().loadFrameState(Constants.APPLICATION_FRAME);
            logger.info("Frame state loaded: {}", state != null ? state.getLookAndFeelClassName() : "null");

            if (state != null && state.getLookAndFeelClassName() != null) {
                UIManager.setLookAndFeel(state.getLookAndFeelClassName());
                logger.info("Set Look and Feel to: {}", state.getLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                logger.info("Set default Look and Feel: FlatLightLaf");
            }

            splash.completeTask("Applied visual theme");
        } catch (Exception e) {
            logger.error("Error setting look and feel: {}", e.getMessage());

        }
    }

    private static void initializeServices() {
        try {
            // Initialize RedisService first
            RedisService redisService = RedisService.getInstance();
            logger.info("Redis service initialized");
            splash.completeTask("Connected to database");

            // Initialize MIDI device manager explicitly
            DeviceManager.getInstance().initialize();
            splash.completeTask("Detected MIDI devices");

            // Initialize SoundbankManager BEFORE the internal synthesizer initialization
            SoundbankManager.getInstance().initializeSoundbanks();
            SoundbankManager.getInstance().ensureSoundbanksLoaded();
            splash.completeTask("Loaded soundbanks");

            // Initialize the internal synthesizer and related synth data
            // using the explicit initialize() entrypoint to avoid constructor side-effects
            InternalSynthManager.getInstance().initialize();
            splash.completeTask("Loaded internal synthesizer");

            // Initialize instrument management and the InstrumentManager explicitly
            InstrumentHelper instrumentHelper = redisService.getInstrumentHelper();
            List<InstrumentWrapper> instruments = instrumentHelper.findAllInstruments();
            logger.info("Found {} instruments in Redis", instruments.size());

            InstrumentManager.getInstance().initialize();
            splash.completeTask("Loaded instrument configurations");

            UserConfigManager.getInstance();
            UserConfigManager.getInstance().initialize();

            splash.completeTask("Loaded user configuration");

            // Initialize SessionManager AFTER instruments are loaded
            SessionManager sessionManager = SessionManager.getInstance();
            sessionManager.initialize();
            logger.info("Session manager initialized");
            splash.completeTask("Initialized session manager");

            // Ensure channel consistency in PlayerManager
            // Initialize PlayerManager explicitly and then ensure channel consistency
            PlayerManager.getInstance().initialize();
            PlayerManager.getInstance().ensureChannelConsistency();

            // Signal system ready - this will trigger waiting sequencers to initialize
            CommandBus.getInstance().publish(Commands.SYSTEM_READY, App.class, null);
            logger.info("System initialization complete");

        } catch (Exception e) {
            handleInitializationFailure("Failed to initialize services", e);
        }
    }

    private static void handleInitializationFailure(String errorMessage, Exception e) {
    logger.error("Critical initialization error: {}", errorMessage);
    logger.error("Exception caught during initialization", e);

        SwingUtilities.invokeLater(() -> {
            String fullMessage = String.format("""
                    Failed to initialize application: %s
                    
                    Error details: %s
                    
                    Please ensure Redis is running and try again.
                    
                    The application will now exit.""", errorMessage, e.getMessage());

            JOptionPane.showMessageDialog(null, fullMessage, "Initialization Error", JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        });
    }

    private void createAndShowGUI() {
        frame = new Frame();

        UIErrorHandler.initialize(frame);

        frame.loadFrameState();
        frame.setVisible(true);
        UIErrorHandler.initialize(frame);
    }

    @Override
    public void onAction(Command action) {
        if (Commands.CHANGE_THEME.equals(action.getCommand())) {
            SwingUtilities.invokeLater(() -> {
                try {
                    frame.saveFrameState();
                    String newLafClass = (String) action.getData();
                    UIManager.setLookAndFeel(newLafClass);
                    final Frame oldFrame = frame;

                    Frame newFrame = new Frame();
                    newFrame.loadFrameState();
                    newFrame.setVisible(true);

                    oldFrame.close();

                    frame = newFrame;
                    CommandBus.getInstance().publish(Commands.THEME_CHANGED, App.class, null);

                } catch (Exception e) {
                    logger.error("Error handling theme change: {}", e.getMessage());
                }
            });
        }
    }

}
