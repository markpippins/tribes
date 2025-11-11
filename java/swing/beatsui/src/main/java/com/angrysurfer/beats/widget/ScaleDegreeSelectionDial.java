package com.angrysurfer.beats.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Point2D;

public class ScaleDegreeSelectionDial extends Dial {

    private static final Logger logger = LoggerFactory.getLogger(ScaleDegreeSelectionDial.class.getName());

    // Scale degree labels with Roman numerals
    private static final String[] SCALE_DEGREE_LABELS = {
            "bVII", "bVI", "bV", "bIV", "bIII", "bII", "bI",
            "I",
            "II", "III", "IV", "V", "VI", "VII", "VIII"
    };

    private static final int DETENT_COUNT = 15;  // -7 to +7
    private static final double START_ANGLE = -90; // Start at top (-90 degrees)
    private static final double TOTAL_ARC = 300;  // Not a full circle to prevent wrap-around
    private static final double DEGREES_PER_DETENT = TOTAL_ARC / (DETENT_COUNT - 1);

    private int currentDetent = 7;  // Default to "I" (index 7 in our array)
    private boolean isDragging = false;
    private double startAngle = 0;

    public ScaleDegreeSelectionDial() {
        super();
        setMinimum(-7);
        setMaximum(7);
        setValue(1, false);  // Default to "I" (1 in our scale degree system)

        setPreferredSize(new Dimension(100, 100));
        setMinimumSize(new Dimension(80, 80));

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
                int detentDelta = (int) Math.round(angleDegrees / DEGREES_PER_DETENT);

                if (detentDelta != 0) {
                    // Calculate new detent position, clamping to valid range
                    int newDetent = Math.max(0, Math.min(DETENT_COUNT - 1, currentDetent + detentDelta));

                    // Only update if the position changes
                    if (newDetent != currentDetent) {
                        currentDetent = newDetent;

                        // Convert from detent position to scale degree value (-7 to 7)
                        int scaleDegreeValue = currentDetent - 7;  // Map 0-14 → -7 to 7

                        // Update the visual representation
                        startAngle = currentAngle;
                        repaint();

                        // Fire change events
                        setValue(scaleDegreeValue, true);

                        logger.debug("Scale degree changed: {} (value: {})",
                                SCALE_DEGREE_LABELS[currentDetent], scaleDegreeValue);
                    }
                }
            }
        });
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

        // Only draw some key markers to avoid crowding
        int[] markersToShow = {0, 3, 7, 11, 14};  // bVII, bIV, I, IV, VIII

        for (int i = 0; i < DETENT_COUNT; i++) {
            double angle = Math.toRadians(startingAngle + (i * DEGREES_PER_DETENT));

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
            boolean shouldShowLabel = false;
            for (int marker : markersToShow) {
                if (i == marker) {
                    shouldShowLabel = true;
                    break;
                }
            }

            if (shouldShowLabel) {
                Point2D labelPos = new Point2D.Double(
                        centerX + Math.cos(angle) * (radius + margin * 1.5),
                        centerY + Math.sin(angle) * (radius + margin * 1.5));

                FontMetrics fm = g2d.getFontMetrics();
                String label = SCALE_DEGREE_LABELS[i];
                int labelW = fm.stringWidth(label);
                int labelH = fm.getHeight();

                g2d.drawString(label, (int) (labelPos.getX() - labelW / 2),
                        (int) (labelPos.getY() + labelH / 4));
            }
        }

        // Draw pointer
        double pointerAngle = Math.toRadians(startingAngle + (currentDetent * DEGREES_PER_DETENT));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(isEnabled() ? Color.RED : Color.GRAY);
        g2d.drawLine((int) centerX, (int) centerY,
                (int) (centerX + Math.cos(pointerAngle) * (radius - margin / 2)),
                (int) (centerY + Math.sin(pointerAngle) * (radius - margin / 2)));

        // Draw center value
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, size / 5));
        String valueText = SCALE_DEGREE_LABELS[currentDetent];
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(valueText, (int) (centerX - fm.stringWidth(valueText) / 2),
                (int) (centerY + fm.getHeight() / 4));

        g2d.dispose();
    }

    @Override
    public void setValue(int value, boolean notify) {
        // Ensure value is within range
        value = Math.max(getMinimum(), Math.min(getMaximum(), value));

        // Convert from scale degree value (-7 to 7) to detent position (0 to 14)
        currentDetent = value + 7;

        // Set the base class value
        super.setValue(value, notify);

        logger.debug("Set scale degree: {} (value: {})",
                SCALE_DEGREE_LABELS[currentDetent], value);

        repaint();
    }

    /**
     * Gets the current scale degree as a Roman numeral string
     */
    public String getScaleDegreeLabel() {
        return SCALE_DEGREE_LABELS[currentDetent];
    }
}
