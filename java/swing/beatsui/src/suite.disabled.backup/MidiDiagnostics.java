package com.angrysurfer.beats.diagnostic.suite;

import javax.sound.midi.*;
import javax.swing.*;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;

import java.awt.*;

/**
 * Helper class for MIDI diagnostics
 */
public class MidiDiagnostics {
    
    private final JFrame parentFrame;
    
    /**
     * Constructor
     */
    public MidiDiagnostics(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }
    
    /**
     * Tests MIDI connections by listing all devices and their status
     */
    public DiagnosticLogBuilder testMidiConnections() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MIDI Connection Test");
        
        try {
            // Get available MIDI devices
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            log.addLine("Found " + infos.length + " MIDI devices:");
            
            int availableReceivers = 0;
            int availableTransmitters = 0;
            
            for (MidiDevice.Info info : infos) {
                try {
                    MidiDevice device = MidiSystem.getDevice(info);
                    boolean isReceiver = device.getMaxReceivers() != 0;
                    boolean isTransmitter = device.getMaxTransmitters() != 0;
                    
                    log.addLine(" - " + info.getName() + " (" + info.getDescription() + ")");
                    log.addIndentedLine("Vendor: " + info.getVendor() + ", Version: " + info.getVersion(), 1);
                    log.addIndentedLine("Receivers: " + device.getMaxReceivers() + 
                                     ", Transmitters: " + device.getMaxTransmitters(), 1);
                    
                    if (isReceiver) {
                        availableReceivers++;
                        try {
                            if (!device.isOpen()) {
                                device.open();
                            }
                            Receiver receiver = device.getReceiver();
                            if (receiver != null) {
                                log.addIndentedLine("Successfully obtained receiver", 1);
                                
                                // Send a test note to the device
                                ShortMessage msg = new ShortMessage();
                                msg.setMessage(ShortMessage.NOTE_ON, 9, 60, 100);
                                receiver.send(msg, -1);
                                
                                Thread.sleep(200);
                                
                                msg.setMessage(ShortMessage.NOTE_OFF, 9, 60, 0);
                                receiver.send(msg, -1);
                                
                                log.addIndentedLine("Sent test note to device", 1);
                                
                                receiver.close();
                            }
                            if (device.isOpen()) {
                                device.close();
                            }
                        } catch (Exception e) {
                            log.addIndentedLine("Error accessing receiver: " + e.getMessage(), 1);
                        }
                    }
                    
                    if (isTransmitter) {
                        availableTransmitters++;
                    }
                } catch (Exception e) {
                    log.addLine(" - Error accessing " + info.getName() + ": " + e.getMessage());
                }
            }
            
            log.addIndentedLine("Found " + availableReceivers + " devices with receivers", 1);
            log.addIndentedLine("Found " + availableTransmitters + " devices with transmitters", 1);
            
            if (availableReceivers == 0) {
                log.addError("No MIDI output devices with receivers found");
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Tests MIDI sound output by playing a sequence of notes
     */
    public DiagnosticLogBuilder testMidiSound() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MIDI Sound Test");
        
        try {
            log.addLine("=== MIDI Sound Test ===");
            
            // Use a single dialog that stays open throughout the test process
            final JDialog testDialog = new JDialog(parentFrame, "MIDI Sound Test", true);
            testDialog.setLayout(new BorderLayout());
            
            JLabel statusLabel = new JLabel("Ready to start MIDI sound test");
            statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            statusLabel.setHorizontalAlignment(JLabel.CENTER);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton nextButton = new JButton("Start Test");
            JButton cancelButton = new JButton("Cancel");
            
            buttonPanel.add(nextButton);
            buttonPanel.add(cancelButton);
            
            testDialog.add(statusLabel, BorderLayout.CENTER);
            testDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            testDialog.setSize(400, 200);
            testDialog.setLocationRelativeTo(parentFrame);
            
            // Set up a flag to track if the test should continue
            final boolean[] continueTest = {false};
            final boolean[] testCancelled = {false};
            
            // Set up button actions
            nextButton.addActionListener(e -> {
                continueTest[0] = true;
                testDialog.setVisible(false);
            });
            
            cancelButton.addActionListener(e -> {
                testCancelled[0] = true;
                testDialog.setVisible(false);
            });
            
            // Show initial dialog
            testDialog.setVisible(true);
            
            if (testCancelled[0]) {
                log.addLine("Test cancelled by user");
                return log;
            }
            
            // Find suitable MIDI device
            log.addLine("Opening synthesizer...");
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            log.addLine("Opened synthesizer: " + synth.getDeviceInfo().getName());
            
            // Play a major scale
            log.addLine("Playing major scale...");
            MidiChannel channel = synth.getChannels()[0];
            int[] notes = {60, 62, 64, 65, 67, 69, 71, 72};
            for (int note : notes) {
                channel.noteOn(note, 100);
                Thread.sleep(300);
                channel.noteOff(note);
                log.addIndentedLine("Played note: " + note, 1);
            }
            
            // Reset dialog for percussion test
            nextButton.setText("Continue to Percussion");
            cancelButton.setText("Stop Test");
            statusLabel.setText("<html>Did you hear the scale?<br>Click 'Continue' to test percussion sounds</html>");
            continueTest[0] = false;
            testDialog.setVisible(true);
            
            if (testCancelled[0]) {
                log.addLine("Test stopped after scale");
                synth.close();
                log.addLine("Synthesizer closed");
                return log;
            }
            
            // Test percussion (channel 9)
            log.addLine("Playing percussion sounds...");
            MidiChannel drumChannel = synth.getChannels()[9];
            int[] drumNotes = {35, 38, 42, 46, 49, 51};
            for (int note : drumNotes) {
                drumChannel.noteOn(note, 100);
                Thread.sleep(300);
                drumChannel.noteOff(note);
                log.addIndentedLine("Played drum note: " + note, 1);
            }
            
            synth.close();
            log.addLine("Synthesizer closed");
            
            // Final confirmation dialog
            nextButton.setText("All Sounds Good");
            JButton someIssuesButton = new JButton("Some Issues");
            JButton noSoundButton = new JButton("No Sound");
            
            buttonPanel.removeAll();
            buttonPanel.add(nextButton);
            buttonPanel.add(someIssuesButton);
            buttonPanel.add(noSoundButton);
            
            statusLabel.setText("<html>MIDI sound test completed.<br>How did it sound?</html>");
            
            // Set up result flags
            final int[] result = {0}; // 0=good, 1=issues, 2=no sound
            
            someIssuesButton.addActionListener(e -> {
                result[0] = 1;
                testDialog.setVisible(false);
            });
            
            noSoundButton.addActionListener(e -> {
                result[0] = 2;
                testDialog.setVisible(false);
            });
            
            testDialog.setVisible(true);
            
            // Process results
            switch (result[0]) {
                case 0:
                    log.addLine("Result: User confirmed all sounds played correctly");
                    break;
                case 1:
                    log.addLine("Result: User reported some issues with playback");
                    break;
                case 2:
                    log.addLine("Result: User reported no sound");
                    log.addError("MIDI sound test failed - No sound heard");
                    break;
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Test MIDI Synthesizer capabilities
     */
    public DiagnosticLogBuilder testSynthesizerCapabilities() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MIDI Synthesizer Capabilities");
        
        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            
            log.addLine("Default Synthesizer: " + synth.getDeviceInfo().getName());
            log.addLine("Vendor: " + synth.getDeviceInfo().getVendor());
            log.addLine("Description: " + synth.getDeviceInfo().getDescription());
            
            // List channels
            log.addSection("MIDI Channels");
            MidiChannel[] channels = synth.getChannels();
            log.addIndentedLine("Available channels: " + channels.length, 1);
            
            // List instruments
            log.addSection("Available Instruments");
            Instrument[] instruments = synth.getAvailableInstruments();
            log.addIndentedLine("Total available instruments: " + instruments.length, 1);
            
            // List a sample of instruments
            for (int i = 0; i < Math.min(instruments.length, 10); i++) {
                Instrument instrument = instruments[i];
                log.addIndentedLine("Instrument " + i + ": " + instrument.getName() + 
                                 " (Bank: " + instrument.getPatch().getBank() + 
                                 ", Program: " + instrument.getPatch().getProgram() + ")", 1);
            }
            
            // List loaded instruments
            Instrument[] loadedInstruments = synth.getLoadedInstruments();
            log.addIndentedLine("Loaded instruments: " + loadedInstruments.length, 1);
            
            // Available soundbank info
            log.addSection("Soundbank Information");
            Soundbank defaultSoundbank = synth.getDefaultSoundbank();
            if (defaultSoundbank != null) {
                log.addIndentedLine("Default Soundbank: " + defaultSoundbank.getName(), 1);
                log.addIndentedLine("Description: " + defaultSoundbank.getDescription(), 1);
                log.addIndentedLine("Vendor: " + defaultSoundbank.getVendor(), 1);
                log.addIndentedLine("Version: " + defaultSoundbank.getVersion(), 1);
                log.addIndentedLine("Total instruments: " + defaultSoundbank.getInstruments().length, 1);
            } else {
                log.addIndentedLine("No default soundbank available", 1);
            }
            
            // Latency info
            log.addSection("Latency Information");
            if (synth instanceof Synthesizer) {
                log.addIndentedLine("Synth latency: " + synth.getLatency() + " microseconds", 1);
                log.addIndentedLine("Max polyphony: " + synth.getMaxPolyphony(), 1);
            }
            
            synth.close();
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
}
