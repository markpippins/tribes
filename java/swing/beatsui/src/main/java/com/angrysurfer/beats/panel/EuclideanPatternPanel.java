package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.widget.NumberedTickDial;
import com.angrysurfer.core.sequencer.DrumSequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * A panel for editing and visualizing Euclidean rhythm patterns
 */
public class EuclideanPatternPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(EuclideanPatternPanel.class);
    // UI components
    private final CircleDisplay circleDisplay;
    // Pattern parameters
    private int steps = 16;
    private int hits = 4;
    private int rotation = 0;
    private int width = 0;  // Width parameter
    private NumberedTickDial stepsDial;
    private NumberedTickDial hitsDial;
    private NumberedTickDial rotationDial;
    private NumberedTickDial widthDial;

    // Title for the panel
    private String title = "Euclidean Pattern";

    // For compact mode
    private boolean isCompact = false;

    private DrumSequencer sequencer;

    public EuclideanPatternPanel(int steps) {
        this(steps, false);
    }

    public EuclideanPatternPanel(int steps, boolean compact) {
        this.steps = steps;
        setLayout(new BorderLayout(5, 5));
        isCompact = compact;

        // Set background color
        setBackground(new Color(40, 40, 40));
        setOpaque(true);

        int circleDiameter = compact ? 120 : 400;
        int dialDiameter = compact ? 60 : 80;

        // Create the circular display component
        circleDisplay = new CircleDisplay();
        circleDisplay.setPreferredSize(new Dimension(circleDiameter, circleDiameter));
        circleDisplay.setMinimumSize(new Dimension(100, 100));

        // Create control panel with all dials - now positioned vertically
        JPanel controlPanel = createControlPanel(dialDiameter);

        // Add title if in compact mode
        if (compact) {
            JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
            titleLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
            titleLabel.setForeground(Color.WHITE);
            add(titleLabel, BorderLayout.NORTH);
        }

        // Add components to the panel - CRITICAL! Now controls on EAST instead of SOUTH
        add(circleDisplay, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);  // Changed from SOUTH to EAST

        // Set border for visibility
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        // Set size for the entire panel - adjust for new layout
        setPreferredSize(new Dimension(compact ? 240 : 600, compact ? 180 : 500));

        // Generate and display the initial pattern
        updatePattern();
    }

    private JPanel createControlPanel(int dialDiameter) {
        JPanel panel = new JPanel();

        // Use vertical BoxLayout instead of FlowLayout
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Controls")
        ));
        panel.setBackground(new Color(40, 40, 40));

        // Create steps dial
        JPanel stepsPanel = new JPanel(new BorderLayout(2, 2));
        stepsPanel.add(new JLabel("Steps", SwingConstants.CENTER), BorderLayout.NORTH);
        stepsPanel.setOpaque(false);
        stepsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        stepsDial = new NumberedTickDial(1, steps);
        stepsDial.setValue(steps);
        stepsDial.setSize(dialDiameter, dialDiameter);
        stepsDial.setPreferredSize(new Dimension(dialDiameter, dialDiameter));
        stepsDial.addChangeListener(e -> {
            steps = stepsDial.getValue();

            // Ensure filled steps doesn't exceed total steps
            if (hits > steps) {
                hits = steps;
                hitsDial.setValue(hits);
            }

            // Update fills dial maximum
            hitsDial.setMaximum(steps);

            // Update rotation dial maximum
            rotationDial.setMaximum(steps > 0 ? steps - 1 : 0);

            updatePattern();
            logger.debug("Total steps changed to: {}", steps);
        });
        stepsPanel.add(stepsDial, BorderLayout.CENTER);

        // Create fills dial
        JPanel fillsPanel = new JPanel(new BorderLayout(2, 2));
        fillsPanel.add(new JLabel("Hits", SwingConstants.CENTER), BorderLayout.NORTH);
        fillsPanel.setOpaque(false);
        fillsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        hitsDial = new NumberedTickDial(0, steps);
        hitsDial.setValue(hits);
        hitsDial.setSize(dialDiameter, dialDiameter);
        hitsDial.setPreferredSize(new Dimension(dialDiameter, dialDiameter));
        hitsDial.addChangeListener(e -> {
            hits = hitsDial.getValue();
            updatePattern();
            logger.debug("Filled steps changed to: {}", hits);
        });
        fillsPanel.add(hitsDial, BorderLayout.CENTER);

        // Create rotation dial panel
        JPanel rotationPanel = new JPanel(new BorderLayout(2, 2));
        rotationPanel.add(new JLabel("Rotate", SwingConstants.CENTER), BorderLayout.NORTH);
        rotationPanel.setOpaque(false);
        rotationPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        rotationDial = new NumberedTickDial(0, steps - 1);
        rotationDial.setValue(rotation);
        rotationDial.setSize(dialDiameter, dialDiameter);
        rotationDial.setPreferredSize(new Dimension(dialDiameter, dialDiameter));
        rotationDial.addChangeListener(e -> {
            rotation = rotationDial.getValue();
            updatePattern();
            logger.debug("Rotation changed to: {}", rotation);
        });
        rotationPanel.add(rotationDial, BorderLayout.CENTER);

        // Create width dial panel
        JPanel widthPanel = new JPanel(new BorderLayout(2, 2));
        widthPanel.add(new JLabel("Width", SwingConstants.CENTER), BorderLayout.NORTH);
        widthPanel.setOpaque(false);
        widthPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        widthDial = new NumberedTickDial(0, 10);
        widthDial.setValue(width);
        widthDial.setSize(dialDiameter, dialDiameter);
        widthDial.setPreferredSize(new Dimension(dialDiameter, dialDiameter));
        widthDial.addChangeListener(e -> {
            width = widthDial.getValue();
            updatePattern();
            logger.debug("Width changed to: {}", width);
        });
        widthPanel.add(widthDial, BorderLayout.CENTER);

        // Add all dial panels to the control panel with vertical spacing
        panel.add(Box.createVerticalStrut(5));
        panel.add(stepsPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(fillsPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(rotationPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(widthPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(Box.createVerticalGlue()); // Fill extra space at bottom

        return panel;
    }

    private void updatePattern() {
        // Update rotation dial's max value based on total steps
        rotationDial.setMaximum(steps > 0 ? steps - 1 : 0);

        // Ensure rotation is within bounds
        if (rotation >= steps) {
            rotation = 0;
            rotationDial.setValue(0);
        }

        // Generate the Euclidean pattern
        boolean[] pattern = generateEuclideanPattern(steps, hits, rotation, width);

        // Update the visual component
        circleDisplay.setPattern(pattern);
        circleDisplay.repaint();
    }

    public void setTitle(String title) {
        this.title = title;

        // Update title label if in compact mode
        if (isCompact) {
            // Find and remove any existing title component
            Component[] components = getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JLabel) {
                    remove(components[i]);
                    break;
                }
            }

            // Add new title label
            JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
            titleLabel.setFont(new Font("Sans Serif", Font.BOLD, 12));
            titleLabel.setForeground(Color.WHITE);
            add(titleLabel, BorderLayout.NORTH);

            revalidate();
            repaint();
        }
    }

    private boolean[] generateEuclideanPattern(int steps, int fills, int rotation, int width) {
        if (steps <= 0) return new boolean[0];
        if (fills <= 0) return new boolean[steps]; // All false
        if (fills >= steps) {
            // All true
            boolean[] allTrue = new boolean[steps];
            for (int i = 0; i < steps; i++) {
                allTrue[i] = true;
            }
            return allTrue;
        }

        boolean[] pattern = new boolean[steps];

        if (width == 0) {
            // Standard Euclidean pattern - use your existing algorithm
            int increment = steps / fills;
            int error = steps % fills;
            int position = 0;

            for (int i = 0; i < fills; i++) {
                pattern[(position + rotation) % steps] = true;
                position += increment;

                // Distribute the remainder evenly
                if (error > 0) {
                    position++;
                    error--;
                }
            }
        } else {
            // Width-distorted pattern - stretch some sides
            double widthFactor = width / 10.0; // Normalize to 0.0-1.0
            int halfSteps = steps / 2;

            // Use a simpler approach: non-linear spacing between fills
            for (int i = 0; i < fills; i++) {
                double normalPosition = (double) i / fills;
                double distortion = widthFactor * Math.sin(normalPosition * Math.PI * 2);

                // Calculate position with distortion
                double adjustedPosition = normalPosition + distortion * 0.2;
                if (adjustedPosition < 0) adjustedPosition = 0;
                if (adjustedPosition >= 1) adjustedPosition = 0.999;

                int position = (int) (adjustedPosition * steps);

                // Apply rotation and set the step
                pattern[(position + rotation) % steps] = true;
            }
        }

        return pattern;
    }

    /**
     * Gets the current pattern of fills as a boolean array
     *
     * @return boolean array where true represents filled steps
     */
    public boolean[] getPattern() {
        // Generate the pattern with current parameters
        return generateEuclideanPattern(steps, hits, rotation, width);
    }

    // Getters for the dials
    public NumberedTickDial getStepsDial() {
        return stepsDial;
    }

    public NumberedTickDial getHitsDial() {
        return hitsDial;
    }

    public NumberedTickDial getRotationDial() {
        return rotationDial;
    }

    public NumberedTickDial getWidthDial() {
        return widthDial;
    }

    // Circle display inner class
    private class CircleDisplay extends JPanel {
        private boolean[] pattern;

        public CircleDisplay() {
            setBackground(new Color(30, 30, 30));
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            pattern = new boolean[steps];
        }

        public void setPattern(boolean[] pattern) {
            this.pattern = pattern;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate dimensions
            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height) - (isCompact ? 10 : 40); // Leave margin based on mode

            // Center the circle
            int centerX = width / 2;
            int centerY = height / 2;

            // Draw outer circle
            g2d.setColor(new Color(60, 60, 60));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(centerX - size / 2, centerY - size / 2, size, size);

            // Draw steps - only if we have a valid pattern
            if (pattern != null && pattern.length > 0) {
                int radius = size / 2;
                int dotRadius = Math.max(3, Math.min(radius / 10, isCompact ? 5 : 12));

                // Arrays for polygon points (only filled steps)
                int[] polygonX = new int[steps];
                int[] polygonY = new int[steps];
                int filledCount = 0;

                // Draw each step position at equal angular intervals
                for (int i = 0; i < pattern.length; i++) {
                    // Calculate angle: start at top (270 degrees) and move clockwise
                    double angleInRadians = Math.toRadians(270 + (360.0 * i / pattern.length));

                    // Calculate position on the circle
                    int x = centerX + (int) (radius * Math.cos(angleInRadians));
                    int y = centerY + (int) (radius * Math.sin(angleInRadians));

                    // Only draw step numbers in full-size mode
                    if (!isCompact) {
                        // Draw step number for all positions
                        g2d.setColor(Color.GRAY);
                        g2d.setFont(new Font("Sans", Font.PLAIN, 10));
                        String stepText = String.valueOf(i + 1);
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(stepText);
                        int textHeight = fm.getHeight();

                        // Position text slightly outside the circle
                        double textAngle = angleInRadians;
                        int textRadius = radius + (isCompact ? 10 : 20);
                        int textX = centerX + (int) (textRadius * Math.cos(textAngle));
                        int textY = centerY + (int) (textRadius * Math.sin(textAngle));

                        g2d.drawString(stepText, textX - textWidth / 2, textY + textHeight / 4);
                    }

                    // Draw step dot - styled according to whether it's active or not
                    if (pattern[i]) {
                        // Store coordinates for the polygon (filled steps only)
                        if (filledCount < polygonX.length) {
                            polygonX[filledCount] = x;
                            polygonY[filledCount] = y;
                            filledCount++;
                        }

                        // Filled step
                        g2d.setColor(new Color(120, 200, 255));
                        g2d.fillOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
                        g2d.setColor(Color.WHITE);
                        g2d.drawOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
                    } else {
                        // Empty step
                        g2d.setColor(new Color(80, 80, 80));
                        g2d.drawOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
                        g2d.setColor(new Color(50, 50, 50));
                        g2d.fillOval(x - dotRadius, y - dotRadius, dotRadius * 2, dotRadius * 2);
                    }
                }

                // Draw connecting lines between all steps to show the circular structure
                g2d.setColor(new Color(80, 80, 80));
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        0, new float[]{1.0f, 2.0f}, 0));

                for (int i = 0; i < pattern.length; i++) {
                    // Calculate positions for current and next point
                    double angle1 = Math.toRadians(270 + (360.0 * i / pattern.length));
                    double angle2 = Math.toRadians(270 + (360.0 * ((i + 1) % pattern.length) / pattern.length));

                    int x1 = centerX + (int) (radius * Math.cos(angle1));
                    int y1 = centerY + (int) (radius * Math.sin(angle1));
                    int x2 = centerX + (int) (radius * Math.cos(angle2));
                    int y2 = centerY + (int) (radius * Math.sin(angle2));

                    g2d.drawLine(x1, y1, x2, y2);
                }

                // Draw the polygon connecting filled steps
                if (filledCount >= 2) {
                    g2d.setColor(new Color(120, 200, 255, 60));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawPolygon(polygonX, polygonY, filledCount);
                    g2d.setColor(new Color(120, 200, 255, 30));
                    g2d.fillPolygon(polygonX, polygonY, filledCount);
                }
            }

            g2d.dispose();
        }
    }
}
