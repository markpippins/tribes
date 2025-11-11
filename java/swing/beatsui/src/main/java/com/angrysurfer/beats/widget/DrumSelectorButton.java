package com.angrysurfer.beats.widget;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.panel.MainPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;

import lombok.Getter;
import lombok.Setter;

/**
 * A specialized DrumButton for the drum sequencer that handles selection state
 */
@Getter
@Setter
public class DrumSelectorButton extends JButton implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSelectorButton.class);


    private int drumPadIndex;
    private DrumSequencer sequencer;
    private boolean isSelected = false;
    private boolean isPressed = false;
    private Color selectedColor = new Color(255, 128, 0); // Bright orange
    private Color normalColor = new Color(80, 80, 80);    // Dark gray
    private Color pressedColor = new Color(120, 180, 240); // Lighter blue when pressed

    /**
     * Create a new drum sequencer button
     *
     * @param drumPadIndex The index of the drum pad (0-15)
     * @param sequencer    The drum sequencer instance
     */
    public DrumSelectorButton(int drumPadIndex, DrumSequencer sequencer) {
        super();
        this.drumPadIndex = drumPadIndex;
        this.sequencer = sequencer;

        // Set fixed width to match grid cells
        setPreferredSize(new Dimension(120, 25));
        setMinimumSize(new Dimension(120, 25));
        setMaximumSize(new Dimension(120, 25));

        // Visual settings for flat look
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setFont(new Font(getFont().getName(), Font.BOLD, 11));

        // Register for command bus events to track selection changes
        CommandBus.getInstance().register(this, new String[]{Commands.DRUM_PAD_SELECTED});

        // Replace existing action listeners to prevent toggle behavior
        for (ActionListener al : getActionListeners()) {
            removeActionListener(al);
        }

        // Add mouse listener for visual feedback and selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();

                // Only handle selection if mouse is still over the button
                if (contains(e.getPoint())) {
                    logger.info("DrumSequencerButton: selecting pad {}", drumPadIndex);

                    // Call the sequencer's selectDrumPad method
                    if (sequencer != null) {
                        sequencer.selectDrumPad(drumPadIndex);
                    } else {
                        logger.error("Error: sequencer is null!");
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Reset pressed state if mouse exits
                isPressed = false;
                repaint();
            }
        });

        // Add ActionListener for selection
        this.addActionListener(e -> sequencer.selectDrumPad(drumPadIndex));

        // Add mouse listener for double-click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // First select the drum
                    sequencer.selectDrumPad(drumPadIndex);

                    // Then navigate to DrumParams tab
                    MainPanel mainPanel = findMainPanel();
                    if (mainPanel != null) {
                        // The index 1 is for "Machine" tab (DrumParamsPanel)
                        mainPanel.setSelectedTab(1);
                    }
                }
            }
        });

        // Add key listener for Return key
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // First select the drum
                    sequencer.selectDrumPad(drumPadIndex);

                    // Then navigate to DrumParams tab
                    MainPanel mainPanel = findMainPanel();
                    if (mainPanel != null) {
                        // The index 1 is for "Machine" tab (DrumParamsPanel)
                        mainPanel.setSelectedTab(1);
                    }
                }
            }
        });

        // Make sure the button can receive focus for key events
        setFocusable(true);
    }

    /**
     * Override the paint method to create flat buttons with rounded edges
     */
    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Determine the color based on button state
        Color buttonColor;
        if (isPressed) {
            buttonColor = pressedColor;
        } else if (isSelected) {
            buttonColor = selectedColor;
        } else {
            buttonColor = normalColor;
        }

        // Draw flat button with rounded corners
        int width = getWidth();
        int height = getHeight();
        int arc = 8; // Rounded corner radius

        // Fill button background with rounded corners
        g2d.setColor(buttonColor);
        g2d.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);

        // Draw subtle border
        g2d.setColor(buttonColor.darker());
        g2d.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);

        // Draw text with drop shadow for better visibility
        String text = getText();
        if (text != null && !text.isEmpty()) {
            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(text);
            int textHeight = metrics.getHeight();
            int x = (width - textWidth) / 2;
            int y = (height - textHeight) / 2 + metrics.getAscent();

            // Draw text shadow
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.drawString(text, x + 1, y + 1);

            // Draw text
            g2d.setColor(getForeground());
            g2d.drawString(text, x, y);
        }

        g2d.dispose();
    }

    /**
     * Set the drum name and tooltip
     *
     * @param name The name of the drum
     */
    public void setDrumName(String name) {
        setText(name);
        setToolTipText(name + " (Pad " + (drumPadIndex + 1) + ")");
    }

    /**
     * Override isSelected to return the selection state
     */
    @Override
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Override setSelected to update visual state
     */
    @Override
    public void setSelected(boolean selected) {
        this.isSelected = selected;

        // Update visual appearance
        if (selected) {
            setBackground(selectedColor);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.WHITE, 2),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));
        } else {
            setBackground(normalColor);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
        }

        // Force repaint to make selection visible
        repaint();

    // Log the selection for debugging
    logger.info("DrumSequencerButton {} selected: {}", drumPadIndex, selected);
    }

    /**
     * Handle command bus events, particularly drum pad selection
     */
    @Override
    public void onAction(Command action) {
        if (Commands.DRUM_PAD_SELECTED.equals(action.getCommand())) {
            if (action.getData() instanceof DrumPadSelectionEvent event) {
                // Update our appearance if this is the newly selected pad
                boolean isNowSelected = (event.getNewSelection() == drumPadIndex);
                if (isSelected != isNowSelected) {
                    setSelected(isNowSelected);
                }
            }
        }
    }

    /**
     * Helper method to find the MainPanel ancestor
     */
    private MainPanel findMainPanel() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof MainPanel mainPanel) {
                return mainPanel;
            }
            parent = parent.getParent();
        }
        return null;
    }
}
