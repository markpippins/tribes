package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing swing controls for melodic sequencer
 */
public class MelodicSequencerSwingPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerSwingPanel.class);
    // Reference to sequencer
    private final MelodicSequencer sequencer;
    // UI components
    private final JSlider swingSlider;
    private final JLabel valueLabel;
    private final JToggleButton swingToggle;

    /**
     * Creates a new swing control panel for melodic sequencer
     */    public MelodicSequencerSwingPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;

        // Reduce layout spacing
        setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));

        UIHelper.setWidgetPanelBorder(this, "Swing");
        
        // Add mouse wheel listener to the entire panel
        addMouseWheelListener(this::handleMouseWheelEvent);

        // Swing on/off toggle
        swingToggle = new JToggleButton("On", sequencer.getSequenceData().isSwingEnabled());
        swingToggle.setPreferredSize(new Dimension(40, 22));
        swingToggle.setMargin(new Insets(2, 2, 2, 2));
        swingToggle.addActionListener(e -> {
            sequencer.getSequenceData().setSwingEnabled(swingToggle.isSelected());
        });
        add(swingToggle);

        // Swing amount slider
        swingSlider = new JSlider(JSlider.HORIZONTAL,
                SequencerConstants.MIN_SWING, SequencerConstants.MAX_SWING, sequencer.getSequenceData().getSwingPercentage());
        swingSlider.setMajorTickSpacing(5);
        swingSlider.setPaintTicks(true);
        swingSlider.setPreferredSize(new Dimension(85, 22));

        valueLabel = new JLabel(sequencer.getSequenceData().getSwingPercentage() + "%");
        valueLabel.setFont(valueLabel.getFont().deriveFont(11f));
        valueLabel.setPreferredSize(new Dimension(25, 22));        swingSlider.addChangeListener(e -> {
            int value = swingSlider.getValue();
            sequencer.getSequenceData().setSwingPercentage(value);
            valueLabel.setText(value + "%");
        });        // Mouse wheel listener is now applied to the entire panel

        add(swingSlider);
        add(valueLabel);
    }    /**
     * Updates controls to match current sequencer state
     */
    public void updateControls() {
        swingToggle.setSelected(sequencer.getSequenceData().isSwingEnabled());
        swingSlider.setValue(sequencer.getSequenceData().getSwingPercentage());
        valueLabel.setText(sequencer.getSequenceData().getSwingPercentage() + "%");
    }
    
    /**
     * Handles mouse wheel events to adjust slider value
     * 
     * @param e The mouse wheel event
     */
    private void handleMouseWheelEvent(java.awt.event.MouseWheelEvent e) {
        if (!swingSlider.isEnabled()) {
            return;
        }
        
        // Determine scroll direction (-1 for up, 1 for down)
        int scrollDirection = e.getWheelRotation() > 0 ? -1 : 1;
        
        // Get current value and adjust by 1 in the scroll direction
        int currentValue = swingSlider.getValue();
        int newValue = currentValue + (scrollDirection * 1);
        
        // Ensure the new value respects the slider bounds
        newValue = Math.max(swingSlider.getMinimum(), Math.min(newValue, swingSlider.getMaximum()));
        
        // Update slider if value changed
        if (newValue != currentValue) {
            swingSlider.setValue(newValue);
            // This will trigger the change listener
        }
    }
}
