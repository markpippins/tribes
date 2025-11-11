package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel to display information about the currently selected drum pad
 */
public class DrumSequencerInfoPanel extends JPanel {
    private JLabel drumNameLabel;
    private JLabel noteLabel;
    private JLabel velocityLabel;
    private JLabel patternUsageLabel;

    private DrumSequencer sequencer;
    private int currentDrumIndex = 0;

    public DrumSequencerInfoPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;

        // Use a BorderLayout for the panel
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createTitledBorder("Selected Drum"));
        
        // Create top panel for drum info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // Create labels
        drumNameLabel = new JLabel("Name: -");
        noteLabel = new JLabel("Note: -");
        velocityLabel = new JLabel("Velocity: -");
        patternUsageLabel = new JLabel("Pattern: 0/16");
        
        // Add info labels horizontally
        infoPanel.add(drumNameLabel);
        infoPanel.add(new JLabel("|"));
        infoPanel.add(noteLabel);
        infoPanel.add(new JLabel("|"));
        infoPanel.add(velocityLabel);
        infoPanel.add(new JLabel("|"));
        infoPanel.add(patternUsageLabel);
        
        infoPanel.setPreferredSize(new Dimension(400, 30));
        infoPanel.setMinimumSize(new Dimension(400, 30));
        // Add info panel to main layout
        add(infoPanel, BorderLayout.CENTER);
        
        // Initial update with the default selected pad
        updateInfo(sequencer.getSelectedPadIndex());
    }
    
    /**
     * Update display with info about the selected drum
     */
    public void updateInfo(int drumPadIndex) {
        if (drumPadIndex < 0) return;
        
        currentDrumIndex = drumPadIndex;
        
        Player strike = sequencer.getPlayer(drumPadIndex);
        
        if (strike == null) {
            drumNameLabel.setText("Name: Not assigned");
            noteLabel.setText("Note: -");
            velocityLabel.setText("Velocity: -");
        } else {
            drumNameLabel.setText("Name: " + (strike.getName() != null ? strike.getName() : "Unnamed"));
            noteLabel.setText("Note: " + strike.getRootNote());
            velocityLabel.setText("Velocity: " + strike.getLevel());
        }
        
        // Update the pattern usage
        updatePatternUsageLabel();
        
        // Update panel title
        setBorder(BorderFactory.createTitledBorder(
            // BorderFactory.createLineBorder(Color.ORANGE, 2), 
            "Pad" + (drumPadIndex + 1)
        ));
        
        repaint();
    }
    
    /**
     * Update the pattern usage label to show active steps
     */
    private void updatePatternUsageLabel() {
        // Count how many steps are active for this drum pad
        int activeSteps = 0;
        int patternLength = sequencer.getPatternLength(currentDrumIndex);
        
        for (int step = 0; step < patternLength; step++) {
            if (sequencer.isStepActive(currentDrumIndex, step)) {
                activeSteps++;
            }
        }
        
        patternUsageLabel.setText("Pattern: " + activeSteps + "/" + patternLength);
    }
    
    /**
     * Update the panel for the selected drum
     */
    public void updateForDrum(int drumIndex) {
        updateInfo(drumIndex);
    }
}
