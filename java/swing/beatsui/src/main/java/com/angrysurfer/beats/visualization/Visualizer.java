package com.angrysurfer.beats.visualization;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.visualization.handler.music.ScrollingSequencerVisualization;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Visualizer implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(Visualizer.class);

    private static final int VISUALIZATION_DELAY = 300; // 30 seconds
    private static final int VISUALIZATION_CHANGE_DELAY = 100; // 10 seconds * 6 = 1 minu
    private final JComponent parent;
    private JButton[][] buttons;
    private Timer animationTimer;
    private IVisualizationHandler currentVisualization = null;
    private Random random = new Random();
    private boolean isVisualizationMode = false;
    private long lastInteraction;
    private Timer visualizationTimer;
    private Timer visualizationChangeTimer;
    private List<IVisualizationHandler> visualizations = new ArrayList<>();

    private boolean isLocked = false; // Add this field

    public Visualizer(JComponent parent, JButton[][] buttons) {
        this.parent = parent;
        this.buttons = buttons;
        initializeVisualizations();
        setupTimers();
        setupAnimation();
        CommandBus.getInstance().register(this, new String[]{
                Commands.START_VISUALIZATION,
                Commands.STOP_VISUALIZATION,
                Commands.LOCK_CURRENT_VISUALIZATION,
                Commands.UNLOCK_CURRENT_VISUALIZATION,
                Commands.VISUALIZATION_SELECTED,
                Commands.VISUALIZATION_HANDLER_REFRESH_REQUESTED,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STATE_CHANGED,
                Commands.PLAYER_SELECTION_EVENT,
                Commands.TRANSPORT_STOP
        });
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.START_VISUALIZATION:
                startVisualizer();
                isVisualizationMode = true;
                break;

            case Commands.STOP_VISUALIZATION:
                stopVisualizer();
                isVisualizationMode = false;
                isLocked = false; // Reset lock state when stopping
                break;

            case Commands.LOCK_CURRENT_VISUALIZATION:
                isLocked = true;
                // Don't need to do anything else - current visualization will keep running
                break;

            case Commands.UNLOCK_CURRENT_VISUALIZATION:
                isLocked = false;
                // Resume random switching by restarting the change timer
                visualizationChangeTimer.start();
                break;

            case Commands.VISUALIZATION_SELECTED:
                startVisualizer((IVisualizationHandler) action.getData());
                break;

            case Commands.VISUALIZATION_HANDLER_REFRESH_REQUESTED:
                refreshVisualizations();
                break;

            case Commands.TRANSPORT_START:
            case Commands.TRANSPORT_STATE_CHANGED: // <-- Add this case
                // For state changed, check if it's true (playing)
                // if (Commands.TRANSPORT_STATE_CHANGED.equals(action.getCommand())
                //         && !(action.getData() instanceof Boolean && (Boolean) action.getData())) {
                //     break; // Only proceed if transport is starting
                // }

                stopVisualizer();
                // Find and start the ScrollingSequencerVisualization
                // for (IVisualizationHandler vis : visualizations) {
                //     if (vis instanceof ScrollingSequencerVisualization) {
                //         startVisualizer(vis);
                //         isLocked = true; // Lock to prevent auto-switching during sequencer mode
                //         break;
                //     }
                // }
                break;

            case Commands.PLAYER_SELECTION_EVENT:
                stopVisualizer();
                // Find and start the StrikeVisualizationHandler
                // for (IVisualizationHandler vis : visualizations) {
                //     if (vis instanceof StrikeVisualizationHandler) {
                //         startVisualizer(vis);
                //         isLocked = true; // Lock to prevent auto-switching during sequencer mode
                //         break;
                //     }
                // }
                break;

            case Commands.TRANSPORT_STOP:
                // Only stop visualization if we were showing the sequencer
                if (currentVisualization instanceof ScrollingSequencerVisualization) {
                    stopVisualizer();
                    isLocked = false; // Unlock when stopping
                }
                break;
        }
    }

    private void refreshVisualizations() {
        visualizations.clear();
        visualizations = getVisualizations();
        for (IVisualizationHandler handler : visualizations) {
            CommandBus.getInstance().publish(Commands.VISUALIZATION_REGISTERED, this, handler);
        }
    }

    private List<IVisualizationHandler> getVisualizations() {
        List<IVisualizationHandler> visualizations = new ArrayList<>();

        try {
            // Get this class's package as the starting point
            String basePackage = this.getClass().getPackage().getName();
            // Go up one level to get the root package
            basePackage = basePackage.substring(0, basePackage.lastIndexOf('.'));

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            scanPackageForVisualizations(basePackage, classLoader, visualizations);
        } catch (Exception e) {
            logger.error("Error scanning for visualizations", e);
        }

        return visualizations;
    }

    private void scanPackageForVisualizations(String packageName, ClassLoader classLoader,
                                              List<IVisualizationHandler> visualizations) {
        try {
            String path = packageName.replace('.', '/');
            java.net.URL resource = classLoader.getResource(path);

            if (resource == null) {
                logger.warn("Package not found: {}", packageName);
                return;
            }

            if (resource.getProtocol().equals("jar")) {
                // Handle JAR files
                String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(
                        java.net.URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                    java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(path) && name.endsWith(".class")) {
                            String className = name.substring(0, name.length() - 6).replace('/', '.');
                            processClass(className, visualizations);
                        }
                    }
                }
            } else {
                // Handle directory structure
                java.io.File directory = new java.io.File(resource.getFile());
                if (directory.exists()) {
                    scanDirectory(directory, packageName, visualizations);
                }
            }
        } catch (Exception e) {
            logger.error("Error scanning package {}", packageName, e);
        }
    }

    private void scanDirectory(java.io.File directory, String packageName,
                               List<IVisualizationHandler> visualizations) {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                String fileName = file.getName();
                if (file.isDirectory()) {
                    // Recursive call for subdirectories
                    scanDirectory(file, packageName + "." + fileName, visualizations);
                } else if (fileName.endsWith(".class")) {
                    // Process class files
                    String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                    processClass(className, visualizations);
                }
            }
        }
    }

    private void processClass(String className, List<IVisualizationHandler> visualizations) {
        try {
            Class<?> cls = Class.forName(className);
            if (IVisualizationHandler.class.isAssignableFrom(cls)
                    && !java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) {
                IVisualizationHandler handler = (IVisualizationHandler) cls.getDeclaredConstructor()
                        .newInstance();
                visualizations.add(handler);
            }
        } catch (Exception e) {
            logger.error("Failed to load visualization: {}", className, e);
        }
    }

    private void initializeVisualizations() {
        refreshVisualizations();
    }

    private void setupTimers() {
        lastInteraction = System.currentTimeMillis();

        visualizationTimer = new Timer(1000, e -> checkVisualizer());
        visualizationTimer.start();

        visualizationChangeTimer = new Timer(VISUALIZATION_CHANGE_DELAY, e -> {
            if (isVisualizationMode && !isLocked) { // Only change if not locked
                setDisplayMode(getRandomVisualization());
            }
        });
    }

    private IVisualizationHandler getRandomVisualization() {
        return visualizations.get(random.nextInt(visualizations.size()));
    }

    public void checkVisualizer() {
        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteraction;
        if (!isVisualizationMode && timeSinceLastInteraction > VISUALIZATION_DELAY) {
            startVisualizer();
        }
    }

    public void startVisualizer() {
        startVisualizer(getRandomVisualization());
    }

    public void startVisualizer(IVisualizationHandler handler) {
        isVisualizationMode = true;
        visualizationChangeTimer.stop(); // Don't auto-change during sequencer mode
        setDisplayMode(handler);
        CommandBus.getInstance().publish(Commands.VISUALIZATION_STARTED, this, handler);
    }

    public void stopVisualizer() {
        isVisualizationMode = false;
        CommandBus.getInstance().publish(Commands.VISUALIZATION_STOPPED, this, null);
        visualizationChangeTimer.stop();
        clearDisplay();
        currentVisualization = null; // Reset current mode
        lastInteraction = System.currentTimeMillis(); // Reset timer
    }

    private void setupAnimation() {
        animationTimer = new Timer(100, e -> updateDisplay());
        animationTimer.start();
    }

    private void setDisplayMode(IVisualizationHandler visualization) {
        currentVisualization = visualization;
        if (Objects.nonNull(currentVisualization) && currentVisualization instanceof LockHandler) {
            ((LockHandler) currentVisualization).lockDisplay();
        }

        clearDisplay();
    }

    private void clearDisplay() {
        for (JButton[] button : buttons) {
            for (int col = 0; col < buttons[0].length; col++) {
                button[col].setText("");
                button[col].setToolTipText("");
                button[col].setBackground(getParent().getBackground());

            }
        }
    }

    public void updateDisplay() {
        if (!isVisualizationMode || currentVisualization == null) {
            return;
        }

//        CommandBus.getInstance().publish(
//                Commands.STATUS_UPDATE,
//                this,
//                new StatusUpdate("Visualizer", "", currentVisualization.getName())
//        );

        try {
            currentVisualization.update(buttons);
        } catch (Exception e) {
            logger.error("{} Error updating display", currentVisualization != null ? currentVisualization.getName() : "<unknown>", e);
        }
    }

}
