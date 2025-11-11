package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.MidiService;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic helper for ReceiverManager
 */
public class ReceiverManagerDiagnostics {

    /**
     * Run diagnostics on the ReceiverManager
     */
    public DiagnosticLogBuilder testReceiverManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("ReceiverManager Diagnostics");

        try {
            ReceiverManager manager = MidiService.getInstance();
            log.addLine("ReceiverManager instance obtained: " + (manager != null));

            // Get available devices to test with
            DeviceManager deviceManager = MidiService.getInstance();
            List<String> deviceNames = deviceManager.getOutputDeviceNames();

            if (deviceNames.isEmpty()) {
                log.addWarning("No MIDI output devices found, limited testing possible");
            } else {
                log.addLine("Found " + deviceNames.size() + " devices for testing");

                // Test receiver creation for each device
                log.addSection("Receiver Creation Tests");

                List<String> successDevices = new ArrayList<>();
                List<String> failedDevices = new ArrayList<>();

                for (String deviceName : deviceNames) {
                    log.addIndentedLine("Testing device: " + deviceName, 1);

                    // Get the device first
                    MidiDevice device = DeviceManager.getDevice(deviceName);

                    if (device != null) {
                        // Test getting a receiver
                        Receiver receiver = manager.getOrCreateReceiver(deviceName, device);

                        if (receiver != null) {
                            log.addIndentedLine("Successfully got receiver", 2);
                            successDevices.add(deviceName);

                            // Test sending a message
                            try {
                                ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 0);
                                receiver.send(msg, -1);
                                log.addIndentedLine("Successfully sent test message", 2);
                            } catch (Exception e) {
                                log.addWarning("Failed to send test message: " + e.getMessage());
                            }

                            // Close the receiver through the manager
                            manager.closeReceiver(deviceName);
                            log.addIndentedLine("Closed receiver", 2);
                        } else {
                            log.addWarning("Failed to get receiver for device: " + deviceName);
                            failedDevices.add(deviceName);
                        }
                    } else {
                        log.addWarning("Failed to get device: " + deviceName);
                        failedDevices.add(deviceName);
                    }
                }

                // Summarize results
                log.addSection("Summary");
                log.addLine("Successfully tested receivers for " + successDevices.size() + " devices");
                log.addLine("Failed to test receivers for " + failedDevices.size() + " devices");

                if (!failedDevices.isEmpty()) {
                    log.addIndentedLine("Failed devices:", 1);
                    for (String name : failedDevices) {
                        log.addIndentedLine(name, 2);
                    }
                }
            }

            // Test Gervill specifically
            log.addSection("Gervill Synth Test");
            testGervillReceiver(log, manager);

            // Test receiver caching
            log.addSection("Receiver Caching Test");
            if (!deviceNames.isEmpty()) {
                String testDevice = deviceNames.get(0);
                log.addLine("Testing caching with device: " + testDevice);

                // Get a device
                MidiDevice device = DeviceManager.getDevice(testDevice);

                if (device != null) {
                    // Get a receiver twice and check if they're the same
                    Receiver receiver1 = manager.getOrCreateReceiver(testDevice, device);
                    Receiver receiver2 = manager.getOrCreateReceiver(testDevice, device);

                    boolean same = (receiver1 == receiver2);
                    log.addLine("Cached receiver test - same instance: " + same);

                    if (!same) {
                        log.addWarning("Receiver caching may not be working properly");
                    }

                    // Clean up
                    manager.closeReceiver(testDevice);
                }
            }

            // Test error recovery
            log.addSection("Error Recovery Test");
            log.addLine("Testing with non-existent device");

            // Try with a non-existent device
            Receiver receiver = manager.getOrCreateReceiver("NonExistentDevice", null);
            log.addLine("Result for non-existent device: " + (receiver == null ? "Null (correct)" : "Not null (unexpected)"));

            if (receiver != null) {
                log.addWarning("ReceiverManager returned a receiver for a non-existent device");
                manager.closeReceiver("NonExistentDevice");
            }

            // Test cleanup
            log.addSection("Cleanup Test");
            manager.clearAllReceivers();
            log.addLine("clearAllReceivers() called");

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Test Gervill receiver specifically
     */
    private void testGervillReceiver(DiagnosticLogBuilder log, MidiService manager) {
        try {
            log.addLine("Testing Gervill synthesizer");

            MidiDevice gervill = DeviceManager.getDevice(SequencerConstants.GERVILL);
            if (gervill == null) {
                log.addWarning("Gervill synthesizer not found");
                return;
            }

            log.addIndentedLine("Gervill device found", 1);

            // Get receiver
            Receiver receiver = manager.getOrCreateReceiver(SequencerConstants.GERVILL, gervill);

            if (receiver == null) {
                log.addWarning("Failed to get receiver for Gervill");
                return;
            }

            log.addIndentedLine("Successfully got Gervill receiver", 1);

            // Send a test message
            try {
                // Create a middle C note-on message
                ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 64);
                receiver.send(msg, -1);
                log.addIndentedLine("Sent Note-On message", 1);

                // Wait briefly
                Thread.sleep(500);

                // Send note-off
                ShortMessage offMsg = new ShortMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
                receiver.send(offMsg, -1);
                log.addIndentedLine("Sent Note-Off message", 1);

                log.addIndentedLine("Gervill test completed successfully", 1);
            } catch (Exception e) {
                log.addWarning("Failed to send test message to Gervill: " + e.getMessage());
            } finally {
                // Close the receiver
                manager.closeReceiver(SequencerConstants.GERVILL);
            }

        } catch (Exception e) {
            log.addWarning("Error in Gervill test: " + e.getMessage());
        }
    }

    /**
     * Test receiver reliability under load
     */
    public DiagnosticLogBuilder testReceiverReliability() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Receiver Reliability Test");

        try {
            ReceiverManager manager = MidiService.getInstance();
            log.addLine("Testing receiver reliability under load");

            // Get Gervill as test device
            MidiDevice gervill = DeviceManager.getDevice(SequencerConstants.GERVILL);

            if (gervill == null) {
                log.addWarning("Gervill synthesizer not found, using system receiver");

                // Use the system receiver instead
                try {
                    // Create a test receiver that counts messages
                    final int[] messageCount = {0};

                    Receiver testReceiver = new Receiver() {
                        @Override
                        public void send(MidiMessage message, long timeStamp) {
                            messageCount[0]++;
                        }

                        @Override
                        public void close() {
                        }
                    };

                    // Run stress test
                    log.addLine("Running message stress test with test receiver");
                    boolean testResult = stressTestReceiver(testReceiver, 1000, log);

                    log.addLine("Stress test result: " + (testResult ? "PASS" : "FAIL"));
                    log.addLine("Messages processed: " + messageCount[0]);

                } catch (Exception e) {
                    log.addWarning("Failed to create test receiver: " + e.getMessage());
                }

                return log;
            }

            // Get the receiver from Gervill
            Receiver receiver = manager.getOrCreateReceiver(SequencerConstants.GERVILL, gervill);

            if (receiver == null) {
                log.addError("Failed to get Gervill receiver");
                return log;
            }

            log.addLine("Got Gervill receiver for stress test");

            // Run the stress test
            boolean testResult = stressTestReceiver(receiver, 1000, log);

            log.addLine("Stress test result: " + (testResult ? "PASS" : "FAIL"));

            // Close the receiver
            manager.closeReceiver(SequencerConstants.GERVILL);

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Send many messages to a receiver to test reliability
     */
    private boolean stressTestReceiver(Receiver receiver, int messageCount, DiagnosticLogBuilder log) {
        try {
            log.addLine("Sending " + messageCount + " MIDI messages...");

            long startTime = System.currentTimeMillis();
            int errorCount = 0;

            for (int i = 0; i < messageCount; i++) {
                try {
                    // Alternate between different message types
                    ShortMessage msg;

                    if (i % 3 == 0) {
                        // Note on
                        msg = new ShortMessage(ShortMessage.NOTE_ON, 0, 60 + (i % 12), 64);
                    } else if (i % 3 == 1) {
                        // Note off
                        msg = new ShortMessage(ShortMessage.NOTE_OFF, 0, 60 + (i % 12), 0);
                    } else {
                        // Control change
                        msg = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, i % 127, i % 127);
                    }

                    receiver.send(msg, -1);

                    // Brief pause every 100 messages to not overwhelm the system
                    if (i % 100 == 0) {
                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    errorCount++;

                    // Log only the first few errors to avoid flooding
                    if (errorCount <= 3) {
                        log.addWarning("Error sending message #" + i + ": " + e.getMessage());
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.addLine("Stress test completed in " + duration + " ms");
            log.addLine("Messages with errors: " + errorCount + " (" +
                    (errorCount * 100.0 / messageCount) + "%)");

            return errorCount == 0;
        } catch (Exception e) {
            log.addWarning("Error during stress test: " + e.getMessage());
            return false;
        }
    }
}
