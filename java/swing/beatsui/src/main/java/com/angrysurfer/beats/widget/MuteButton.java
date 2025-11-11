package com.angrysurfer.beats.widget;

import java.awt.*;
import javax.swing.JToggleButton;

import com.angrysurfer.beats.util.UIHelper;

public class MuteButton extends JToggleButton {
    private static final long serialVersionUID = 1L;
    private Color baseColor = new Color(60, 60, 60);
    private Color activeColor = new Color(200, 30, 30); // Red for mute
    private boolean highlighted = false;
    private static final Dimension BUTTON_SIZE = new Dimension(25, 16);
    
    public MuteButton() {
        super();
        setup();
    }
    
    private void setup() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setToolTipText("Click to mute for this step");
        
        // Set fixed size
        setMaximumSize(BUTTON_SIZE);
        setPreferredSize(BUTTON_SIZE);
        setMinimumSize(BUTTON_SIZE);
    }
    
    public void setHighlighted(boolean highlighted) {
        if (this.highlighted != highlighted) {
            this.highlighted = highlighted;
            repaint();
        }
    }
    
    public boolean isHighlighted() {
        return highlighted;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        
        // Base color based on state
        if (highlighted) {
            g2d.setColor(UIHelper.fadedOrange);
        } else {
            g2d.setColor(isSelected() ? activeColor : baseColor);
        }
        
        // Fill button with gradient
        g2d.setPaint(new GradientPaint(
            0, 0, 
            g2d.getColor().brighter(), 
            0, height, 
            g2d.getColor().darker()
        ));
        g2d.fillRoundRect(0, 0, width, height, 6, 6);
        
        // Draw mute symbol or speaker icon
        if (isSelected()) {
            drawMuteSymbol(g2d, width, height);
        } else {
            drawSpeakerIcon(g2d, width, height);
        }
        
        // Draw border
        g2d.setColor(isSelected() ? activeColor.brighter() : baseColor.brighter());
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawRoundRect(0, 0, width-1, height-1, 6, 6);
        
        g2d.dispose();
    }
    
    private void drawMuteSymbol(Graphics2D g2d, int width, int height) {
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Draw speaker shape
        drawSpeakerBase(g2d, centerX, centerY);
        
        // Draw X over it
        g2d.drawLine(centerX+1, centerY-3, centerX+5, centerY+3);
        g2d.drawLine(centerX+5, centerY-3, centerX+1, centerY+3);
    }
    
    private void drawSpeakerIcon(Graphics2D g2d, int width, int height) {
        g2d.setColor(Color.WHITE);
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Draw speaker base
        drawSpeakerBase(g2d, centerX, centerY);
        
        // Draw sound waves
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawArc(centerX+2, centerY-3, 3, 6, -45, 90);
        g2d.drawArc(centerX+3, centerY-4, 5, 8, -45, 90);
    }
    
    private void drawSpeakerBase(Graphics2D g2d, int centerX, int centerY) {
        // Speaker cone
        int[] xPoints = {centerX-5, centerX-1, centerX+2, centerX+2};
        int[] yPoints = {centerY, centerY-3, centerY-3, centerY+3};
        g2d.fillPolygon(xPoints, yPoints, 4);
    }
}
