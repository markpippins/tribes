package com.angrysurfer.core.model;

import com.angrysurfer.core.api.midi.MidiControlMessageEnum;
import com.angrysurfer.core.model.feature.Pad;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.util.IntegerArrayConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public final class InstrumentWrapper implements Serializable {

    public static final Integer DEFAULT_CHANNEL = 0;
    public static final Integer[] DEFAULT_CHANNELS = new Integer[]{DEFAULT_CHANNEL};
    public static final Integer[] ALL_CHANNELS = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    static final Random rand = new Random();
    static Logger logger = LoggerFactory.getLogger(InstrumentWrapper.class.getCanonicalName());
    private static ScheduledExecutorService NOTE_OFF_SCHEDULER;
    // Cached ShortMessages for better performance
    @JsonIgnore
    private final ShortMessage cachedNoteOn = new ShortMessage();
    @JsonIgnore
    private final ShortMessage cachedNoteOff = new ShortMessage();
    @JsonIgnore
    private final ShortMessage cachedControlChange = new ShortMessage();
    @JsonIgnore
    private final Map<String, Object> properties = new HashMap<>();
    private Long id;
    @JsonIgnore
    private transient MidiDevice.Info deviceInfo = null;
    private Boolean internal;
    private int bankMSB = 0; // Bank MSB (CC 0)
    private int bankLSB = 0; // Bank LSB (CC 32)
    private Boolean assignedToPlayer = false;
    private Boolean isDefault = false;
    private List<ControlCode> controlCodes = new ArrayList<>();
    private Set<Pad> pads = new HashSet<>();
    @JsonIgnore
    private transient Map<Integer, String> assignments = new HashMap<>();
    @JsonIgnore
    private Map<Integer, Integer[]> boundaries = new HashMap<>();
    @JsonIgnore
    private Map<Integer, Map<Long, String>> captions = new HashMap<>();
    // Primary change: Receiver becomes the primary MIDI output mechanism
    @JsonIgnore
    private Receiver receiver;  // Direct reference for faster access
    // Keep device as a backup and for metadata
    @JsonIgnore
    private MidiDevice device;
    @Column(name = "name", unique = true)
    private String name;
    private String deviceName = SequencerConstants.GERVILL;
    @JsonIgnore
    private int defaultChannel = 0;
    private boolean internalSynth;
    private String description;
    @Convert(converter = IntegerArrayConverter.class)
    private Integer[] receivedChannels = ALL_CHANNELS;
    private Integer channel;
    private Integer lowestNote = 0;
    private Integer highestNote = 127;
    private Integer highestPreset;
    private Integer preferredPreset;
    private Boolean hasAssignments;
    private String playerClassName;
    private Boolean available = true;
    private Set<Pattern> patterns;
    private Integer preset = 1;
    private String soundbankName;
    private Integer bankIndex;
    // Add flag to track initialization state
    private boolean initialized = false;

    /**
     * Default constructor with safe boolean initialization
     */
    public InstrumentWrapper() {
        // Initialize boolean fields to prevent NPE
        this.internal = Boolean.FALSE;
        this.available = Boolean.FALSE;
        this.initialized = Boolean.FALSE;
    }

    /**
     * Constructor with just a name and Receiver
     */
    public InstrumentWrapper(String name, Receiver receiver) {
        this.name = name;
        this.receiver = receiver;
        this.internal = (receiver == null);
        this.channel = DEFAULT_CHANNEL;
        this.deviceName = "External";
    }

    /**
     * Constructor for creating an instrument wrapper
     */
    public InstrumentWrapper(String name, MidiDevice device, int channel) {
        this.name = name;
        this.device = device;
        this.channel = channel;

        // Safely get device info if device is not null
        if (device != null) {
            this.deviceInfo = device.getDeviceInfo();
            this.deviceName = deviceInfo.getName();

            // Try to get a receiver from the device
            try {
                if (!device.isOpen()) {
                    device.open();
                }
                this.receiver = device.getReceiver();
            } catch (MidiUnavailableException e) {
                logger.warn("Could not get receiver from device: {}", e.getMessage());
            }
        } else {
            // Set default values for null device
            this.deviceName = "Internal";
        }

        // Set default values
        this.internal = (device == null); // Assume internal if device is null
    }

    public InstrumentWrapper(String name, MidiDevice device, Integer[] channels) {
        setName(Objects.isNull(name) ? device.getDeviceInfo().getName() : name);
        setDevice(device);
        setDeviceName(device.getDeviceInfo().getName());
        setReceivedChannels(channels);

        // Try to get a receiver from the device
        try {
            if (device != null && !device.isOpen()) {
                device.open();
            }
            if (device != null) {
                this.receiver = device.getReceiver();
            }
        } catch (MidiUnavailableException e) {
            logger.warn("Could not get receiver from device: {}", e.getMessage());
        }

        logger.info("Created instrument {} with channels: {}", getName(), Arrays.toString(channels));
    }

    /**
     * Shutdown the scheduler when needed
     */
    public static void shutdownScheduler() {
        if (NOTE_OFF_SCHEDULER != null) {
            NOTE_OFF_SCHEDULER.shutdown();
            try {
                if (!NOTE_OFF_SCHEDULER.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    NOTE_OFF_SCHEDULER.shutdownNow();
                }
            } catch (InterruptedException e) {
                NOTE_OFF_SCHEDULER.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setName(String name) {
        this.name = name;
        if (name != null) {
            if (name.toLowerCase().contains(SequencerConstants.GERVILL) ||
                    name.toLowerCase().contains("synth") ||
                    name.toLowerCase().contains("drum")) {
                this.internal = true;
            }
        }
    }

    @JsonIgnore
    public boolean isMultiTimbral() {
        return receivedChannels != null && receivedChannels.length > 1;
    }

    /**
     * Check if this instrument receives on the specified channel
     */
    @JsonIgnore
    public boolean receivesOn(int channel) {
        // Handle null receivedChannels
        if (receivedChannels == null) {
            // If no channels specified, check the main channel
            return this.channel != null && this.channel == channel;
        }

        // Check if the instrument receives on multiple channels
        try {
            return Arrays.asList(receivedChannels).contains(channel) ||
                    (this.channel != null && this.channel == channel);
        } catch (NullPointerException e) {
            // Fallback to main channel if there's an issue with receivedChannels
            return this.channel != null && this.channel == channel;
        }
    }

    // Convenience method for single-channel devices
    @JsonIgnore
    public int getDefaultChannel() {
        return receivedChannels != null && receivedChannels.length > 0 ? receivedChannels[0] : DEFAULT_CHANNEL;
    }

    public String assignedControl(int cc) {
        return assignments.getOrDefault(cc, "NONE");
    }

    /**
     * Send MIDI message directly to the receiver
     *
     * @param message The MIDI message to send
     */
    public void sendMessage(MidiMessage message) {
        try {
            // First try the direct receiver
//            if (receiver != null) {
//                receiver.send(message, -1);
//                return;
//            }

            // If receiver is null but we have a device, try to get a receiver
            if (device != null) {
                ensureDeviceOpen();
                Receiver deviceReceiver = device.getReceiver();
                if (deviceReceiver != null) {
                    deviceReceiver.send(message, -1);
                    return;
                }
            }

            if (deviceName != null) {
                Receiver managedReceiver = MidiService.getInstance().getReceiver(deviceName);
                if (managedReceiver != null) {
                    managedReceiver.send(message, -1);
                    this.receiver = managedReceiver;
                    return;
                }
            }

            // If we get here, we couldn't send the message
            logger.warn("Could not send MIDI message - no receiver available");
        } catch (Exception e) {
            logger.error("Error sending MIDI message: {}", e.getMessage());

            // Try recovery if needed
            tryRecoverReceiver();
        }
    }

    /**
     * Try to recover a working receiver if our current one has failed
     */
    private void tryRecoverReceiver() {
        try {
            // Try to get a new receiver from the device
            if (device != null) {
                ensureDeviceOpen();
                this.receiver = device.getReceiver();
                logger.info("Recovered receiver from device");
                return;
            }

            if (deviceName != null) {
                this.receiver = MidiService.getInstance().getReceiver(deviceName);
                logger.info("Recovered receiver from MidiService");
            }
        } catch (Exception e) {
            logger.error("Failed to recover receiver: {}", e.getMessage());
        }
    }

    /**
     * Ensure device is open if it exists
     */
    private void ensureDeviceOpen() {
        if (device != null && !device.isOpen()) {
            try {
                device.open();
            } catch (MidiUnavailableException e) {
                logger.error("Failed to open device: {}", e.getMessage());
            }
        }
    }

    public void channelPressure(int data1, int data2) {
        try {
            ShortMessage message = new ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, data1, data2);
            sendMessage(message);
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for channel pressure: {}", e.getMessage());
        }
    }

    public void controlChange(int controller, int value) {
        try {
            cachedControlChange.setMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value);
            sendMessage(cachedControlChange);
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for control change: {}", e.getMessage());
        }
    }

    public void noteOn(int note, int velocity) {
        try {
            synchronized (cachedNoteOn) {
                cachedNoteOn.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
                sendMessage(cachedNoteOn);
            }
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for note on: {}", e.getMessage());
        }
    }

    public void noteOff(int note, int velocity) {
        try {
            synchronized (cachedNoteOff) {
                cachedNoteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, velocity);
                sendMessage(cachedNoteOff);
            }
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for note off: {}", e.getMessage());
        }
    }

    public void polyPressure(int data1, int data2) {
        try {
            ShortMessage message = new ShortMessage(ShortMessage.POLY_PRESSURE, channel, data1, data2);
            sendMessage(message);
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for poly pressure: {}", e.getMessage());
        }
    }

    public void programChange(int program, int data2) {
        try {
            ShortMessage message = new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, data2);
            sendMessage(message);
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for program change: {}", e.getMessage());
        }
    }

    public void start() {
        try {
            ShortMessage message = new ShortMessage(ShortMessage.START, 0, 0, 0);
            sendMessage(message);
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for start: {}", e.getMessage());
        }
    }

    public void stop() {
        try {
            ShortMessage message = new ShortMessage(ShortMessage.STOP, 0, 0, 0);
            sendMessage(message);
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for stop: {}", e.getMessage());
        }
    }

    /**
     * Play a note with specified decay time
     */
    public void playMidiNote(int note, int velocity, int decay) {
        // Send note on
        noteOn(note, velocity);

        // Schedule note off
        scheduleNoteOff(note, 0, decay);
    }

    /**
     * Schedule a noteOff command after specified delay
     */
    private void scheduleNoteOff(int note, int velocity, int delayMs) {
        // Initialize scheduler if needed
        if (NOTE_OFF_SCHEDULER == null) {
            NOTE_OFF_SCHEDULER = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "NoteOffScheduler");
                t.setDaemon(true);
                return t;
            });
        }

        // Schedule the note-off message
        NOTE_OFF_SCHEDULER.schedule(() -> {
            noteOff(note, velocity);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        logger.info("Cleaning up instrument: {}", getName());

        // Close receiver if we have one
        if (receiver != null) {
            try {
                receiver.close();
                receiver = null;
            } catch (Exception e) {
                logger.warn("Error closing receiver: {}", e.getMessage());
            }
        }

        // Close device if we have one
        if (device != null && device.isOpen()) {
            try {
                device.close();
            } catch (Exception e) {
                logger.warn("Error closing device: {}", e.getMessage());
            }
        }

        if (deviceName != null) {
            MidiService.getInstance().closeReceiver(deviceName);
        }
    }

    /**
     * Set the device and update receiver accordingly
     */
    public void setDevice(MidiDevice device) {
        // Clean up existing resources
        cleanup();

        // Update device reference
        this.device = device;

        if (device != null) {
            setDeviceName(device.getDeviceInfo().getName());

            // Get a new receiver from the device
            try {
                if (!device.isOpen()) {
                    device.open();
                }
                this.receiver = device.getReceiver();
                logger.info("Device {} initialized with receiver", getName());
            } catch (MidiUnavailableException e) {
                logger.error("Failed to get receiver from device: {}", e.getMessage());
            }
        }
    }

    /**
     * Apply current bank and program settings
     */
    public void applyBankAndProgram() {
        try {
            // Send bank select MSB (CC 0)
            controlChange(0, bankMSB);

            // Send bank select LSB (CC 32)
            controlChange(32, bankLSB);

            // Send program change
            if (preset != null) {
                programChange(preset, 0);
                logger.info("Applied bank={}/{}, program={}", bankMSB, bankLSB, preset);
            }
        } catch (Exception e) {
            logger.error("Failed to apply bank and program: {}", e.getMessage());
        }
    }

    /**
     * Get a receiver for this instrument, creating one if needed
     *
     * @return The instrument's receiver
     */
    public Receiver getReceiver() {
        // If we already have a working receiver, use it
        if (receiver != null) {
            return receiver;
        }

        // If we have a device, try to get a receiver from it
        if (device != null) {
            try {
                ensureDeviceOpen();
                receiver = device.getReceiver();
                if (receiver != null) {
                    return receiver;
                }
            } catch (MidiUnavailableException e) {
                logger.warn("Could not get receiver from device: {}", e.getMessage());
            }
        }

        if (deviceName != null) {
            try {
                receiver = MidiService.getInstance().getReceiver(deviceName);
                if (receiver != null) {
                    return receiver;
                }
            } catch (Exception e) {
                logger.warn("Could not get receiver from MidiService: {}", e.getMessage());
            }
        }

        logger.error("Failed to get a receiver for instrument: {}", getName());
        return null;
    }

    /**
     * Set the receiver directly (preferred approach)
     */
    public void setReceiver(Receiver receiver) {
        // Clean up old receiver if exists
        if (this.receiver != null && this.receiver != receiver) {
            try {
                this.receiver.close();
            } catch (Exception e) {
                logger.warn("Error closing old receiver: {}", e.getMessage());
            }
        }

        this.receiver = receiver;
        logger.debug("Set receiver for instrument: {}", getName());
    }

    /**
     * Send multiple MIDI CC messages efficiently in bulk
     */
    public void sendBulkCC(int[] controllers, int[] values) {
        if (controllers.length != values.length) {
            throw new IllegalArgumentException("Controller and value arrays must be same length");
        }

        // Get or create a receiver
        Receiver usableReceiver = getReceiver();
        if (usableReceiver == null) {
            logger.error("Cannot send bulk CC - no receiver available");
            return;
        }

        // Reuse single message for all CC messages
        ShortMessage msg = new ShortMessage();
        try {
            // Send all CC messages with same timestamp for efficiency
            for (int i = 0; i < controllers.length; i++) {
                msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, controllers[i], values[i]);
                usableReceiver.send(msg, -1);
            }
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data in bulk CC: {}", e.getMessage());
        }
    }

    // Helper methods for data management
    public void assign(int cc, String control) {
        getAssignments().put(cc, control);
    }

    public void setBounds(int cc, int lowerBound, int upperBound) {
        getBoundaries().put(cc, new Integer[]{lowerBound, upperBound});
    }

    @JsonIgnore
    public Integer getAssignmentCount() {
        return getAssignments().size();
    }

    /**
     * Get the bank index from MSB/LSB
     */
    public Integer getBankIndex() {
        // If we have MSB/LSB values set, calculate the combined index
        if (bankMSB != 0 || bankLSB != 0) {
            return (bankMSB << 7) | bankLSB;
        }
        // Otherwise return the stored bankIndex field
        return bankIndex;
    }

    /**
     * Set the bank index (combined MSB/LSB)
     */
    public void setBankIndex(Integer bankIndex) {
        this.bankIndex = bankIndex;

        if (bankIndex == null) {
            this.bankMSB = 0;
            this.bankLSB = 0;
        } else {
            // Use upper/lower bytes of the integer for MSB/LSB
            this.bankMSB = (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON; // Upper 7 bits
            this.bankLSB = bankIndex & MidiControlMessageEnum.POLY_MODE_ON; // Lower 7 bits
        }

        logger.info("Set bank index to: {} (MSB: {}, LSB: {})",
                bankIndex, bankMSB, bankLSB);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
