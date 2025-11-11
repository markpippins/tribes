package com.angrysurfer.beats.panel.modulation;

import com.angrysurfer.beats.widget.LEDIndicator;

import java.awt.*;

/**
 * Enhanced LED indicator specifically for Turing Machine visualization
 * with multiple colors based on bit value and current position
 */
public class TuringMachineLED extends LEDIndicator {
    
    // Colors for different states
    private static final Color COLOR_ON_ACTIVE = new Color(20, 220, 20);    // Bright green (1 + active)
    private static final Color COLOR_OFF_ACTIVE = new Color(220, 20, 20);   // Bright red (0 + active)
    private static final Color COLOR_ON_NORMAL = new Color(0, 160, 0);      // Normal green (1)
    private static final Color COLOR_OFF_NORMAL = new Color(160, 0, 0);     // Normal red (0)
    
    // Bit value and active status
    private boolean bitValue = false;
    private boolean isActivePosition = false;
    
    /**
     * Create a new Turing Machine LED
     * 
     * @param label Optional label text
     */
    public TuringMachineLED(String label) {
        super(COLOR_OFF_NORMAL, label);
        
        // We're always "on" - the color represents the value
        setOn(true);
    }
    
    /**
     * Set the bit value and update the LED appearance
     * 
     * @param value The bit value (true=1, false=0)
     */
    public void setBitValue(boolean value) {
        this.bitValue = value;
        updateColor();
    }
    
    /**
     * Set whether this LED is at the active position
     * 
     * @param active True if this is the active bit position
     */
    public void setActivePosition(boolean active) {
        this.isActivePosition = active;
        updateColor();
    }
    
    /**
     * Update the LED color based on current state
     */
    private void updateColor() {
        if (isActivePosition) {
            setOnColor(bitValue ? COLOR_ON_ACTIVE : COLOR_OFF_ACTIVE);
        } else {
            setOnColor(bitValue ? COLOR_ON_NORMAL : COLOR_OFF_NORMAL);
        }
        repaint();
    }
    
    /**
     * Get the bit value
     */
    public boolean getBitValue() {
        return bitValue;
    }
    
    /**
     * Override paint component to add a pulsing effect for active position
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Optionally add pulsing animation for active position
        if (isActivePosition) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 0.5f));
            
            int size = Math.min(getWidth(), getHeight()) / 3;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x, y, size, size);
            g2d.dispose();
        }
    }
}
