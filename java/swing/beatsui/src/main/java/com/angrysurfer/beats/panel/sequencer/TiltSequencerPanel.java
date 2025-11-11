package com.angrysurfer.beats.panel.sequencer;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.ScaleDegreeSelectionDial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SequencerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel with 16 tilt dials that respond to bar changes via TimingBus
 */
public class TiltSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(TiltSequencerPanel.class);
    private static final int DIAL_COUNT = 16;
    private static final int MIN_VALUE = -7;
    private static final int MAX_VALUE = 7;
    private static final int DEFAULT_VALUE = 0;

    private final List<ScaleDegreeSelectionDial> tiltDials = new ArrayList<>(DIAL_COUNT);
    private final List<JPanel> dialContainers = new ArrayList<>(DIAL_COUNT);
    private final MelodicSequencer sequencer;
    private int currentBar = 0;

    public TiltSequencerPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        setLayout(new BorderLayout());
        UIHelper.setWidgetPanelBorder(this, "Harmonic Tilt");
        setPreferredSize(new Dimension(getPreferredSize().width, 100));

        JPanel dialsPanel = new JPanel(new GridLayout(1, DIAL_COUNT, 2, 0));
        JPanel labelsPanel = new JPanel(new GridLayout(1, DIAL_COUNT, 2, 0));

        dialsPanel.setBackground(getBackground());
        labelsPanel.setBackground(getBackground());

        // Create the dials and labels
        for (int i = 0; i < DIAL_COUNT; i++) {
            // Create a dial
            ScaleDegreeSelectionDial dial = createTiltDial(i);
            tiltDials.add(dial);

            // Create container for dial with spacing
            JPanel dialContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            // dialContainer.setPreferredSize(new Dimension(80, 80));
            dialContainer.add(dial);

            dialContainers.add(dialContainer);
            dialsPanel.add(dialContainer);

            // Create label for dial
            // JLabel label = new JLabel(String.valueOf(i + 1), JLabel.CENTER);
            // label.setFont(label.getFont().deriveFont(10f));
            // labelsPanel.add(label);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(dialsPanel, BorderLayout.CENTER);
        mainPanel.add(labelsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Register with the TimingBus
        TimingBus.getInstance().register(this);
    }

    private ScaleDegreeSelectionDial createTiltDial(int index) {
        // Create a scale degree dial with the appropriate range
        ScaleDegreeSelectionDial dial = new ScaleDegreeSelectionDial();

        // Set size smaller than default
        dial.setPreferredSize(new Dimension(70, 70));

        // Configure appearance
        dial.setMinimum(MIN_VALUE);
        dial.setMaximum(MAX_VALUE);
        dial.setValue(DEFAULT_VALUE, false);

        // Add change listener
        dial.addChangeListener(e -> {
            int tiltValue = dial.getValue();
            logger.debug("Tilt value changed for bar {}: {} ({})",
                    index + 1, tiltValue, dial.getScaleDegreeLabel());

            // Store the tilt value in the sequencer
            if (sequencer != null) {
                sequencer.getSequenceData().getHarmonicTiltValuesRaw()[index] = tiltValue;
                sequencer.getSequenceData().setTiltValue(index, tiltValue);
                SequencerService.getInstance().saveSequence(sequencer);
            }

            // If this is the current active bar, apply the tilt immediately
            if (index == (currentBar % DIAL_COUNT)) {
                applyTilt();
            }
        });

        return dial;
    }

    @Override
    public void onAction(Command action) {
        if (action.getData() instanceof TimingUpdate update) {
            // Check if it's a bar change
            if (update.bar() != currentBar) {
                currentBar = update.bar() - 1;
                highlightCurrentBar();
                applyTilt();
            }
        }
    }

    /**
     * Apply the tilt value for the current bar to the sequencer
     */
    private void applyTilt() {
        // Get current bar within the 16-bar pattern
        int barIndex = currentBar % DIAL_COUNT;

        // Get the tilt value for the current bar
        int tiltValue = getTiltValue(barIndex);

        // Apply the tilt to the sequencer
        if (sequencer != null) {
            // Apply the note offset to the sequencer
            sequencer.setCurrentTilt(tiltValue);

            String degreeLabel = "I";
            if (barIndex >= 0 && barIndex < tiltDials.size()) {
                degreeLabel = tiltDials.get(barIndex).getScaleDegreeLabel();
            }

            logger.debug("Applied tilt of {} ({}) to bar {}",
                    tiltValue, degreeLabel, currentBar + 1);
        }
    }

    private void highlightCurrentBar() {
        // Map the bar to a step (potentially with wrap-around)
        int dialToHighlight = currentBar % DIAL_COUNT;

        // Update the appearance of all dial containers
        for (int i = 0; i < dialContainers.size(); i++) {
            JPanel container = dialContainers.get(i);

            if (i == dialToHighlight) {
                // Highlight the current step
                // container.setBorder(BorderFactory.createLoweredSoftBevelBorder()); //
                container.setBorder(BorderFactory.createLineBorder(UIHelper.coolBlue, 1));
            } else {
                // Reset highlighting
                container.setBorder(null);
            }

            container.repaint();
        }
    }

    /**
     * Get the tilt value for a specific step
     */
    public int getTiltValue(int step) {
        if (step >= 0 && step < tiltDials.size()) {
            return tiltDials.get(step).getValue();
        }
        return DEFAULT_VALUE;
    }

    /**
     * Set the tilt value for a specific step
     */
    public void setTiltValue(int step, int value) {
        if (step >= 0 && step < tiltDials.size()) {
            tiltDials.get(step).setValue(Math.max(MIN_VALUE, Math.min(MAX_VALUE, value)), false);
        }
    }

    /**
     * Synchronize the dial positions with the sequencer tilt values
     */
    public void syncWithSequencer() {
        if (sequencer == null) {
            logger.warn("Cannot sync TiltSequencerPanel - sequencer is null");
            return;
        }

        logger.debug("Syncing tilt panel with sequencer values");

        // Use more direct access to harmonic tilt values for debugging
        List<Integer> tiltValues = sequencer.getHarmonicTiltValues();

        // Log details about what we got
        if (tiltValues != null) {
            logger.info("Received {} tilt values from sequencer: {}",
                    tiltValues.size(), tiltValues);

            // Loop through and update dial values
            for (int i = 0; i < Math.min(tiltDials.size(), tiltValues.size()); i++) {
                int tiltValue = tiltValues.get(i);

                // Ensure the value is in range
                int safeValue = Math.max(MIN_VALUE, Math.min(MAX_VALUE, tiltValue));

                // Set the value without triggering change listeners
                tiltDials.get(i).setValue(safeValue, false);

                // Force immediate repaint of this dial
                tiltDials.get(i).repaint();
            }
        } else {
            logger.warn("No harmonic tilt values available from sequencer");
        }

        // Force complete panel repaint
        revalidate();
        repaint();
    }
}
