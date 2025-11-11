package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.widget.Dial;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel containing a Dial and a small toggle button in the bottom right corner.
 */
@Getter
public class ToggleDialPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ToggleDialPanel.class);

    private final Dial dial;
    private final JToggleButton toggleButton;
    private final List<ChangeListener> dialChangeListeners = new ArrayList<>();
    private final List<ActionListener> toggleActionListeners = new ArrayList<>();

    /**
     * Default constructor
     */
    public ToggleDialPanel() {
        this(null);
    }

    /**
     * Constructor with command
     *
     * @param command The command to send when dial value changes
     */
    public ToggleDialPanel(String command) {
        setLayout(null); // Use absolute positioning for precise control

        // Create the dial
        dial = new Dial(command);
        dial.setPreferredSize(new Dimension(60, 60));

        // Create toggle button with a small size
        toggleButton = new JToggleButton();
        toggleButton.setPreferredSize(new Dimension(16, 16));
        toggleButton.setFocusPainted(false);
        toggleButton.setBorderPainted(true);
        toggleButton.setContentAreaFilled(true);

        // Add components to the panel
        add(dial);
        add(toggleButton);

        // Set up event forwarding
        setupEventForwarding();
    }

    @Override
    public void doLayout() {
        // Layout components
        Dimension size = getSize();

        // Position the dial to fill most of the panel
        int dialSize = Math.min(size.width, size.height) - 4;
        dial.setBounds(2, 2, dialSize, dialSize);

        // Position toggle button in bottom right
        int buttonSize = Math.max(14, dialSize / 4);
        int buttonX = size.width - buttonSize - 2;
        int buttonY = size.height - buttonSize - 2;
        toggleButton.setBounds(buttonX, buttonY, buttonSize, buttonSize);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }

        // Calculate preferred size based on dial and some padding
        Dimension dialSize = dial.getPreferredSize();
        return new Dimension(dialSize.width + 10, dialSize.height + 10);
    }

    private void setupEventForwarding() {
        // Forward dial events
        dial.addChangeListener(e -> {
            // Forward to our listeners
            ChangeEvent newEvent = new ChangeEvent(this);
            for (ChangeListener listener : dialChangeListeners) {
                listener.stateChanged(newEvent);
            }
        });

        // Forward toggle button events
        toggleButton.addActionListener(e -> {
            // Forward to our listeners
            ActionEvent newEvent = new ActionEvent(
                    this, ActionEvent.ACTION_PERFORMED, "toggle",
                    e.getWhen(), e.getModifiers());

            for (ActionListener listener : toggleActionListeners) {
                listener.actionPerformed(newEvent);
            }
        });
    }

    // --- Convenience methods for the dial ---

    /**
     * Get dial value
     */
    public int getValue() {
        return dial.getValue();
    }

    /**
     * Set dial value
     */
    public void setValue(int value) {
        dial.setValue(value, true);
    }

    /**
     * Set dial value with notification control
     */
    public void setValue(int value, boolean notify) {
        dial.setValue(value, notify);
    }

    /**
     * Set dial minimum value
     */
    public void setMinimum(int min) {
        dial.setMinimum(min);
    }

    /**
     * Set dial maximum value
     */
    public void setMaximum(int max) {
        dial.setMaximum(max);
    }

    /**
     * Set dial label
     */
    public void setLabel(String label) {
        dial.setLabel(label);
    }

    /**
     * Set dial command
     */
    public void setCommand(String command) {
        dial.setCommand(command);
    }

    /**
     * Set dial knob color
     */
    public void setKnobColor(Color color) {
        dial.setKnobColor(color);
    }

    // --- Convenience methods for the toggle button ---

    /**
     * Get toggle state
     */
    public boolean isSelected() {
        return toggleButton.isSelected();
    }

    /**
     * Set toggle state
     */
    public void setSelected(boolean selected) {
        toggleButton.setSelected(selected);
    }

    /**
     * Set toggle button tooltip
     */
    public void setToggleTooltip(String tooltip) {
        toggleButton.setToolTipText(tooltip);
    }

    /**
     * Set toggle button text
     */
    public void setToggleText(String text) {
        toggleButton.setText(text);
    }

    /**
     * Set toggle button color
     */
    public void setToggleColor(Color color) {
        toggleButton.setBackground(color);
    }

    // --- Event listener management ---

    /**
     * Add dial change listener
     */
    public void addDialChangeListener(ChangeListener listener) {
        dialChangeListeners.add(listener);
    }

    /**
     * Remove dial change listener
     */
    public void removeDialChangeListener(ChangeListener listener) {
        dialChangeListeners.remove(listener);
    }

    /**
     * Add toggle button action listener
     */
    public void addToggleActionListener(ActionListener listener) {
        toggleActionListeners.add(listener);
    }

    /**
     * Remove toggle button action listener
     */
    public void removeToggleActionListener(ActionListener listener) {
        toggleActionListeners.remove(listener);
    }
}

// Create a toggle dial panel
// ToggleDialPanel toggleDial = new ToggleDialPanel("REVERB_SEND");

// // Set up the dial properties
// toggleDial.setMinimum(0);
// toggleDial.setMaximum(127);
// toggleDial.setValue(64);
// toggleDial.setLabel("Rev");
// toggleDial.setKnobColor(UIHelper.getDialColor("reverb"));

// // Set up the toggle button properties
// toggleDial.setToggleTooltip("Toggle Reverb On/Off");
// toggleDial.setSelected(true);
// toggleDial.setToggleColor(new Color(60, 180, 60)); // Green for "on"

// // Add listeners for both components
// toggleDial.addDialChangeListener(e -> {
//     ToggleDialPanel source = (ToggleDialPanel) e.getSource();
//     logger.debug("Dial value changed: {}", source.getValue());
// });

// toggleDial.addToggleActionListener(e -> {
//     ToggleDialPanel source = (ToggleDialPanel) e.getSource();
//     logger.debug("Toggle changed: {}", source.isSelected());

//     // Optionally change toggle button color based on state
//     if (source.isSelected()) {
//         source.setToggleColor(new Color(60, 180, 60)); // Green for "on"
//     } else {
//         source.setToggleColor(new Color(180, 60, 60)); // Red for "off"
//     }
// });

// // Add to your panel
// yourPanel.add(toggleDial);
