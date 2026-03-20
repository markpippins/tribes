package com.angrysurfer.grid.visualization;

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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Visualizer {

    private static final Logger logger = LoggerFactory.getLogger(Visualizer.class);

    private static final int VISUALIZATION_DELAY = 300; // 30 seconds
    private static final int VISUALIZATION_CHANGE_DELAY = 100; // 10 seconds * 6 = 1 minute
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
        // Start visualizer automatically
        startVisualizer();
    }

    private void refreshVisualizations() {
        visualizations.clear();
        visualizations = getVisualizations();
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
        if (visualizations.isEmpty()) {
            return null;
        }
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
        if (handler == null) return;
        
        isVisualizationMode = true;
        visualizationChangeTimer.stop(); // Don't auto-change during sequencer mode
        setDisplayMode(handler);
        logger.info("Started visualization: {}", handler.getName());
    }

    public void stopVisualizer() {
        isVisualizationMode = false;
        visualizationChangeTimer.stop();
        clearDisplay();
        currentVisualization = null; // Reset current mode
        lastInteraction = System.currentTimeMillis(); // Reset timer
        logger.info("Stopped visualization");
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
                button[col].setBackground(parent.getBackground());

            }
        }
    }

    public void updateDisplay() {
        if (!isVisualizationMode || currentVisualization == null) {
            return;
        }

        try {
            currentVisualization.update(buttons);
        } catch (Exception e) {
            logger.error("{} Error updating display", currentVisualization != null ? currentVisualization.getName() : "<unknown>", e);
        }
    }

    // Public API methods for external control
    public void lockCurrentVisualization() {
        isLocked = true;
        logger.info("Locked current visualization");
    }

    public void unlockCurrentVisualization() {
        isLocked = false;
        // Resume random switching by restarting the change timer
        visualizationChangeTimer.start();
        logger.info("Unlocked current visualization");
    }

    public void switchToRandomVisualization() {
        if (!isLocked) {
            startVisualizer(getRandomVisualization());
        }
    }

    public void switchToVisualization(String visualizationName) {
        for (IVisualizationHandler handler : visualizations) {
            if (handler.getName().equals(visualizationName)) {
                startVisualizer(handler);
                return;
            }
        }
        logger.warn("Visualization not found: {}", visualizationName);
    }

    public List<String> getAvailableVisualizations() {
        List<String> names = new ArrayList<>();
        for (IVisualizationHandler handler : visualizations) {
            names.add(handler.getName());
        }
        return names;
    }

    public boolean isVisualizationMode() {
        return isVisualizationMode;
    }
}