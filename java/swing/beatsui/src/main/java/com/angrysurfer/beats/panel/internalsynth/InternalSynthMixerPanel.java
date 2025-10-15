package com.angrysurfer.beats.panel.internalsynth;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;

import lombok.Getter;
import lombok.Setter;

/**
 * Panel for controlling oscillator mixing parameters of a synthesizer
 */
@Getter
@Setter

public class InternalSynthMixerPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthMixerPanel.class);
    
    // Mixer control constants
    public static final int CC_BALANCE_1_2 = 8;
    public static final int CC_BALANCE_3 = 9;
    public static final int CC_MASTER_VOLUME = 7;
    
    private final Synthesizer synthesizer;
    private int midiChannel;
    
    // UI components
    private Dial balance12Dial;
    private Dial balance3Dial;
    private Dial masterDial;
    
    /**
     * Create a new Mixer control panel
     * 
     * @param synthesizer The MIDI synthesizer to control
     * @param midiChannel The MIDI channel to send control changes to
     */
    public InternalSynthMixerPanel(Synthesizer synthesizer, int midiChannel) {
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
                "Oscillator Mix",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));
        
        // Use horizontal FlowLayout with left alignment and minimal vertical gap
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
        
        // Set preferred dimensions - wider, less tall for horizontal layout
        setPreferredSize(new Dimension(350, 100));

        // Balance between osc 1 & 2
        JPanel balance12Group = createDialGroup("Osc 1-2");
        balance12Dial = createStandardDial("Balance between Oscillators 1 & 2", 64);
        balance12Group.add(balance12Dial);

        // Balance between result and osc 3
        JPanel balance3Group = createDialGroup("Osc 3 Mix");
        balance3Dial = createStandardDial("Mix in Oscillator 3", 32);
        balance3Group.add(balance3Dial);

        // Master level
        JPanel masterGroup = createDialGroup("Master");
        masterDial = createStandardDial("Master Volume Level", 100);
        masterGroup.add(masterDial);

        // Add components in horizontal layout
        add(balance12Group);
        add(balance3Group);
        add(masterGroup);
    }
    
    /**
     * Add event listeners to all controls
     */
    private void setupEventHandlers() {
        balance12Dial.addChangeListener(e -> 
            setControlChange(CC_BALANCE_1_2, balance12Dial.getValue()));
            
        balance3Dial.addChangeListener(e -> 
            setControlChange(CC_BALANCE_3, balance3Dial.getValue()));
            
        masterDial.addChangeListener(e -> 
            setControlChange(CC_MASTER_VOLUME, masterDial.getValue()));
    }
    
    /**
     * Reset all controls to their default values
     */
    public void resetToDefaults() {
        balance12Dial.setValue(64); // Center balance between osc 1-2
        balance3Dial.setValue(32);  // Mix in some of oscillator 3
        masterDial.setValue(100);   // Full master volume
        
        // Send these values to the synth
        updateSynthState();
    }
    
    /**
     * Send the current state of all controls to the synthesizer
     */
    public void updateSynthState() {
        setControlChange(CC_BALANCE_1_2, balance12Dial.getValue());
        setControlChange(CC_BALANCE_3, balance3Dial.getValue());
        setControlChange(CC_MASTER_VOLUME, masterDial.getValue());
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
     * Create a dial group with a label
     */
    private JPanel createDialGroup(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Add title label
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add components vertically
        panel.add(Box.createVerticalStrut(5));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        
        return panel;
    }

    /**
     * Create a dial with consistent styling
     * 
     * @param label The dial label
     * @param tooltip Tooltip text
     * @param initialValue Initial value (0-127)
     * @return Configured dial
     */
    private Dial createStandardDial(String tooltip, int initialValue) {
        return UIHelper.createStandardDial(tooltip, initialValue);
    }
    
    // Getters and setters for individual control values
    
    public int getBalance12() {
        return balance12Dial.getValue();
    }
    
    public void setBalance12(int value) {
        balance12Dial.setValue(value);
    }
    
    public int getBalance3() {
        return balance3Dial.getValue();
    }
    
    public void setBalance3(int value) {
        balance3Dial.setValue(value);
    }
    
    public int getMasterVolume() {
        return masterDial.getValue();
    }
    
    public void setMasterVolume(int value) {
        masterDial.setValue(value);
    }
}