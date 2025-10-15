package com.angrysurfer.beats.panel.modulation;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel containing multiple XY control pads
 */
public class QuadXYPadPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(QuadXYPadPanel.class);
    private static final int PAD_COUNT = 4; // Number of XY pads
    private static final int PAD_SIZE = 150; // Size of each pad in pixels
    private static final int PAD_MARGIN = 20; // Margin between pads
    private static final int KNOB_RADIUS = 12; // Radius of control knobs
    
    private final XYPadPositions positions;
    private int activePad = -1; // Currently active pad (-1 = none)
    
    // Labels for each pad
    private final String[] padLabels = {
        "Filter Cutoff/Res", 
        "LFO Rate/Depth", 
        "Delay Time/Feedback", 
        "Reverb Size/Mix"
    };
    
    // X/Y axis labels for each pad
    private final String[][] axisLabels = {
        {"Cutoff", "Resonance"}, 
        {"Rate", "Depth"}, 
        {"Time", "Feedback"}, 
        {"Size", "Mix"}
    };
    
    // Colors for each pad
    private final Color[] padColors = {
        new Color(120, 180, 255), // Blue
        new Color(255, 140, 100), // Orange  
        new Color(120, 220, 120), // Green
        new Color(220, 120, 220)  // Purple
    };
    
    // Listeners
    private final List<XYPadListener> listeners = new ArrayList<>();
    
    /**
     * Listener interface for XY pad changes
     */
    public interface XYPadListener {
        void padPositionChanged(int padIndex, float x, float y);
    }
    
    /**
     * Add a listener to receive pad position change events
     */
    public void addXYPadListener(XYPadListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a listener
     */
    public void removeXYPadListener(XYPadListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify listeners of position change
     */
    private void notifyListeners(int padIndex) {
        XYPadPositions.Point2D position = positions.getPosition(padIndex);
        if (position != null) {
            for (XYPadListener listener : listeners) {
                listener.padPositionChanged(padIndex, position.getX(), position.getY());
            }
        }
    }
    
    /**
     * Constructor
     */
    public QuadXYPadPanel() {
        positions = new XYPadPositions(PAD_COUNT);
        
        // Set up the panel
        setBackground(new Color(30, 30, 30));
        setPreferredSize(new Dimension(
            PAD_SIZE * 2 + PAD_MARGIN * 3,
            PAD_SIZE * 2 + PAD_MARGIN * 3
        ));
        
        // Add mouse event handlers
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                activePad = getPadAtPoint(e.getX(), e.getY());
                if (activePad >= 0) {
                    updatePadPosition(activePad, e.getX(), e.getY());
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                activePad = -1;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (activePad >= 0) {
                    updatePadPosition(activePad, e.getX(), e.getY());
                    repaint();
                }
            }
        });
    }
    
    /**
     * Update a pad's position based on mouse coordinates
     */
    private void updatePadPosition(int padIndex, int mouseX, int mouseY) {
        // Get pad bounds
        Rectangle bounds = getPadBounds(padIndex);
        
        // Calculate normalized position (0.0 - 1.0)
        float x = Math.max(0.0f, Math.min(1.0f, (mouseX - bounds.x) / (float)bounds.width));
        float y = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - bounds.y) / (float)bounds.height));
        
        // Update position
        positions.setPosition(padIndex, x, y);
        
        // Notify listeners
        notifyListeners(padIndex);
    }
    
    /**
     * Determine which pad is at the given point
     * @return pad index or -1 if none
     */
    private int getPadAtPoint(int x, int y) {
        for (int i = 0; i < PAD_COUNT; i++) {
            if (getPadBounds(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Get the bounds rectangle for a pad
     */
    private Rectangle getPadBounds(int padIndex) {
        int row = padIndex / 2;
        int col = padIndex % 2;
        
        int x = PAD_MARGIN + col * (PAD_SIZE + PAD_MARGIN);
        int y = PAD_MARGIN + row * (PAD_SIZE + PAD_MARGIN);
        
        return new Rectangle(x, y, PAD_SIZE, PAD_SIZE);
    }
    
    /**
     * Get the current position of a pad
     */
    public XYPadPositions.Point2D getPadPosition(int padIndex) {
        return positions.getPosition(padIndex);
    }
    
    /**
     * Set the position of a pad programmatically
     */
    public void setPadPosition(int padIndex, float x, float y) {
        if (padIndex >= 0 && padIndex < PAD_COUNT) {
            // Constrain values to 0.0 - 1.0
            x = Math.max(0.0f, Math.min(1.0f, x));
            y = Math.max(0.0f, Math.min(1.0f, y));
            
            positions.setPosition(padIndex, x, y);
            repaint();
            
            // Notify listeners
            notifyListeners(padIndex);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw each pad
        for (int i = 0; i < PAD_COUNT; i++) {
            drawPad(g2d, i);
        }
    }
    
    /**
     * Draw a single XY pad
     */
    private void drawPad(Graphics2D g2d, int padIndex) {
        Rectangle bounds = getPadBounds(padIndex);
        XYPadPositions.Point2D position = positions.getPosition(padIndex);
        Color padColor = padColors[padIndex];
        
        // Draw pad background
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        // Draw pad border
        g2d.setColor(padColor.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        // Draw grid lines
        g2d.setColor(new Color(70, 70, 70));
        g2d.setStroke(new BasicStroke(1));
        
        // Vertical grid lines
        for (int i = 1; i < 4; i++) {
            int x = bounds.x + (bounds.width * i) / 4;
            g2d.drawLine(x, bounds.y, x, bounds.y + bounds.height);
        }
        
        // Horizontal grid lines
        for (int i = 1; i < 4; i++) {
            int y = bounds.y + (bounds.height * i) / 4;
            g2d.drawLine(bounds.x, y, bounds.x + bounds.width, y);
        }
        
        // Draw center lines
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawLine(bounds.x, bounds.y + bounds.height/2, bounds.x + bounds.width, bounds.y + bounds.height/2);
        g2d.drawLine(bounds.x + bounds.width/2, bounds.y, bounds.x + bounds.width/2, bounds.y + bounds.height);
        
        // Draw position marker
        int markerX = bounds.x + Math.round(position.getX() * bounds.width);
        int markerY = bounds.y + Math.round((1.0f - position.getY()) * bounds.height);
        
        // Draw cross lines to marker
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                   10.0f, new float[]{3.0f, 3.0f}, 0.0f));
        g2d.setColor(padColor.darker());
        g2d.drawLine(bounds.x, markerY, bounds.x + bounds.width, markerY);
        g2d.drawLine(markerX, bounds.y, markerX, bounds.y + bounds.height);
        
        // Draw knob shadow
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillOval(markerX - KNOB_RADIUS + 2, markerY - KNOB_RADIUS + 2, 
                   KNOB_RADIUS * 2, KNOB_RADIUS * 2);
        
        // Draw knob
        g2d.setColor(padColor);
        g2d.fillOval(markerX - KNOB_RADIUS, markerY - KNOB_RADIUS, 
                   KNOB_RADIUS * 2, KNOB_RADIUS * 2);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(markerX - KNOB_RADIUS, markerY - KNOB_RADIUS, 
                   KNOB_RADIUS * 2, KNOB_RADIUS * 2);
        
        // Draw pad label
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String label = padLabels[padIndex];
        int textWidth = fm.stringWidth(label);
        g2d.drawString(label, bounds.x + (bounds.width - textWidth) / 2, 
                     bounds.y + bounds.height + 15);
        
        // Draw axis labels
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        fm = g2d.getFontMetrics();
        
        // X-axis label (at bottom)
        String xLabel = axisLabels[padIndex][0];
        textWidth = fm.stringWidth(xLabel);
        g2d.drawString(xLabel, bounds.x + (bounds.width - textWidth) / 2, 
                     bounds.y + bounds.height - 5);
        
        // Y-axis label (rotated at left)
        String yLabel = axisLabels[padIndex][1];
        g2d.rotate(-Math.PI/2, bounds.x - 5, bounds.y + bounds.height/2);
        g2d.drawString(yLabel, bounds.x - 5 - fm.stringWidth(yLabel)/2, 
                     bounds.y + bounds.height/2 + fm.getAscent()/2);
        g2d.rotate(Math.PI/2, bounds.x - 5, bounds.y + bounds.height/2);
        
        // Draw value indicators (percentage)
        String xValue = String.format("%.0f%%", position.getX() * 100);
        String yValue = String.format("%.0f%%", position.getY() * 100);
        
        // Draw X percentage at right
        g2d.setColor(padColor);
        g2d.drawString(xValue, bounds.x + bounds.width + 5, markerY + 4);
        
        // Draw Y percentage at top
        g2d.drawString(yValue, markerX - fm.stringWidth(yValue)/2, bounds.y - 5);
    }
    
    /**
     * Static method to create a frame with the XY pad panel for testing
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("XY Pad Controller");
            QuadXYPadPanel xyPadPanel = new QuadXYPadPanel();
            
            // Add listener for demonstration
            xyPadPanel.addXYPadListener((padIndex, x, y) -> {
                logger.info("Pad {} position: {:.2f}, {:.2f}", padIndex, x, y);
            });
            
            JPanel infoPanel = new JPanel();
            JTextArea infoArea = new JTextArea(6, 40);
            infoArea.setEditable(false);
            infoArea.setText("XY Pad Controller\n\n" +
                           "Drag the knobs to control parameters.\n" +
                           "Each pad provides two parameters (X and Y axis).\n" +
                           "Values are normalized from 0.0 to 1.0.");
            infoPanel.add(new JScrollPane(infoArea));
            
            frame.setLayout(new BorderLayout());
            frame.add(xyPadPanel, BorderLayout.CENTER);
            frame.add(infoPanel, BorderLayout.SOUTH);
            
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Data structure to hold the positions of multiple XY pads
     */
    public static class XYPadPositions {
        private final List<Point2D> positions;

        public XYPadPositions(int padCount) {
            positions = new ArrayList<>(padCount);
            for (int i = 0; i < padCount; i++) {
                positions.add(new Point2D(0.5f, 0.5f)); // Initialize at center
            }
        }

        public Point2D getPosition(int padIndex) {
            if (padIndex >= 0 && padIndex < positions.size()) {
                return positions.get(padIndex);
            }
            return null;
        }

        public void setPosition(int padIndex, float x, float y) {
            if (padIndex >= 0 && padIndex < positions.size()) {
                positions.get(padIndex).set(x, y);
            }
        }

        public int getCount() {
            return positions.size();
        }

        /**
         * Inner class representing a 2D point with float coordinates
         */
        public static class Point2D {
            private float x;
            private float y;

            public Point2D(float x, float y) {
                this.x = x;
                this.y = y;
            }

            public void set(float x, float y) {
                this.x = x;
                this.y = y;
            }

            public float getX() {
                return x;
            }

            public float getY() {
                return y;
            }

            public void setX(float x) {
                this.x = x;
            }

            public void setY(float y) {
                this.y = y;
            }

            @Override
            public String toString() {
                return String.format("(%.2f, %.2f)", x, y);
            }
        }
    }
}