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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * Panel for controlling filter parameters of a synthesizer
 */
@Getter
@Setter
public class InternalSynthFilterPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(InternalSynthFilterPanel.class);

    // Filter control constants
    public static final int CC_FILTER_TYPE = 102;
    public static final int CC_FILTER_CUTOFF = 74;
    public static final int CC_FILTER_RESONANCE = 71;
    public static final int CC_ENV_AMOUNT = 110;

    private final Synthesizer synthesizer;
    private int midiChannel;

    // UI components
    private JSlider filterTypeSlider;
    private JSlider cutoffSlider;
    private JSlider resonanceSlider;
    private JSlider envAmountSlider;

    /**
     * Create a new Filter control panel
     *
     * @param synthesizer The MIDI synthesizer to control
     * @param midiChannel The MIDI channel to send control changes to
     */
    public InternalSynthFilterPanel(Synthesizer synthesizer, int midiChannel) {
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
                "Filter",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));

        // Use FlowLayout for sliders in a row
        setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));

        // Create filter type slider with labels
        filterTypeSlider = createLabeledVerticalSlider(
                "Filter Type", 0, 3, 0,
                new String[]{"Low Pass", "High Pass", "Band Pass", "Notch"}
        );

        // Create other sliders
        cutoffSlider = createVerticalSlider("Filter Cutoff Frequency", 100);
        resonanceSlider = createVerticalSlider("Resonance/Q", 10);
        envAmountSlider = createVerticalSlider("Envelope Amount", 0);

        // Create slider groups - filter type first
        JPanel typeGroup = createSliderGroup("Type", filterTypeSlider);
        JPanel cutoffGroup = createSliderGroup("Cutoff", cutoffSlider);
        JPanel resonanceGroup = createSliderGroup("Resonance", resonanceSlider);
        JPanel envAmtGroup = createSliderGroup("Env Amount", envAmountSlider);

        // Add to filter params panel - filter type first
        add(typeGroup);
        add(cutoffGroup);
        add(resonanceGroup);
        add(envAmtGroup);

        // Add event listeners
        setupEventHandlers();
    }

    /**
     * Add event listeners to all controls
     */
    private void setupEventHandlers() {
        filterTypeSlider.addChangeListener(e -> {
            if (!filterTypeSlider.getValueIsAdjusting()) {
                int filterType = filterTypeSlider.getValue();
                setControlChange(CC_FILTER_TYPE, filterType * 32); // Scale to 0-127 range
                logger.debug("Filter type: {} (CC{}={})", filterType, CC_FILTER_TYPE, filterType * 32);
            }
        });

        cutoffSlider.addChangeListener(e -> {
            if (!cutoffSlider.getValueIsAdjusting()) {
                setControlChange(CC_FILTER_CUTOFF, cutoffSlider.getValue());
            }
        });

        resonanceSlider.addChangeListener(e -> {
            if (!resonanceSlider.getValueIsAdjusting()) {
                setControlChange(CC_FILTER_RESONANCE, resonanceSlider.getValue());
            }
        });

        envAmountSlider.addChangeListener(e -> {
            if (!envAmountSlider.getValueIsAdjusting()) {
                setControlChange(CC_ENV_AMOUNT, envAmountSlider.getValue());
            }
        });
    }

    /**
     * Reset all controls to their default values
     */
    public void resetToDefaults() {
        filterTypeSlider.setValue(0);    // Low Pass
        cutoffSlider.setValue(100);      // Fairly open filter
        resonanceSlider.setValue(10);    // Light resonance
        envAmountSlider.setValue(0);     // No envelope modulation

        // Send these values to the synth
        updateSynthState();
    }

    /**
     * Send the current state of all controls to the synthesizer
     */
    public void updateSynthState() {
        setControlChange(CC_FILTER_TYPE, filterTypeSlider.getValue() * 32);
        setControlChange(CC_FILTER_CUTOFF, cutoffSlider.getValue());
        setControlChange(CC_FILTER_RESONANCE, resonanceSlider.getValue());
        setControlChange(CC_ENV_AMOUNT, envAmountSlider.getValue());
    }

    /**
     * Set a MIDI CC value on the synth
     *
     * @param ccNumber The CC number to set
     * @param value    The value to set (0-127)
     */
    private void setControlChange(int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel midiCh = synthesizer.getChannels()[midiChannel];
                if (midiCh != null) {
                    midiCh.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                logger.error("Error setting CC {} on channel {}", ccNumber, midiChannel + 1, e);
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
     * @param tooltip      Tooltip text
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
     * @param tooltip      Tooltip text
     * @param min          Minimum value
     * @param max          Maximum value
     * @param initialValue Initial value
     * @param labels       Array of labels for tick marks
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

    // Getters and setters for individual control values

    public int getFilterType() {
        return filterTypeSlider.getValue();
    }

    public void setFilterType(int value) {
        filterTypeSlider.setValue(value);
    }

    public int getCutoff() {
        return cutoffSlider.getValue();
    }

    public void setCutoff(int value) {
        cutoffSlider.setValue(value);
    }

    public int getResonance() {
        return resonanceSlider.getValue();
    }

    public void setResonance(int value) {
        resonanceSlider.setValue(value);
    }

    public int getEnvAmount() {
        return envAmountSlider.getValue();
    }

    public void setEnvAmount(int value) {
        envAmountSlider.setValue(value);
    }

}