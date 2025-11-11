package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import java.util.List;

/**
 * Diagnostic helper for DeviceManager
 */
public class DeviceManagerDiagnostics {

    /**
     * Run diagnostics on the DeviceManager
     */
    public DiagnosticLogBuilder testDeviceManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("DeviceManager Diagnostics");

        try {
            DeviceManager manager = MidiService.getInstance();
            log.addLine("DeviceManager instance obtained: " + (manager != null));

            // Test device enumeration
            log.addSection("Available MIDI Devices");
            List<String> deviceNames = manager.getOutputDeviceNames();

            if (deviceNames.isEmpty()) {
                log.addWarning("No MIDI output devices found");
            } else {
                log.addLine("Found " + deviceNames.size() + " output devices:");
                for (String name : deviceNames) {
                    log.addIndentedLine(name, 1);
                }
            }

            // Test device acquisition
            log.addSection("Device Acquisition Test");

            if (!deviceNames.isEmpty()) {
                for (String deviceName : deviceNames) {
                    log.addIndentedLine("Testing device: " + deviceName, 1);

                    // Try to acquire the device
                    MidiDevice device = manager.acquireDevice(deviceName);
                    boolean acquired = (device != null);
                    log.addIndentedLine("Acquired: " + acquired, 2);

                    if (acquired) {
                        log.addIndentedLine("Device open: " + device.isOpen(), 2);
                        log.addIndentedLine("Max receivers: " + device.getMaxReceivers(), 2);
                        log.addIndentedLine("Max transmitters: " + device.getMaxTransmitters(), 2);

                        // Release the device
                        manager.releaseDevice(deviceName);
                        log.addIndentedLine("Device released", 2);
                    } else {
                        log.addWarning("Failed to acquire device: " + deviceName);
                    }
                }
            }

            // Test static methods
            log.addSection("Static Method Tests");

            try {
                // Test getMidiOutDevices
                List<MidiDevice> outDevices = DeviceManager.getMidiOutDevices();
                log.addLine("getMidiOutDevices found " + outDevices.size() + " devices");

                // Test device selection
                if (!outDevices.isEmpty()) {
                    MidiDevice testDevice = outDevices.get(0);
                    String deviceName = testDevice.getDeviceInfo().getName();
                    log.addLine("Testing select() with: " + deviceName);

                    boolean selected = DeviceManager.select(testDevice);
                    log.addLine("Device selected: " + selected);

                    if (selected && testDevice.isOpen()) {
                        testDevice.close();
                        log.addLine("Device closed after test");
                    }
                }

                // Test cleanup
                log.addLine("Testing cleanupMidiDevices()");
                DeviceManager.cleanupMidiDevices();
                log.addLine("Cleanup completed");

                // Test if Gervill is available
                MidiDevice gervill = DeviceManager.getDevice(SequencerConstants.GERVILL);
                log.addLine("Gervill availability: " + (gervill != null));
                if (gervill != null && gervill.isOpen()) {
                    gervill.close();
                }

            } catch (Exception e) {
                log.addWarning("Error during static method tests: " + e.getMessage());
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Test device selection for a specific device
     */
    public DiagnosticLogBuilder testSpecificDevice(String deviceName) {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Device Test: " + deviceName);

        try {
            log.addLine("Testing device: " + deviceName);

            // Try to get the device
            MidiDevice device = DeviceManager.getDevice(deviceName);

            if (device == null) {
                log.addError("Device not found: " + deviceName);
                return log;
            }

            log.addIndentedLine("Device found", 1);
            log.addIndentedLine("Description: " + device.getDeviceInfo().getDescription(), 1);
            log.addIndentedLine("Vendor: " + device.getDeviceInfo().getVendor(), 1);
            log.addIndentedLine("Version: " + device.getDeviceInfo().getVersion(), 1);

            // Try to open
            boolean wasOpen = device.isOpen();
            if (!wasOpen) {
                try {
                    device.open();
                    log.addIndentedLine("Successfully opened device", 1);
                } catch (MidiUnavailableException e) {
                    log.addError("Failed to open device: " + e.getMessage());
                }
            } else {
                log.addIndentedLine("Device was already open", 1);
            }

            // Check receiver capability
            log.addIndentedLine("Max receivers: " + device.getMaxReceivers(), 1);

            try {
                if (device.getMaxReceivers() != 0) {
                    device.getReceiver();
                    log.addIndentedLine("Successfully got receiver", 1);
                }
            } catch (MidiUnavailableException e) {
                log.addError("Failed to get receiver: " + e.getMessage());
            }

            // Clean up
            if (!wasOpen && device.isOpen()) {
                device.close();
                log.addIndentedLine("Closed device after testing", 1);
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }
}
