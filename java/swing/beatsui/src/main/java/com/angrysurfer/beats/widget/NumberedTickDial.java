package com.angrysurfer.beats.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * A dial control with numbered tick marks that shows integer values
 */
public class NumberedTickDial extends Dial {

    private static final Logger logger = LoggerFactory.getLogger(NumberedTickDial.class.getName());

    private static final double START_ANGLE = -90; // Start at top (-90 degrees)
    private static final double TOTAL_ARC = 300;   // Not a full circle to prevent wrap-around

    private final int detentCount;               // Number of detent positions
    private final double degreesPerDetent;       // Angular distance between detents
    private int currentDetent = 0;         // Current detent position
    private boolean isDragging = false;    // Whether user is dragging the dial
    private double startAngle = 0;         // Starting angle for drag operations
    private String[] tickLabels;           // Labels for tick marks

    /**
     * Creates a new NumberedTickDial with the specified value range
     *
     * @param min The minimum value
     * @param max The maximum value
     */
    public NumberedTickDial(int min, int max) {
        super();

        // Set value range
        setMinimum(min);
        setMaximum(max);

        // Calculate number of detent positions
        detentCount = max - min + 1;
        degreesPerDetent = TOTAL_ARC / (detentCount - 1);

        // Generate tick labels
        generateTickLabels(min, max);

        // Default to middle value
        int defaultValue = (min + max) / 2;
        setValue(defaultValue, false);

        // Set size
        setPreferredSize(new Dimension(100, 100));
        setMinimumSize(new Dimension(80, 80));

        // Setup mouse listeners for interaction
        setupMouseListeners();
    }

    /**
     * Creates a dial with values from 0 to the specified maximum
     */
    public NumberedTickDial(int max) {
        this(0, max);
    }

    /**
     * Generates numeric labels for tick marks
     */
    private void generateTickLabels(int min, int max) {
        tickLabels = new String[detentCount];
        for (int i = 0; i < detentCount; i++) {
            tickLabels[i] = String.valueOf(min + i);
        }
    }

    /**
     * Sets up mouse listeners for dial interaction
     */
    private void setupMouseListeners() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!isEnabled()) return;
                isDragging = true;
                Point center = new Point(getWidth() / 2, getHeight() / 2);
                startAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                isDragging = false;
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (!isEnabled() || !isDragging) return;

                Point center = new Point(getWidth() / 2, getHeight() / 2);
                double currentAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);

                double angleDelta = currentAngle - startAngle;
                if (angleDelta > Math.PI) angleDelta -= 2 * Math.PI;
                if (angleDelta < -Math.PI) angleDelta += 2 * Math.PI;

                double angleDegrees = Math.toDegrees(angleDelta);
                int detentDelta = (int) Math.round(angleDegrees / degreesPerDetent);

                if (detentDelta != 0) {
                    // Calculate new detent position, clamping to valid range
                    int newDetent = Math.max(0, Math.min(detentCount - 1, currentDetent + detentDelta));

                    // Only update if the position changes
                    if (newDetent != currentDetent) {
                        currentDetent = newDetent;

                        // Convert from detent position to actual value
                        int value = getMinimum() + currentDetent;

                        // Update the visual representation
                        startAngle = currentAngle;
                        repaint();

                        // Fire change events
                        setValue(value, true);

                        logger.debug("Dial changed to value: {}", value);
                    }
                }
            }
        });
    }

    @Override
    public void setValue(int value, boolean notify) {
        // Ensure value is within range
        value = Math.max(getMinimum(), Math.min(getMaximum(), value));

        // Convert from value to detent position
        currentDetent = value - getMinimum();

        // Set the base class value
        super.setValue(value, notify);

        logger.debug("Set dial value to: {}", value);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 10;
        int margin = size / 10;

        // Center coordinates
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radius = (size - 2 * margin) / 2.0;

        // Draw dial background
        g2d.setColor(getParent().getBackground());
        g2d.fillOval(x + margin, y + margin, size - 2 * margin, size - 2 * margin);

        // Draw outer ring
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x + margin, y + margin, size - 2 * margin, size - 2 * margin);

        // Draw arc to show limited rotation range
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawArc(x + margin / 2, y + margin / 2, size - margin, size - margin,
                (int) (START_ANGLE - TOTAL_ARC / 2), (int) TOTAL_ARC);

        // Calculate starting position for detent markers
        double startingAngle = START_ANGLE - (TOTAL_ARC / 2);

        // Draw detent markers and labels
        Font font = new Font("SansSerif", Font.PLAIN, size / 12);
        g2d.setFont(font);

        // Determine which ticks to label - avoid crowding by showing subset
        int tickSpacing = Math.max(1, detentCount / 10);
        if (detentCount <= 10) {
            tickSpacing = 1; // Show all ticks for small ranges
        } else if (detentCount <= 20) {
            tickSpacing = 2; // Show every other tick for medium ranges
        }

        for (int i = 0; i < detentCount; i++) {
            double angle = Math.toRadians(startingAngle + (i * degreesPerDetent));

            // Calculate marker points
            Point2D p1 = new Point2D.Double(centerX + Math.cos(angle) * (radius - margin),
                    centerY + Math.sin(angle) * (radius - margin));
            Point2D p2 = new Point2D.Double(centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius);

            // Highlight current position
            if (i == currentDetent) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2f));
            } else {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.setStroke(new BasicStroke(1f));
            }

            // Draw marker line
            g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());

            // Draw labels for key positions
            boolean shouldShowLabel = (i % tickSpacing == 0) || (i == 0) || (i == detentCount - 1);

            if (shouldShowLabel) {
                Point2D labelPos = new Point2D.Double(
                        centerX + Math.cos(angle) * (radius + margin * 1.5),
                        centerY + Math.sin(angle) * (radius + margin * 1.5));

                FontMetrics fm = g2d.getFontMetrics();
                String label = tickLabels[i];
                int labelW = fm.stringWidth(label);
                int labelH = fm.getHeight();

                g2d.drawString(label, (int) (labelPos.getX() - labelW / 2),
                        (int) (labelPos.getY() + labelH / 4));
            }
        }

        // Draw pointer
        double pointerAngle = Math.toRadians(startingAngle + (currentDetent * degreesPerDetent));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(isEnabled() ? Color.RED : Color.GRAY);
        g2d.drawLine((int) centerX, (int) centerY,
                (int) (centerX + Math.cos(pointerAngle) * (radius - margin / 2)),
                (int) (centerY + Math.sin(pointerAngle) * (radius - margin / 2)));

        // Draw center value
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, size / 5));
        String valueText = tickLabels[currentDetent];
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(valueText, (int) (centerX - fm.stringWidth(valueText) / 2),
                (int) (centerY + fm.getHeight() / 4));

        g2d.dispose();
    }

    /**
     * Gets the current value as a string
     */
    public String getValueText() {
        return tickLabels[currentDetent];
    }

    /**
     * Sets custom labels for the tick marks
     */
    public void setCustomTickLabels(String[] customLabels) {
        if (customLabels != null && customLabels.length == detentCount) {
            this.tickLabels = customLabels;
            repaint();
        } else {
            logger.warn("Custom labels array size doesn't match detent count");
        }
    }
}
