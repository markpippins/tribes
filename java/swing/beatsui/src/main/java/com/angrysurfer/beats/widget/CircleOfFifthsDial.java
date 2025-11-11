package com.angrysurfer.beats.widget;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;

/**
 * A specialized note selection dial that displays notes in the Circle of Fifths layout.
 * Shows relationships between keys and provides visual color coding for related keys.
 */
@Getter
@Setter
public class CircleOfFifthsDial extends NoteSelectionDial {
    private static final Logger logger = LoggerFactory.getLogger(CircleOfFifthsDial.class);

    // Circle of fifths note order - starting with C at top, moving clockwise
    private static final String[] CIRCLE_ORDER = {"C", "G", "D", "A", "E", "B", "F#", "C#", "G#", "D#", "A#", "F"};

    // Colors for circle segments - from warm to cool colors for a rainbow effect
    private static final Color[] KEY_COLORS = {
            new Color(220, 50, 50),    // C - Red
            new Color(240, 120, 40),   // G - Orange
            new Color(250, 200, 40),   // D - Yellow
            new Color(180, 220, 40),   // A - Yellow-Green
            new Color(40, 180, 80),    // E - Green
            new Color(30, 160, 180),   // B - Teal
            new Color(50, 120, 220),   // F# - Blue
            new Color(100, 80, 220),   // C# - Indigo
            new Color(150, 60, 200),   // G# - Purple
            new Color(200, 50, 180),   // D# - Magenta
            new Color(220, 40, 130),   // A# - Pink
            new Color(220, 40, 80)     // F - Red-Pink
    };

    // Relative minor key offset (3 steps clockwise in the circle)
    private static final int RELATIVE_MINOR_OFFSET = 3;

    // Flag to toggle between showing major or minor keys
    private boolean showMinorKeys = false;

    public CircleOfFifthsDial() {
        super();
        setToolTipText("Circle of Fifths - Click to select note, double-click to toggle major/minor");

        setPreferredSize(new Dimension(110, 110));
        setMinimumSize(new Dimension(110, 110));

        // Add double-click handler to toggle between major/minor view
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showMinorKeys = !showMinorKeys;
                    repaint();
                    logger.debug("Toggled to show {} keys", showMinorKeys ? "minor" : "major");
                }
            }
        });
    }

    /**
     * Overrides the parent's detection of current detent to use circle of fifths ordering
     */
    @Override
    public void setValue(int midiNoteValue, boolean notify) {
        // Call parent implementation first
        super.setValue(midiNoteValue, notify);

        // Then recalculate the detent position based on circle of fifths
        String noteName = NOTE_NAMES[midiNoteValue % 12];
        for (int i = 0; i < CIRCLE_ORDER.length; i++) {
            if (CIRCLE_ORDER[i].equals(noteName)) {
                // This is our position in the circle of fifths
                break;
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 20;
        int margin = size / 10;

        // Center coordinates
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radius = (size - 2 * margin) / 2.0;

        // Draw background
        g2d.setColor(getParent().getBackground().darker());
        g2d.fillOval(x + margin / 2, y + margin / 2, size - margin, size - margin);

        // Draw key segments
        double arcAngle = 360.0 / 12;
        double startAngle = -90 - arcAngle / 2; // Start at top, adjust for segment centering

        for (int i = 0; i < CIRCLE_ORDER.length; i++) {
            double segmentAngle = startAngle + (i * arcAngle);

            // Get key name - adjust for minor if needed
            String keyName = CIRCLE_ORDER[i];
            if (showMinorKeys) {
                // Calculate the relative minor key
                int minorIndex = (i + RELATIVE_MINOR_OFFSET) % 12;
                keyName = CIRCLE_ORDER[minorIndex].toLowerCase();
            }

            // Determine if this is the current selection
            boolean isSelected = false;
            String currentNote = NOTE_NAMES[getValue() % 12];
            if (currentNote.equals(CIRCLE_ORDER[i])) {
                isSelected = true;
            }

            // Set color and opacity
            Color segmentColor = KEY_COLORS[i];
            if (!isSelected) {
                // Make non-selected keys more transparent
                segmentColor = new Color(
                        segmentColor.getRed(),
                        segmentColor.getGreen(),
                        segmentColor.getBlue(),
                        180);
            }

            // Draw the segment
            g2d.setColor(segmentColor);
            Shape arc = new Arc2D.Double(
                    x + margin,
                    y + margin,
                    size - 2 * margin,
                    size - 2 * margin,
                    segmentAngle,
                    arcAngle,
                    Arc2D.PIE);
            g2d.fill(arc);

            // Draw the segment outline
            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(1f));
            g2d.draw(arc);

            // Draw note name label
            double labelAngle = Math.toRadians(segmentAngle + arcAngle / 2);
            Point2D labelPos = new Point2D.Double(
                    centerX + Math.cos(labelAngle) * (radius * 0.75),
                    centerY + Math.sin(labelAngle) * (radius * 0.75));

            // Set text properties
            Font labelFont = isSelected ?
                    new Font("SansSerif", Font.BOLD, size / 9) :
                    new Font("SansSerif", Font.PLAIN, size / 10);
            g2d.setFont(labelFont);

            FontMetrics fm = g2d.getFontMetrics();
            String label = keyName;
            int labelW = fm.stringWidth(label);
            int labelH = fm.getHeight();

            // Highlight current selection
            if (isSelected) {
                g2d.setColor(Color.WHITE);
                int padding = 4;
                g2d.fillRoundRect(
                        (int) (labelPos.getX() - labelW / 2 - padding),
                        (int) (labelPos.getY() - labelH / 2),
                        labelW + padding * 2,
                        labelH,
                        8, 8);
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }

            g2d.drawString(label,
                    (int) (labelPos.getX() - labelW / 2),
                    (int) (labelPos.getY() + labelH / 4));
        }

        // Draw inner circle with octave
        int innerRadius = size / 6;
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillOval(
                (int) (centerX - innerRadius),
                (int) (centerY - innerRadius),
                innerRadius * 2,
                innerRadius * 2);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(
                (int) (centerX - innerRadius),
                (int) (centerY - innerRadius),
                innerRadius * 2,
                innerRadius * 2);

        // Draw octave number in center
        g2d.setFont(new Font("SansSerif", Font.BOLD, size / 6));
        String octaveText = String.valueOf(getOctave());
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(octaveText,
                (int) (centerX - fm.stringWidth(octaveText) / 2),
                (int) (centerY + fm.getHeight() / 4));

        // Draw "mode" indicator (major/minor)
//        String modeText = showMinorKeys ? "minor" : "MAJOR";
//        g2d.setFont(new Font("SansSerif", Font.ITALIC, size / 12));
//        fm = g2d.getFontMetrics();
//        g2d.drawString(modeText,
//                (int) (centerX - fm.stringWidth(modeText) / 2),
//                (int) (centerY + innerRadius + fm.getHeight() + 4));

        g2d.dispose();
    }

    /**
     * Toggles between showing major or minor keys
     */
    public void toggleMajorMinor() {
        showMinorKeys = !showMinorKeys;
        repaint();
    }
}
