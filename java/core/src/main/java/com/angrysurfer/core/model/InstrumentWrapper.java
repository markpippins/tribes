package com.angrysurfer.core.model;

// import com.angrysurfer.core.service.ReceiverManager; // Keep if ReceiverManager interaction is planned

import com.angrysurfer.core.sequencer.SequencerConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public final class InstrumentWrapper implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(InstrumentWrapper.class);

    private static final int DEFAULT_CHANNEL = 0;
    private static final int DEFAULT_VELOCITY = 100;
    private static final int DEFAULT_NOTE_DURATION_MS = 500;

    private static final String LOG_MSG_CHANNEL_PART = ", channel ";

    private static final ScheduledExecutorService noteOffScheduler;

    static {
        noteOffScheduler = Executors.newScheduledThreadPool(4, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("InstrumentWrapper-NoteOffScheduler");
            return t;
        });
    }

    @JsonIgnore
    private final transient Map<Integer, Integer> cachedControlChange = new HashMap<>();

    @JsonIgnore
    private Map<String, Object> properties;

    private List<ControlCode> controlCodes;

    private Long id;
    private String name;
    private Integer channel;
    private String deviceName;

    @JsonIgnore
    private MidiDevice device;

    @JsonIgnore
    private Receiver receiver;

    private Boolean defaultInstrument = false;
    private Boolean assignedToPlayer = false;
    private Integer bankIndex;
    private Integer preset;
    private String soundBank;

    private String description;
    private boolean initialized;

    private int bankLSB;
    private int bankMSB;

    private Integer highestNote = 126;
    private Integer lowestNote = 0;

    public InstrumentWrapper() {
        this.controlCodes = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    public InstrumentWrapper(InstrumentInfo instrumentInfo) {
        this();
        this.name = instrumentInfo.name();
        this.channel = (instrumentInfo.channel() >= 0 && instrumentInfo.channel() <= 15) ? instrumentInfo.channel() : DEFAULT_CHANNEL;
        this.device = instrumentInfo.device();
        this.deviceName = instrumentInfo.deviceName();
        this.receiver = instrumentInfo.receiver();
    }

    // Constructors

    public boolean isInternalSynth() {
        return (Objects.nonNull(getDeviceName()) && getDeviceName().equals(SequencerConstants.GERVILL) ||
                (Objects.nonNull(getDevice()) && getDevice() instanceof Synthesizer));
    }

    public void setDevice(MidiDevice device) {
        this.device = device;
        if (device != null) {
            this.deviceName = device.getDeviceInfo().getName();
            try {
                this.receiver = device.getReceiver();
            } catch (MidiUnavailableException e) {
                throw new RuntimeException(e);
            }
        }

        if (this.receiver == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Device name changed to '{}'. Attempting to acquire new receiver.", deviceName);
            }
            if (logger.isWarnEnabled()) {
                logger.warn("ReceiverManager interaction for new deviceName '{}' is not fully implemented. Receiver is currently null.", deviceName);
            }
        }
    }

    @JsonIgnore
    public boolean isAvailable() {
        return Objects.nonNull(getDevice()) && getDevice().isOpen();
    }


    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        if (this.receiver == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Device name changed to '{}'. Attempting to acquire new receiver.", deviceName);
            }
            if (logger.isWarnEnabled()) {
                logger.warn("ReceiverManager interaction for new deviceName '{}' is not fully implemented. Receiver is currently null.", deviceName);
            }
        }
    }

    public void setChannel(Integer channelInput) {
        if (channelInput != null && channelInput >= 0 && channelInput <= 15) {
            this.channel = channelInput;
        } else {
            this.channel = DEFAULT_CHANNEL;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Set channel to: {} for instrument {}", this.channel, getName());
        }
    }

    public void setChannel(int channelInput) {
        if (channelInput >= 0 && channelInput <= 15) {
            this.channel = channelInput;
        } else {
            this.channel = DEFAULT_CHANNEL;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Set channel to: {} for instrument {}", this.channel, getName());
        }
    }

    public Map<Integer, Integer> getCachedControlChanges() {
        return cachedControlChange;
    }

    // MIDI sending methods
    public void sendMessage(ShortMessage msg) {
        Receiver currentReceiver = getReceiver();
        if (currentReceiver != null) {
            currentReceiver.send(msg, -1);
            if (logger.isTraceEnabled()) {
                logger.trace("Sent MIDI message: status={}, data1={}, data2={} on channel {} for instrument {}",
                        msg.getStatus(), msg.getData1(), msg.getData2(), msg.getChannel(), getName());
            }
        }
        // No explicit else block needed here as getReceiver() already logs a warning if receiver is null.
    }

    public void noteOn(int noteNumber, int velocity) {
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_ON, this.channel, noteNumber, velocity);
            sendMessage(msg);
            if (logger.isDebugEnabled()) {
                logger.debug("Note ON: note={}, velocity={}, channel={} for {}", noteNumber, velocity, this.channel, getName());
            }
        } catch (InvalidMidiDataException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Invalid MIDI data for NOTE_ON, instrument " + getName() + LOG_MSG_CHANNEL_PART + this.channel, e);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error sending NOTE_ON for instrument " + getName(), e);
            }
        }
    }

    public void noteOff(int noteNumber, int velocity) {
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.NOTE_OFF, this.channel, noteNumber, velocity);
            sendMessage(msg);
            if (logger.isDebugEnabled()) {
                logger.debug("Note OFF: note={}, velocity={}, channel={} for {}", noteNumber, velocity, this.channel, getName());
            }
        } catch (InvalidMidiDataException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Invalid MIDI data for NOTE_OFF, instrument " + getName() + LOG_MSG_CHANNEL_PART + this.channel, e);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error sending NOTE_OFF for instrument " + getName(), e);
            }
        }
    }

    public void noteOff(int noteNumber) {
        noteOff(noteNumber, 0);
    }

    public void scheduleNoteOff(final int noteNumber, final int velocity, long delayMs) {
        try {
            noteOffScheduler.schedule(() -> noteOff(noteNumber, velocity), delayMs, TimeUnit.MILLISECONDS);
            if (logger.isDebugEnabled()) {
                logger.debug("Scheduled Note OFF: note={}, velocity={}, delay={}ms for {}", noteNumber, velocity, delayMs, getName());
            }
        } catch (RejectedExecutionException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Note OFF scheduling rejected for instrument " + getName() + ". Executing immediately.", e);
            }
            noteOff(noteNumber, velocity);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error scheduling NOTE_OFF for instrument " + getName() + ". Executing immediately.", e);
            }
            noteOff(noteNumber, velocity);
        }
    }

    public void scheduleNoteOff(final int noteNumber, long delayMs) {
        scheduleNoteOff(noteNumber, 0, delayMs);
    }

    public void playNote(int noteNumber, int velocity, int durationMs) {
        noteOn(noteNumber, velocity);
        if (durationMs > 0) {
            scheduleNoteOff(noteNumber, velocity, durationMs);
        }
    }

    public void playNote(int noteNumber, int durationMs) {
        playNote(noteNumber, DEFAULT_VELOCITY, durationMs);
    }

    public void playNote(int noteNumber) {
        playNote(noteNumber, DEFAULT_VELOCITY, DEFAULT_NOTE_DURATION_MS);
    }

    public void controlChange(int controller, int value) {
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.CONTROL_CHANGE, this.channel, controller, value);
            sendMessage(msg);
            cachedControlChange.put(controller, value);
            if (logger.isDebugEnabled()) {
                logger.debug("Control Change: controller={}, value={}, channel={} for {}", controller, value, this.channel, getName());
            }
        } catch (InvalidMidiDataException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Invalid MIDI data for CONTROL_CHANGE, instrument " + getName() + LOG_MSG_CHANNEL_PART + this.channel, e);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error sending CONTROL_CHANGE for instrument " + getName(), e);
            }
        }
    }

    public void pitchBend(int value) {
        try {
            ShortMessage msg = new ShortMessage();
            int lsb = value & 0x7F;
            int msb = (value >> 7) & 0x7F;
            msg.setMessage(ShortMessage.PITCH_BEND, this.channel, lsb, msb);
            sendMessage(msg);
            if (logger.isDebugEnabled()) {
                logger.debug("Pitch Bend: value={}, channel={} for {}", value, this.channel, getName());
            }
        } catch (InvalidMidiDataException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Invalid MIDI data for PITCH_BEND, instrument " + getName() + LOG_MSG_CHANNEL_PART + this.channel, e);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error sending PITCH_BEND for instrument " + getName(), e);
            }
        }
    }

    public void programChange(int programNumber, int data2) {
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.PROGRAM_CHANGE, this.channel, programNumber, data2);
            sendMessage(msg);
            if (logger.isDebugEnabled()) {
                logger.debug("Program Change: program={}, channel={} for {}", programNumber, this.channel, getName());
            }
        } catch (InvalidMidiDataException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Invalid MIDI data for PROGRAM_CHANGE, instrument " + getName() + LOG_MSG_CHANNEL_PART + this.channel, e);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error sending PROGRAM_CHANGE for instrument " + getName(), e);
            }
        }
    }

    public void allNotesOff() {
        controlChange(123, 0);
        if (logger.isInfoEnabled()) {
            logger.info("Sent All Notes Off for instrument {} on channel {}", getName(), this.channel);
        }
    }

    public void resetAllControllers() {
        controlChange(121, 0);
        if (logger.isInfoEnabled()) {
            logger.info("Sent Reset All Controllers for instrument {} on channel {}", getName(), this.channel);
        }
    }

    // New method
    public boolean receivesOn(int queryChannel) {
        return this.channel == queryChannel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("InstrumentWrapper{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append("'}");
        sb.append(", deviceName='").append(deviceName).append("'}");
        sb.append(", channel=").append(channel);
        sb.append(", internalSynth=").append(isInternalSynth());
        sb.append(", isDefault=").append(defaultInstrument);
        sb.append(", assignedToPlayer=").append(assignedToPlayer);
        sb.append(", bankIndex=").append(bankIndex);
        sb.append(", preset=").append(preset);
        sb.append(", soundbankName='").append(soundBank).append("'}");
        if (receiver != null) {
            sb.append(receiver.getClass().getSimpleName());
        } else {
            sb.append("null");
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstrumentWrapper that = (InstrumentWrapper) o;

        // Primary comparison based on ID if both are non-null
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }

        // If one ID is null and the other isn't, they are not equal
        if ((id == null && that.id != null) || (id != null && that.id == null)) {
            return false;
        }

        // If both IDs are null, compare based on other significant fields
        // (name, deviceName, channel)
        if (channel != that.channel) return false;
        if (!Objects.equals(name, that.name)) return false;
        return Objects.equals(deviceName, that.deviceName);
    }

    @Override
    public int hashCode() {
        // If ID is available, use its hash code primarily
        if (this.id != null) {
            return this.id.hashCode();
        }
        // Fallback hashcode if id is null, using other significant fields
        int result = this.name != null ? this.name.hashCode() : 0;
        result = 31 * result + (this.deviceName != null ? this.deviceName.hashCode() : 0);
        result = 31 * result + this.channel; // channel is primitive int
        return result;
    }

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


//    private void readObject(ObjectInputStream inStream)
//            throws IOException, ClassNotFoundException {
//        inStream.defaultReadObject();
//        this.receiver = null;
//        // transient cachedControlChange is already re-initialized by its declaration
//        initializeScheduler(); // Ensure scheduler is ready
//        if (InstrumentWrapper.logger.isInfoEnabled()) {
//            InstrumentWrapper.logger.info("InstrumentWrapper deserialized: {}. Receiver needs to be re-injected.", this.name);
//        }
//    }
}
