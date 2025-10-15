package com.angrysurfer.beats.util;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.panel.LivePanel;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.config.TableState;
import com.angrysurfer.core.event.PlayerRefreshEvent;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * Utility class providing static UI helper methods
 */
public class UIHelper {

    // UI constants - Optional size adjustments
    public static final int CONTROL_HEIGHT = 24; // UPDATED from 22 to 24
    public static final int CONTROL_WIDTH = 100;
    public static final int SMALL_CONTROL_WIDTH = 40; // UPDATED from 35 to 40
    public static final int MEDIUM_CONTROL_WIDTH = 60; // UPDATED from 55 to 60
    public static final int LARGE_CONTROL_WIDTH = 120; // UPDATED from 85 to 120
    public static final int ID_LABEL_WIDTH = 85;
    // Add standard dial size constants
    public static final int STANDARD_DIAL_SIZE = 40;
    // Add these constants to the UIHelper class
    public static final Color FIELD_BACKGROUND = new Color(240, 240, 240);
    public static final Color FIELD_FOREGROUND = new Color(20, 20, 20);
    private static final Logger logger = LoggerFactory.getLogger(UIHelper.class.getName());
    // Greys & Dark Blues
    public static Color charcoalGray = new Color(40, 40, 40); // Deep console casing
    public static Color slateGray = new Color(70, 80, 90); // Cool metallic panel
    public static Color deepNavy = new Color(20, 50, 90); // Darker Neve-style blue
    public static Color mutedOlive = new Color(85, 110, 60); // Vintage military-style green
    public static Color fadedLime = new Color(140, 160, 80); // Aged LED green
    // Yellows & Oranges
    public static Color dustyAmber = new Color(200, 140, 60); // Classic VU meter glow
    public static Color warmMustard = new Color(180, 140, 50); // Retro knob indicator
    public static Color deepOrange = new Color(190, 90, 40); // Vintage warning light
    // Accents
    public static Color agedOffWhite = new Color(225, 215, 190); // Worn plastic knobs
    public static Color deepTeal = new Color(30, 80, 90); // Tascam-inspired accent
    public static Color darkGray = new Color(50, 50, 50); // Deep charcoal (console casing)
    public static Color warmGray = new Color(120, 120, 120); // Aged metal panel
    public static Color mutedRed = new Color(180, 60, 60); // Classic button color
    public static Color fadedOrange = new Color(210, 120, 50); // Vintage indicator light
    public static Color coolBlue = new Color(50, 130, 200); // Neve-style trim
    public static Color warmOffWhite = new Color(230, 220, 200);// Aged plastic knobs

    public static Color getDialColor(String name) {

        switch (name) {
            case "swing":
                return charcoalGray;
            case "velocity":
                return slateGray;
            case "probability":
                return deepTeal;
            case "reverb":
                return deepNavy;
            case "random":
                return mutedOlive;
            case "pan":
            case "sparse":
                return fadedLime;
            case "chorus":
                return dustyAmber;
            case "gate":
            case "decay":
                return warmMustard;
            case "nudge":
                return deepOrange;
            case "tilt":
            case "delay":
                return mutedRed;
            case "tune":
            case "drive":
                return fadedOrange;
            case "bright":
            case "tone":
                return darkGray;
            default:
                return coolBlue; // Default to white if not found
        }
    }

    public static Color getButtonColor() {
        return new Color(150, 150, 150); // Aged metal panel
    }

    public static Color getBackgroundColor() {
        return new Color(40, 40, 40); // Deep console casing
    }

    public static Color getTextColor() {
        return new Color(255, 255, 255); // Bright white text for contrast
    }

    public static Color getAccentColor() {
        return new Color(30, 80, 90); // Tascam-inspired accent color
    }

    public static Color[] getColors() {
        return new Color[]{coolBlue, darkGray, warmGray, charcoalGray, slateGray, deepNavy, dustyAmber, warmMustard,
                deepOrange, mutedRed, fadedOrange, mutedOlive, fadedLime};
    }

    public static Color[] getAccentColors() {
        return new Color[]{agedOffWhite, deepTeal, warmOffWhite};
    }

    /**
     * Safely adds a component to a container with error handling
     *
     * @param container   The container to add the component to
     * @param component   The component to add
     * @param constraints Layout constraints (if applicable)
     * @return true if addition was successful, false otherwise
     */
    public static boolean addSafely(Container container, Component component, Object constraints) {
        if (component == null) {
            return false;
        }

        if (container == null) {
            return false;
        }

        try {
            container.add(component, constraints);
            return true;
        } catch (Exception e) {
            logger.error("Error adding component safely", e);
            return false;
        }
    }

    /**
     * Overloaded version without constraints for simpler layouts
     */
    public static boolean addSafely(Container container, Component component) {
        return addSafely(container, component, null);
    }

    public static JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(0, 2, 0, 2)));

        return panel;
    }

    public static void setWidgetPanelBorder(JPanel panel, String title) {
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(1, 2, 1, 2)));
    }

    public static void setPanelBorder(JPanel panel) {
        panel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
    }

    /**
     * Saves the column order of a table
     */
    public static void saveColumnOrder(JTable table, String tableName, Set<String> columns) {
        try {
            TableState state = RedisService.getInstance().loadTableState(tableName);
            if (state != null) {
                List<String> columnOrder = new ArrayList<>();
                // Get visible column order
                for (int i = 0; i < table.getColumnCount(); i++) {
                    int modelIndex = table.convertColumnIndexToModel(i);
                    String columnName = columns.toArray()[modelIndex].toString();
                    columnOrder.add(columnName);
                }

                // Only save if we have all columns
                if (columnOrder.size() == columns.size()) {
                    logger.info("Saving column order: " + String.join(", ", columnOrder));
                    state.setColumnOrder(columnOrder);
                    RedisService.getInstance().saveTableState(state, tableName);
                } else {
                    logger.error("Column order incomplete, not saving");
                }
            }
        } catch (Exception e) {
            logger.error("Error saving column order: " + e.getMessage());
        }
    }

    /**
     * Restores the column order of a table
     */
    public static void restoreColumnOrder(JTable table, String tableName, Set<String> columns) {
        try {
            TableState state = RedisService.getInstance().loadTableState(tableName);
            List<String> savedOrder = state != null ? state.getColumnOrder() : null;

            if (savedOrder != null && !savedOrder.isEmpty() && savedOrder.size() == columns.size()) {
                logger.info("Restoring column order: " + String.join(", ", savedOrder));

                // Create a map of column names to their current positions
                Map<String, Integer> currentOrder = new HashMap<>();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    currentOrder.put(columns.toArray()[i].toString(), i);
                }

                // Move each column to its saved position
                for (int i = 0; i < savedOrder.size(); i++) {
                    String colName = savedOrder.get(i);
                    Integer currentPos = currentOrder.get(colName);
                    if (currentPos != null && currentPos != i) {
                        table.getColumnModel().moveColumn(currentPos, i);
                        // Update the currentOrder map after moving
                        for (Map.Entry<String, Integer> entry : currentOrder.entrySet()) {
                            if (entry.getValue() == i) {
                                currentOrder.put(entry.getKey(), currentPos);
                                break;
                            }
                        }
                        currentOrder.put(colName, i);
                    }
                }
            } else {
                logger.info("No valid column order found to restore");
            }
        } catch (Exception e) {
            logger.error("Error restoring column order: " + e.getMessage());
        }
    }

    /**
     * Creates a standardized text field with customizable properties
     */
    public static JTextField createTextField(String initialValue, int columns, boolean editable, boolean enabled,
                                             boolean centered, Color backgroundColor) {
        JTextField field;

        // Create with initial text or columns
        if (initialValue != null) {
            field = new JTextField(initialValue);
            if (columns > 0) {
                field.setColumns(columns);
            }
        } else {
            field = new JTextField(columns > 0 ? columns : 10); // Default to 10 columns
        }

        // Apply common settings
        field.setEditable(editable);
        field.setEnabled(enabled);

        // Optional text alignment
        if (centered) {
            field.setHorizontalAlignment(JTextField.CENTER);
        }

        // Optional background color
        if (backgroundColor != null) {
            field.setBackground(backgroundColor);
        }

        // Set both alignment properties to CENTER
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setAlignmentY(Component.CENTER_ALIGNMENT);

        return field;
    }

    /**
     * Overloaded method with fewer parameters for simpler cases
     */
    public static JTextField createTextField(String initialValue, int columns) {
        return createTextField(initialValue, columns, false, true, true, null);
    }

    /**
     * Creates a status display field (non-editable, with initial value)
     */
    public static JTextField createStatusField(String initialValue, int columns) {
        Color lightGray = new Color(240, 240, 240);
        JTextField field = createTextField(initialValue, columns, false, false, true, Color.GREEN);
        field.setMaximumSize(new Dimension(columns * 10, 25)); // Rough size approximation
        return field;
    }

    /**
     * Creates a disabled status field with consistent sizing
     *
     * @param initialValue Initial text value
     * @return Configured text field
     */
    public static JTextField createDisplayField(String initialValue) {
        Color lightGray = new Color(240, 240, 240);
        JTextField field = createTextField(initialValue, 4, false, false, true, lightGray);
        field.setMaximumSize(new Dimension(50, 25));
        return field;
    }

    /**
     * Creates a disabled status field with inverse display (custom colors)
     *
     * @param initialValue Initial text value
     * @param foreground   Text color
     * @param background   Background color
     * @return Configured text field with inverse colors
     */
    public static JTextField createInverseDisplayField(String initialValue, Color foreground, Color background) {
        JTextField field = createTextField(initialValue, 4, false, false, true, background);
        field.setForeground(foreground);
        field.setMaximumSize(new Dimension(50, 25));
        return field;
    }

    /**
     * Creates a panel with up/down buttons for adjustments
     */
    public static LivePanel createVerticalAdjustPanel(String label, String upLabel, String downLabel, String upCommand,
                                                      String downCommand) {
        LivePanel navPanel = new LivePanel() {
            @Override
            public void handlePlayerActivated() {

            }

            @Override
            public void handlePlayerUpdated() {

            }
        };
        navPanel.setLayout(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add margins
        JLabel octaveLabel = new JLabel(label, JLabel.CENTER);

        // Create up and down buttons
        JButton prevButton = new JButton(upLabel);
        prevButton.setActionCommand(upCommand);
        // If it's a transpose command, publish without player data
        if (upCommand.equals(Commands.TRANSPOSE_UP) || upCommand.equals(Commands.TRANSPOSE_DOWN)) {
            prevButton.addActionListener(
                    e -> CommandBus.getInstance().publish(e.getActionCommand(), UIHelper.class, null));
        } else {
            // Original code path for other commands
            prevButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), UIHelper.class,
                    navPanel.getPlayer()));
        }

        // Similar change for the down button
        JButton nextButton = new JButton(downLabel);
        nextButton.setActionCommand(downCommand);
        if (downCommand.equals(Commands.TRANSPOSE_UP) || downCommand.equals(Commands.TRANSPOSE_DOWN)) {
            nextButton.addActionListener(
                    e -> CommandBus.getInstance().publish(e.getActionCommand(), UIHelper.class, null));
        } else {
            nextButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), UIHelper.class,
                    navPanel.getPlayer()));
        }

        // Enable/disable buttons based on player selection
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);

        return navPanel;
    }

    /**
     * Helper method to find components by type in a container hierarchy and apply
     * an action
     */
    public static <T extends Component> void findComponentsByType(Container container,
                                                                  Class<T> componentClass, Consumer<Component> action) {

        // Check all components in the container
        for (Component component : container.getComponents()) {
            // If component matches the requested class, apply the action
            if (componentClass.isAssignableFrom(component.getClass())) {
                action.accept(component);
            }

            // If component is itself a container, recursively search it
            if (component instanceof Container) {
                findComponentsByType((Container) component, componentClass, action);
            }
        }
    }

    /**
     * Check if a component is a child (direct or indirect) of a container
     */
    public static boolean isChildOf(Component child, Container parent) {
        // Check if the component's parent is the target container
        Container current = child.getParent();
        while (current != null) {
            if (current == parent) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Get the first parent of a specific type
     */
    public static <T extends Container> T getParentOfType(Component component, Class<T> parentClass) {
        Container current = component.getParent();
        while (current != null) {
            if (parentClass.isAssignableFrom(current.getClass())) {
                return parentClass.cast(current);
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Find all components of a specific type in a container
     */
    public static <T extends Component> List<T> findAllComponentsOfType(Container container, Class<T> componentClass) {
        List<T> result = new ArrayList<>();

        findComponentsByType(container, componentClass, component -> {
            result.add(componentClass.cast(component));
        });

        return result;
    }

    /**
     * Create a standard-sized dial with consistent styling
     */
    public static Dial createStandardDial(String tooltip, int initialValue) {
        Dial dial = new Dial();
        dial.setUpdateOnResize(false);
        dial.setToolTipText(tooltip);
        dial.setValue(initialValue);
        dial.setMaximumSize(new Dimension(STANDARD_DIAL_SIZE, STANDARD_DIAL_SIZE));
        dial.setPreferredSize(new Dimension(STANDARD_DIAL_SIZE, STANDARD_DIAL_SIZE));
        return dial;
    }

    /**
     * Create a standard-sized dial with a label
     */
    public static Dial createLabeledDial(String label, String tooltip, int initialValue) {
        Dial dial = createStandardDial(tooltip, initialValue);
        dial.setUpdateOnResize(false);
        dial.setLabel(label);
        return dial;
    }

    /**
     * Create a player refresh button
     *
     * @param player     The player to refresh
     * @param buttonText Optional button text (if null, uses refresh symbol)
     * @return A configured JButton
     */
    public static JButton createPlayerRefreshButton(Player player, String buttonText) {
        JButton refreshButton;

        if (buttonText != null) {
            refreshButton = new JButton(buttonText);
        } else {
            refreshButton = new JButton(Symbols.get(Symbols.REFRESH));
        }

        refreshButton.setToolTipText("Refresh instrument sound");
        refreshButton.setPreferredSize(new Dimension(SMALL_CONTROL_WIDTH, CONTROL_HEIGHT));

        refreshButton.addActionListener(e -> {
            if (player != null && player.getInstrument() != null) {
                // Create a refresh event for this specific player
                PlayerRefreshEvent event = new PlayerRefreshEvent(refreshButton, player);

                // Send the event
                CommandBus.getInstance().publish(
                        Commands.PLAYER_REFRESH_EVENT,
                        refreshButton,
                        event
                );

                // Show status update
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        refreshButton,
                        new StatusUpdate(
                                "Sound Refresh", "Info",
                                "Refreshed instrument for " + player.getName())
                );
            }
        });

        return refreshButton;
    }

    /**
     * Create a player-aware labeled control panel with title
     *
     * @param title            The panel title
     * @param player           The player this panel works with
     * @param hasRefreshButton Whether to include a refresh button
     * @return A configured JPanel with BorderLayout
     */
    public static JPanel createPlayerAwareControlPanel(String title, Player player, boolean hasRefreshButton) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        if (hasRefreshButton && player != null) {
            JPanel headerPanel = new JPanel(new BorderLayout());

            // Add refresh button to the right side
            JButton refreshButton = createPlayerRefreshButton(player, null);
            headerPanel.add(refreshButton, BorderLayout.EAST);

            panel.add(headerPanel, BorderLayout.NORTH);
        }

        return panel;
    }

    /**
     * Create a dial panel for a player parameter
     *
     * @param label           The parameter label
     * @param player          The player this dial affects
     * @param min             Minimum value
     * @param max             Maximum value
     * @param initialValue    Initial value
     * @param propertyUpdater Function that updates the player property
     * @return A configured dial panel
     */
    public static JPanel createPlayerParameterDialPanel(
            String label,
            Player player,
            int min,
            int max,
            int initialValue,
            Consumer<Integer> propertyUpdater
    ) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JLabel nameLabel = new JLabel(label, JLabel.CENTER);
        panel.add(nameLabel, BorderLayout.NORTH);

        Dial dial = new Dial(min, max, initialValue);
        dial.setPreferredSize(new Dimension(50, 50));

        dial.addChangeListener(e -> {
            int value = dial.getValue();
            if (player != null && propertyUpdater != null) {
                // Update the property
                propertyUpdater.accept(value);

                // Send a player update event
                CommandBus.getInstance().publish(
                        Commands.PLAYER_UPDATE_EVENT,
                        dial,
                        new PlayerUpdateEvent(dial, player)
                );
            }
        });

        panel.add(dial, BorderLayout.CENTER);

        JLabel valueLabel = new JLabel(String.valueOf(initialValue), JLabel.CENTER);
        dial.addChangeListener(e -> valueLabel.setText(String.valueOf(dial.getValue())));
        panel.add(valueLabel, BorderLayout.SOUTH);

        return panel;
    }
}
