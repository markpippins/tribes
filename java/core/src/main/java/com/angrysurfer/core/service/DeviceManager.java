package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.exception.MidiDeviceException;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.sequencer.SequencerConstants;

public class DeviceManager implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
    private static final Map<String, MidiDevice> deviceCache = new ConcurrentHashMap<>();
    // Add this static field to disable excessive validation
    private static final boolean disableExcessiveValidation = true;
    private static DeviceManager instance;
    private final List<MidiDevice> availableOutputDevices = new ArrayList<>();

    private final Map<String, MidiDevice> activeDevices = new ConcurrentHashMap<>();

    // Private constructor for singleton
    private DeviceManager() {
        // Keep constructor lightweight. Call initialize() during startup to
        // populate device lists and register for events.
    }

    // Singleton accessor
    public static synchronized DeviceManager getInstance() {
        if (instance == null) {
            instance = new DeviceManager();
        }
        return instance;
    }

    // Existing static methods can remain for backwards compatibility
    public static void cleanupMidiDevices() {
        if (disableExcessiveValidation) {
            return; // Skip the expensive validation during playback
        }
        logger.info("cleanupMidiDevices()");
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.isOpen()) {
                    device.getReceivers().forEach(Receiver::close);
                    device.close();
                }
            } catch (MidiUnavailableException e) {
                logger.warn("Error during cleanup of device: " + info.getName(), e);
            }
        }
    }

    // Get a specific MIDI device by name
    public static MidiDevice getMidiDevice(String name) {
        // Use device cache instead
        MidiDevice cachedDevice = deviceCache.get(name);
        if (cachedDevice != null && cachedDevice.isOpen()) {
            return cachedDevice;
        }

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            if (info.getName().contains(name)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    deviceCache.put(name, device);
                    return device;
                } catch (MidiUnavailableException e) {
                    logger.debug("Error accessing MIDI device: {}", name); // Reduced logging severity
                }
            }
        }
        return null;
    }

    // Get all MIDI output devices
    public static List<MidiDevice> getMidiOutDevices() {
        logger.info("getMidiOutDevices()");
        List<MidiDevice> devices = new ArrayList<>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                // Only add devices that support output (MaxReceivers > 0 or unlimited (-1))
                if (device.getMaxReceivers() != 0) {
                    devices.add(device);
                    logger.info("Found MIDI output device: {} (Receivers: {})",
                            info.getName(),
                            device.getMaxReceivers());
                }
            } catch (MidiUnavailableException e) {
                logger.error("Error accessing MIDI device: " + info.getName(), e);
            }
        }
        return devices;
    }

    // Improved error handling and validation
    public static List<MidiDevice.Info> getMidiDeviceInfos() {
        logger.info("getMidiDeviceInfos()");
        try {
            return Arrays.stream(MidiSystem.getMidiDeviceInfo())
                    .filter(Objects::nonNull)
                    .map(info -> {
                        return info;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            throw new MidiDeviceException("Failed to get MIDI device infos", ex);
        }
    }

    public static int midiChannel(int realChannel) {
        return realChannel - 1;
    }

    // Better error handling for reset
    public static void reset() {
        logger.info("reset()");
        try {
            var sequencer = MidiSystem.getSequencer();
            if (sequencer != null) {
                sequencer.getReceivers().forEach(Receiver::close);
            }
        } catch (MidiUnavailableException ex) {
            logger.warn("Failed to reset MIDI system", ex);
            // Don't throw - this is cleanup code
        }
    }

    // Improved resource management
    public static boolean select(MidiDevice device) {
        if (device == null)
            throw new IllegalArgumentException("Device cannot be null");

        logger.info("select({})", device.getDeviceInfo().getName());

        try {
            if (!device.isOpen()) {
                device.open();
            }
            return device.isOpen();
        } catch (MidiUnavailableException ex) {
            throw new MidiDeviceException("Failed to select MIDI device", ex);
        }
    }

    public static boolean select(String name) throws MidiUnavailableException {
        logger.info("select() - name: {}", name);
        logger.info("select({})", name);
        MidiDevice device = getMidiDevice(name);
        return device != null && select(device);
    }

    // Improved message sending with validation
    @SuppressWarnings("unused")
    public static void sendMessage(InstrumentWrapper instrument, int channel, int messageType, int data1, int data2) {
        logger.info("sendMessage() - instrument: {}, channel: {}, messageType: {}, data1: {}, data2: {}",
                instrument.getName(), channel, messageType, data1, data2);

        if (Objects.isNull(instrument)) {
            logger.warn("Instrument cannot be null");
            return;
        }

        try {
            ShortMessage message = new ShortMessage(messageType,
                    channel,
                    validateData(data1),
                    validateData(data2));
            instrument.sendMessage(message);
        } catch (InvalidMidiDataException ex) {
            throw new MidiDeviceException("Failed to send MIDI message", ex);
        }

    }

    static int validateData(int data) {
        if (data < 0 || data > 126) {
            throw new IllegalArgumentException("MIDI data must be between 0 and 126");
        }
        return data;
    }

    // CommandListener implementation
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        switch (action.getCommand()) {
            case Commands.REFRESH_MIDI_DEVICES -> refreshDeviceList();
        }
    }

    // Update the device list
    public void refreshDeviceList() {
        logger.info("Refreshing MIDI device list");
        availableOutputDevices.clear();
        availableOutputDevices.addAll(getMidiOutDevices());
    }

    /**
     * Explicit initialization for DeviceManager. Populates the device list and
+     * registers for command events. Call during application startup.
     */
    public synchronized void initialize() {
        refreshDeviceList();
        CommandBus.getInstance().register(this, new String[]{Commands.REFRESH_MIDI_DEVICES});
    }

    // Add proper cleanup
    // @Override
    // public void finalize() {
    // reset();
    // }

    // Get names of available output devices
    public List<String> getAvailableOutputDeviceNames() {
        if (availableOutputDevices.isEmpty()) {
            refreshDeviceList();
        }

        return availableOutputDevices.stream()
                .map(device -> device.getDeviceInfo().getName())
                .collect(Collectors.toList());
    }

    /**
     * Get all available MIDI output devices
     *
     * @return List of available MIDI output devices
     * @throws MidiUnavailableException if the MIDI system is unavailable
     */
    public List<MidiDevice> getAvailableOutputDevices() throws MidiUnavailableException {
        List<MidiDevice> outputDevices = new ArrayList<>();

        // Get all MIDI device info objects
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        // Log found devices
        logger.debug("Found {} MIDI devices", infos.length);

        // Try to open each device and check if it's an output device
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);

                // Check if device has transmitter ports (is an output device)
                if (device.getMaxReceivers() != 0) {
                    // This is an output device
                    outputDevices.add(device);
                    logger.debug("Found output device: {}", info.getName());
                }
            } catch (MidiUnavailableException e) {
                logger.warn("Could not open MIDI device {}: {}",
                        info.getName(), e.getMessage());
                // Continue to next device
            }
        }

        // Log summary
        logger.info("Found {} available output devices", outputDevices.size());

        return outputDevices;
    }

    // Controlled acquisition and release of devices
    public synchronized MidiDevice acquireDevice(String name) {
        if (activeDevices.containsKey(name)) {
            return activeDevices.get(name);
        }

        MidiDevice device = getMidiDevice(name);
        if (device != null) {
            try {
                if (!device.isOpen()) {
                    device.open();
                }
                activeDevices.put(name, device);
                return device;
            } catch (MidiUnavailableException e) {
                logger.error("Failed to acquire device: {}", name, e);
            }
        }
        return null;
    }

    public synchronized void releaseDevice(String name) {
        if (activeDevices.containsKey(name)) {
            MidiDevice device = activeDevices.get(name);
            if (device != null && device.isOpen()) {
                device.close();
            }
            activeDevices.remove(name);
        }
    }

    /**
     * Try to ensure the Gervill synthesizer is available
     *
     * @return true if Gervill is available and open
     */
    public boolean ensureGervillAvailable() {
        logger.info("Ensuring Gervill synthesizer is available");
        try {
            MidiDevice gervill = getMidiDevice(SequencerConstants.GERVILL);
            if (gervill == null) {
                // Try to create the Gervill synthesizer
                try {
                    MidiSystem.getSynthesizer().open();
                    gervill = getMidiDevice(SequencerConstants.GERVILL);
                    logger.info("Initialized Gervill synthesizer");
                } catch (Exception e) {
                    logger.error("Failed to initialize Gervill synthesizer: {}", e.getMessage());
                    return false;
                }
            }

            if (gervill != null && !gervill.isOpen()) {
                try {
                    gervill.open();
                    logger.info("Opened Gervill synthesizer");
                } catch (Exception e) {
                    logger.error("Failed to open Gervill synthesizer: {}", e.getMessage());
                    return false;
                }
            }

            return gervill != null && gervill.isOpen();
        } catch (Exception e) {
            logger.error("Error ensuring Gervill is available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets a default MIDI output device, with preference for Gervill.
     *
     * @return A MIDI device, or null if none available
     */
    public MidiDevice getDefaultOutputDevice() {
        try {
            // First, try to get Gervill
            MidiDevice device = getMidiDevice(SequencerConstants.GERVILL);
            if (device != null) {
                if (!device.isOpen()) {
                    try {
                        device.open();
                    } catch (Exception e) {
                        logger.warn("Could not open Gervill: {}", e.getMessage());
                    }
                }
                if (device.isOpen()) {
                    return device;
                }
            }

            // If Gervill not available, try any other device
            List<String> devices = getAvailableOutputDeviceNames();
            for (String name : devices) {
                try {
                    device = getMidiDevice(name);
                    if (device != null &&
                            device.getMaxReceivers() != 0 &&
                            !name.contains("Real Time Sequencer")) {

                        if (!device.isOpen()) {
                            device.open();
                        }

                        if (device.isOpen()) {
                            logger.info("Using {} as default MIDI device", name);
                            return device;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Failed to open device {}: {}", name, e.getMessage());
                }
            }

            // Last resort - try to get the Java Synthesizer
            try {
                Synthesizer synth = MidiSystem.getSynthesizer();
                if (!synth.isOpen()) {
                    synth.open();
                }
                logger.info("Using Java Synthesizer as fallback device");
                return synth;
            } catch (Exception e) {
                logger.error("Could not open Java Synthesizer: {}", e.getMessage());
            }

            return null;
        } catch (Exception e) {
            logger.error("Error getting default output device: {}", e.getMessage());
            return null;
        }
    }
}