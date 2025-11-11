package com.angrysurfer.beats.panel.sample;

import java.awt.*;
import javax.swing.*;
import javax.sound.sampled.AudioFormat;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaveformControlsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WaveformControlsPanel.class);
    
    // Callback interface
    public interface AudioControlListener {
        void onPlayRequested();
        void onStopRequested();
        void onPlaySelectionRequested();
        void onZoomChanged(double zoomFactor);
        void onCreatePlayerRequested(); // New method
    }
    
    // UI Components
    private JButton playButton;
    private JButton playSelectionButton; // New button
    private JButton stopButton;
    private JButton createPlayerButton; // New button
    private JLabel durationLabel;
    private JLabel selectionLabel;
    private JSlider zoomSlider;
    private JLabel zoomLabel;
    
    // Listener for control actions
    private AudioControlListener listener;
    
    public WaveformControlsPanel(AudioControlListener listener) {
        this.listener = listener;
        
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // Play button
        playButton = new JButton("▶ Play All");
        playButton.setEnabled(false);
        playButton.addActionListener(e -> {
            if (listener != null) {
                listener.onPlayRequested();
            }
        });
        
        // Play Selection button (new)
        playSelectionButton = new JButton("▶ Selection");
        playSelectionButton.setEnabled(false);
        playSelectionButton.addActionListener(e -> {
            if (listener != null) {
                listener.onPlaySelectionRequested();
            }
        });
        
        // Stop button
        stopButton = new JButton("⏹ Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> {
            if (listener != null) {
                listener.onStopRequested();
            }
        });
        
        // Create Player button - new addition
        createPlayerButton = new JButton("➕ Create Player");
        createPlayerButton.setEnabled(false);
        createPlayerButton.addActionListener(e -> {
            if (listener != null) {
                listener.onCreatePlayerRequested();
            }
        });
        
        // Zoom slider with expanded range (0.1x to 10x)
        zoomSlider = new JSlider(1, 100, 50); // Values will be mapped
        zoomSlider.setPreferredSize(new Dimension(150, 20));
        zoomSlider.setToolTipText("Zoom");
        zoomSlider.addChangeListener(e -> {
            // Map slider values to exponential zoom scale (0.1x to 10x)
            double value = zoomSlider.getValue() / 50.0; // 50 = 1x zoom
            double zoomFactor;
            
            if (value < 1.0) {
                // 0.1x to 1x (logarithmic)
                zoomFactor = Math.pow(10, value - 1);
            } else {
                // 1x to 10x (logarithmic)
                zoomFactor = value; 
            }
            
            updateZoomLabel(zoomFactor);
            
            if (listener != null) {
                listener.onZoomChanged(zoomFactor);
            }
        });
        
        // Zoom label
        zoomLabel = new JLabel("1.00x");
        
        // Add buttons to panel
        buttonsPanel.add(playButton);
        buttonsPanel.add(playSelectionButton); // Add the new button
        buttonsPanel.add(stopButton);
        buttonsPanel.add(createPlayerButton); // Add the new button
        buttonsPanel.add(new JLabel("Zoom:"));
        buttonsPanel.add(zoomSlider);
        buttonsPanel.add(zoomLabel);
        
        // Duration and selection labels
        durationLabel = new JLabel("Duration: --:--");
        selectionLabel = new JLabel("Selection: --:-- to --:--");
        
        JPanel labelsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        labelsPanel.add(durationLabel);
        labelsPanel.add(selectionLabel);
        
        // Add components to panel
        add(buttonsPanel, BorderLayout.WEST);
        add(labelsPanel, BorderLayout.EAST);
    }
    
    // Update control states
    public void setPlayEnabled(boolean enabled) {
        playButton.setEnabled(enabled);
    }
    
    public void setPlaySelectionEnabled(boolean enabled) {
        playSelectionButton.setEnabled(enabled);
    }
    
    public void setStopEnabled(boolean enabled) {
        stopButton.setEnabled(enabled);
    }
    
    public void setCreatePlayerEnabled(boolean enabled) {
        createPlayerButton.setEnabled(enabled);
    }
    
    public void updateDurationLabel(double duration) {
        if (duration <= 0) {
            durationLabel.setText("Duration: --:--");
        } else {
            String formattedDuration = formatTime(duration);
            durationLabel.setText("Duration: " + formattedDuration);
        }
    }
    
    public void updateSelectionLabel(int startFrame, int endFrame, AudioFormat format) {
        if (format == null) {
            selectionLabel.setText("Selection: --:-- to --:--");
            return;
        }
        
        double startTime = startFrame / format.getFrameRate();
        double endTime = endFrame / format.getFrameRate();
        
        String formattedStart = formatTime(startTime);
        String formattedEnd = formatTime(endTime);
        
        selectionLabel.setText("Selection: " + formattedStart + " to " + formattedEnd);
    }
    
    private void updateZoomLabel(double zoomFactor) {
        zoomLabel.setText(String.format("%.2fx", zoomFactor));
    }
    
    /**
     * Format time value as mm:ss.ms
     */
    private String formatTime(double seconds) {
        int mins = (int)(seconds / 60);
        int secs = (int)(seconds % 60);
        int ms = (int)((seconds - Math.floor(seconds)) * 1000);
        
        return String.format("%02d:%02d.%03d", mins, secs, ms);
    }
}
