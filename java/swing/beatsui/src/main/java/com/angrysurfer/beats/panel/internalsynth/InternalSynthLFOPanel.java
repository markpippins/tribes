package com.angrysurfer.beats.panel.internalsynth;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Hashtable;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel for controlling LFO (Low Frequency Oscillator) parameters of a synthesizer
 */
@Getter
@Setter
 public class InternalSynthLFOPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthLFOPanel.class);
    
    // LFO control constants
    public static final int CC_LFO_WAVEFORM = 12;
    public static final int CC_LFO_DESTINATION = 13;
    public static final int CC_LFO_RATE = 76;
    public static final int CC_LFO_AMOUNT = 77;
    
    private final transient Synthesizer synthesizer;
    private Integer midiChannel;
    
    // UI components
    private JSlider waveformSlider;
    private JSlider destinationSlider;
    private JSlider rateSlider;
    private JSlider amountSlider;
    
    /**
     * Create a new LFO control panel
     * 
     * @param synthesizer The MIDI synthesizer to control
     * @param midiChannel The MIDI channel to send control changes to
     */
    public InternalSynthLFOPanel(Synthesizer synthesizer, int midiChannel) {
        super();
        this.synthesizer = synthesizer;
        this.midiChannel = midiChannel;
        
        initializeUI();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeUI() {
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "LFO Controls",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));

        // Use FlowLayout for sliders in a row
        setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));

        // Create vertical sliders with labeled ticks
        waveformSlider = createLabeledVerticalSlider(
                "LFO Waveform", 0, 3, 0,
                new String[]{"Sine", "Triangle", "Square", "S&H"}
        );

        destinationSlider = createLabeledVerticalSlider(
                "LFO Destination", 0, 3, 0,
                new String[]{"Off", "Pitch", "Filter", "Amp"}
        );

        rateSlider = createVerticalSlider("LFO Rate", 50);
        amountSlider = createVerticalSlider("LFO Amount", 0);

        // Create slider groups with labels
        JPanel waveGroup = createSliderGroup("Waveform", waveformSlider);
        JPanel destGroup = createSliderGroup("Destination", destinationSlider);
        JPanel rateGroup = createSliderGroup("Rate", rateSlider);
        JPanel amountGroup = createSliderGroup("Amount", amountSlider);

        // Add slider groups to panel
        add(waveGroup);
        add(destGroup);
        add(rateGroup);
        add(amountGroup);

        // Add event listeners
        setupEventHandlers();
    }
    
    /**
     * Add event listeners to all controls
     */
    private void setupEventHandlers() {
        waveformSlider.addChangeListener(e -> {
            if (!waveformSlider.getValueIsAdjusting()) {
                int value = waveformSlider.getValue();
                setControlChange(CC_LFO_WAVEFORM, value * 42); // Scale to 0-127 range
            }
        });

        destinationSlider.addChangeListener(e -> {
            if (!destinationSlider.getValueIsAdjusting()) {
                int value = destinationSlider.getValue();
                setControlChange(CC_LFO_DESTINATION, value * 42); // Scale to 0-127 range
            }
        });

        rateSlider.addChangeListener(e -> {
            if (!rateSlider.getValueIsAdjusting()) {
                setControlChange(CC_LFO_RATE, rateSlider.getValue());
            }
        });

        amountSlider.addChangeListener(e -> {
            if (!amountSlider.getValueIsAdjusting()) {
                setControlChange(CC_LFO_AMOUNT, amountSlider.getValue());
            }
        });
    }
    
    /**
     * Reset all controls to their default values
     */
    public void resetToDefaults() {
        waveformSlider.setValue(0);    // Sine
        destinationSlider.setValue(0); // Off
        rateSlider.setValue(50);       // Mid rate
        amountSlider.setValue(0);      // No amount
        
        // Send these values to the synth
        updateSynthState();
    }
    
    /**
     * Send the current state of all controls to the synthesizer
     */
    public void updateSynthState() {
        setControlChange(CC_LFO_WAVEFORM, waveformSlider.getValue() * 42);
        setControlChange(CC_LFO_DESTINATION, destinationSlider.getValue() * 42);
        setControlChange(CC_LFO_RATE, rateSlider.getValue());
        setControlChange(CC_LFO_AMOUNT, amountSlider.getValue());
    }
    
    /**
     * Set a MIDI CC value on the synth
     * 
     * @param ccNumber The CC number to set
     * @param value The value to set (0-127)
     */
    private void setControlChange(int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel midiCh = synthesizer.getChannels()[midiChannel];
                if (midiCh != null) {
                    midiCh.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                logger.error("Error setting CC {} on channel {}: {}", ccNumber, (midiChannel + 1), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Create a slider with a label underneath
     */
    private JPanel createSliderGroup(String title, JSlider slider) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));

        // Center the slider
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sliderPanel.add(slider);

        // Add label at bottom
        JLabel label = new JLabel(title);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        group.add(sliderPanel);
        group.add(label);

        return group;
    }

    /**
     * Create a vertical slider with consistent styling
     * 
     * @param tooltip Tooltip text
     * @param initialValue Initial value (0-127)
     * @return Configured JSlider
     */
    private JSlider createVerticalSlider(String tooltip, int initialValue) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, 127, initialValue);
        slider.setToolTipText(tooltip);

        // Set up tick marks
        slider.setMajorTickSpacing(32);
        slider.setMinorTickSpacing(16);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(false);

        // Create tick labels - just show a few key values
    Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(32, new JLabel("32"));
        labelTable.put(64, new JLabel("64"));
        labelTable.put(96, new JLabel("96"));
        labelTable.put(127, new JLabel("127"));
        slider.setLabelTable(labelTable);

        // FlatLaf properties
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);

        // Set reasonable size for a vertical slider
        slider.setPreferredSize(new Dimension(60, 120));

        return slider;
    }

    /**
     * Create a vertical slider with labeled tick marks
     * 
     * @param tooltip Tooltip text
     * @param min Minimum value
     * @param max Maximum value 
     * @param initialValue Initial value
     * @param labels Array of labels for tick marks
     * @return Configured JSlider with labels
     */
    private JSlider createLabeledVerticalSlider(String tooltip, int min, int max, int initialValue, String[] labels) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, min, max, initialValue);
        slider.setToolTipText(tooltip);

        // Set up tick marks and labels
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);

    // Create tick labels
    Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (int i = min; i <= max; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Dialog", Font.PLAIN, 9));
            labelTable.put(i, label);
        }
        slider.setLabelTable(labelTable);

        // Add FlatLaf styling
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);

        // Set reasonable size for a vertical slider
        slider.setPreferredSize(new Dimension(60, 120));

        return slider;
    }
    
    // Getters for individual control values
    
    public int getWaveform() {
        return waveformSlider.getValue();
    }
    
    public void setWaveform(int value) {
        waveformSlider.setValue(value);
    }
    
    public int getDestination() {
        return destinationSlider.getValue();
    }
    
    public void setDestination(int value) {
        destinationSlider.setValue(value);
    }
    
    public int getRate() {
        return rateSlider.getValue();
    }
    
    public void setRate(int value) {
        rateSlider.setValue(value);
    }
    
    public int getAmount() {
        return amountSlider.getValue();
    }
    
    public void setAmount(int value) {
        amountSlider.setValue(value);
    }
}
