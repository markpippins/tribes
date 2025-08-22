package com.angrysurfer.core.service;

import com.angrysurfer.core.api.midi.MidiControlMessageEnum;
import com.angrysurfer.core.model.InstrumentInfo;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.preset.DrumItem;
import com.angrysurfer.core.model.preset.SynthData;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manager for internal synthesizer instruments and presets. This singleton
 * provides access to internal synthesizers and their preset information.
 */
public class InternalSynthManager {

    private static final Logger logger = LoggerFactory.getLogger(InternalSynthManager.class);
    private static InternalSynthManager instance;
    // Map of synth IDs to preset information
    private final Map<Long, SynthData> synthDataMap = new HashMap<>();
    // Default channel for melodic sounds
    // Use LinkedHashMap to preserve insertion order
    private final LinkedHashMap<String, Soundbank> soundbanks = new LinkedHashMap<>();
    private final ScheduledExecutorService noteOffScheduler = Executors.newScheduledThreadPool(2);
    // Map to store available banks for each soundbank (by name)
    private final Map<String, List<Integer>> availableBanksMap = new HashMap<>();
    // Add synthesizer as a central instance
    private Synthesizer synthesizer;
    // Add these fields for performance
    private MidiChannel[] cachedChannels;

    /**
     * Initialize the manager and register command listeners
     */
    private InternalSynthManager() {
    }

    /**
     * Get the singleton instance
     */
    public static synchronized InternalSynthManager getInstance() {
        if (instance == null) {
            instance = new InternalSynthManager();
        }
        return instance;
    }

    public void initialize() {
        try {
            initializeSynthesizer();
            initializeSynthData();
            loadDefaultSoundbank();
        } catch (Exception e) {
            logger.error("Error initializing InternalSynthManager", e);
        }
    }

    /**
     * Initialize synthesizer data structures
     */
    public void initializeSynthData() {
        try {
            // Clear existing data first
            synthDataMap.clear();

            // We need a synthesizer instance
            if (synthesizer == null) {
                initializeSynthesizer();
            }

            if (synthesizer != null) {
                // Create entry for the default soundbank
                Soundbank defaultSoundbank = synthesizer.getDefaultSoundbank();
                if (defaultSoundbank != null) {
                    String sbName = SequencerConstants.DEFAULT_SOUNDBANK;
                    SynthData synthData = new SynthData(sbName);

                    // Add all instruments from default soundbank
                    for (Instrument instrument : defaultSoundbank.getInstruments()) {
                        synthData.addInstrument(instrument);
                    }

                    // Store in map with the synthesizer ID
                    long synthId = System.identityHashCode(synthesizer);
                    synthDataMap.put(synthId, synthData);

                    // Also add to soundbanks collection
                    soundbanks.put(sbName, defaultSoundbank);

                    // Cache available banks
                    availableBanksMap.put(sbName, synthData.getAvailableBanks());

                    logger.info("Initialized synthesizer data with {} instruments",
                            synthData.getInstruments().size());
                } else {
                    logger.warn("No default soundbank available in synthesizer");
                }
            } else {
                logger.warn("No synthesizer available for initialization");
            }
        } catch (Exception e) {
            logger.error("Error initializing synth data: " + e.getMessage(), e);
        }
    }

    /**
     * Ensure internal synthesizer is available
     */
    public void ensureInternalSynthAvailable() {
        if (!checkInternalSynthAvailable()) {
            initializeSynthesizer();
            logger.info("Initialized internal synth for drum sequencer");
        }
    }

    /**
     * Initialize the synthesizer
     * This should be called during startup
     */
    private void initializeSynthesizer() {
        try {
            // Try to find Gervill synthesizer first
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info gervillInfo = null;

            for (MidiDevice.Info info : infos) {
                if (info.getName().contains(SequencerConstants.GERVILL)) {
                    gervillInfo = info;
                    break;
                }
            }

            if (gervillInfo != null) {
                synthesizer = (Synthesizer) MidiSystem.getMidiDevice(gervillInfo);
            }

            // If Gervill not found, get default synthesizer
            if (synthesizer == null) {
                synthesizer = MidiSystem.getSynthesizer();
            }

            if (synthesizer != null && !synthesizer.isOpen()) {
                synthesizer.open();
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                logger.info("Synthesizer initialized: {}", synthesizer.getDeviceInfo().getName());
            }
        } catch (Exception e) {
            logger.error("Error initializing synthesizer: " + e.getMessage(), e);
        }
    }

    /**
     * Get the synthesizer instance
     *
     * @return The MIDI synthesizer
     */
    public Synthesizer getSynthesizer() {
        if (synthesizer == null || !synthesizer.isOpen()) {
            initializeSynthesizer();
        }
        return synthesizer;
    }

    /**
     * Get the internal synthesizer device
     *
     * @return The internal synthesizer device
     * @throws MidiUnavailableException if the synthesizer is unavailable
     */
    public MidiDevice getInternalSynthDevice() throws MidiUnavailableException {
        if (synthesizer == null) {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            cachedChannels = synthesizer.getChannels();
        }

        return synthesizer;
    }

    /**
     * Get all available internal synthesizers in the system
     *
     * @return A list of InstrumentWrapper objects that are internal synthesizers
     */
    public List<InstrumentWrapper> getInternalSynths() {
        List<InstrumentWrapper> internalSynths = new ArrayList<>();

        try {
            // Get all MIDI device info
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

            for (MidiDevice.Info info : infos) {
                try {
                    // Try to get the device
                    MidiDevice device = MidiSystem.getMidiDevice(info);

                    // Check if it's a synthesizer
                    if (device instanceof Synthesizer) {
                        // Create an instrument wrapper for this synth
                        InstrumentInfo instrumentInfo = new InstrumentInfo(info.getDescription(), -1, info.getName(), device, device.getReceiver());
                        InstrumentWrapper wrapper = new InstrumentWrapper(instrumentInfo);
                        wrapper.setId(System.currentTimeMillis()); // Unique ID
                        wrapper.setName(info.getName());
                        wrapper.setDeviceName(info.getName());
                        wrapper.setDevice(device);
                        wrapper.setDescription(info.getDescription());


                        // Add to the list
                        internalSynths.add(wrapper);

                        logger.debug("Found internal synthesizer: {}", info.getName());
                    }
                } catch (Exception e) {
                    logger.warn("Error checking device {}: {}", info.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error getting internal synthesizers: {}", e.getMessage(), e);
        }

        return internalSynths;
    }

    /**
     * Creates and initializes an instrument for the internal synth on a specific
     * channel
     * This is the single entry point for all internal synth instrument creation
     *
     * @param channel The MIDI channel to use
     * @param name    Optional custom name (will be generated if null)
     * @return A fully configured InstrumentWrapper for the internal synth
     */
    public InstrumentWrapper createInternalInstrument(String name, int channel, MidiDevice device) {
        if (synthesizer == null) {
            initializeSynthesizer();
            if (synthesizer == null) {
                logger.error("Failed to initialize synthesizer");
                return null;
            }
        }

        // Generate a name if none provided
        String instrumentName = name != null ? name : channel == SequencerConstants.MIDI_DRUM_CHANNEL ? "Internal Drums" : "Internal Synth " + channel;

        try {
            InstrumentWrapper instrument = InstrumentManager.getInstance().createInstrumentWrapper(channel, name);
            instrument.setDevice(device);
            if (!device.isOpen())
                device.open();
            // Initialize the instrument MIDI state
            initializeInstrumentState(instrument);

            logger.info("Created internal instrument: {} for channel {}", instrumentName, channel);
            return instrument;
        } catch (Exception e) {
            logger.error("Error creating internal instrument: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Initialize the MIDI state for an internal instrument
     * Sends the necessary MIDI commands to set up the instrument
     *
     * @param instrument The instrument to initialize
     */
    public void initializeInstrumentState(InstrumentWrapper instrument) {
        if (instrument == null)
            return;

        try {
            // Apply bank selection
            int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
            int channel = instrument.getChannel();

            // Send bank select commands
            int bankMSB = (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON;
            int bankLSB = bankIndex & MidiControlMessageEnum.POLY_MODE_ON;

            // Send through instrument to ensure routing is correct
            instrument.controlChange(0, bankMSB); // Bank MSB
            instrument.controlChange(32, bankLSB); // Bank LSB

            // Send program change
            int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
            instrument.programChange(preset, 0);

            logger.debug("Initialized MIDI state for instrument {} (channel: {}, bank: {}, preset: {})",
                    instrument.getName(), channel, bankIndex, preset);
        } catch (Exception e) {
            logger.error("Failed to initialize instrument state: {}", e.getMessage(), e);
        }
    }

    /**
     * Update an instrument's preset with proper MIDI commands
     *
     * @param instrument The instrument to update
     * @param bankIndex  The bank index
     * @param preset     The program/preset number
     * @return true if preset was applied successfully, false otherwise
     */
    public boolean updateInstrumentPreset(InstrumentWrapper instrument, Integer bankIndex, Integer preset) {
        if (instrument == null)
            return false;

        try {
            // Update instrument properties
            if (bankIndex != null) {
                instrument.setBankIndex(bankIndex);
            }

            if (preset != null) {
                instrument.setPreset(preset);
            }

            // Make sure we have the correct channel
            int channel = instrument.getChannel() != -1 ? instrument.getChannel() : 0;
            bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
            preset = instrument.getPreset() != null ? instrument.getPreset() : 0;

            logger.info("InternalSynthManager updating: channel={}, bank={}, program={}",
                    channel, bankIndex, preset);

            // Make sure synth is initialized
//            if (synthesizer == null || !synthesizer.isOpen()) {
//                initializeSynthesizer();
//            }

            boolean success = false;

            if (synthesizer != null && synthesizer.isOpen()) {
                // First apply through the synth's MidiChannels directly
//                MidiChannel[] channels = synthesizer.getChannels();
//                if (channels != null && channel < channels.length) {
//                    channels[channel].controlChange(0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON);
//                    channels[channel].controlChange(32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON);
//                    channels[channel].programChange(preset);
//
//                    // For percussion channel, ensure drum mode is enabled
//                    if (channel == SequencerConstants.MIDI_DRUM_CHANNEL) {
//                        channels[channel].controlChange(0, 120);
//                    }
//
//                    logger.info("Applied preset via direct synth channel: ch={}, bank={}, program={}",
//                            channel, bankIndex, preset);
//
//                    success = true;
//                } else {
//                    logger.warn("Could not access synthesizer channel {}", channel);
//                }

                // Also try through Receiver for completeness
                try {
                    Receiver receiver = instrument.getReceiver();
                    if (receiver != null) {
                        // Bank select MSB
                        javax.sound.midi.ShortMessage bankMSB = new javax.sound.midi.ShortMessage();
                        bankMSB.setMessage(0xB0 | channel, 0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON);
                        receiver.send(bankMSB, -1);

                        // Bank select LSB
                        javax.sound.midi.ShortMessage bankLSB = new javax.sound.midi.ShortMessage();
                        bankLSB.setMessage(0xB0 | channel, 32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON);
                        receiver.send(bankLSB, -1);

                        // Program change
                        javax.sound.midi.ShortMessage pc = new javax.sound.midi.ShortMessage();
                        pc.setMessage(0xC0 | channel, preset, 0);
                        receiver.send(pc, -1);

                        logger.info("Applied preset via synth receiver: ch={}, bank={}, program={}",
                                channel, bankIndex, preset);

                        success = true;
                    }
                } catch (Exception e) {
                    logger.warn("Error sending direct MIDI messages: {}", e.getMessage());
                }
            } else {
                logger.warn("Synthesizer not available for preset change");
                return false;
            }

            logger.debug("Updated preset for instrument {} to bank {}, program {}",
                    instrument.getName(),
                    instrument.getBankIndex(),
                    instrument.getPreset());

            return success;
        } catch (Exception e) {
            logger.error("Failed to update instrument preset: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Simple test to check if an instrument belongs to the internal synth
     *
     * @param instrument The instrument to check
     * @return true if this is an internal synth instrument
     */
    public boolean isInternalSynthInstrument(InstrumentWrapper instrument) {
        if (instrument == null)
            return false;

        return SequencerConstants.GERVILL.equals(instrument.getDeviceName()) ||
                "Java Sound Synthesizer".equals(instrument.getDeviceName()) ||
                (instrument.getDevice() == synthesizer);
    }


    /**
     * Get instrument for internal synthesizer on a specific channel
     *
     * @param channel MIDI channel
     * @return The instrument, creating it if necessary
     */
    public InstrumentWrapper createInternalSynthInstrument(int channel, boolean exclusive, int tag) {
        String name = (channel == SequencerConstants.MIDI_DRUM_CHANNEL) ?
                "Internal Drums " + tag :
                "Internal Melo " + channel + "-" + tag;

//        InstrumentWrapper instrument = InstrumentManager.getInstance().getInstrumentCache().get(id);
//        if (instrument != null) {
//            if (exclusive && Boolean.TRUE.equals(instrument.getAssignedToPlayer())) {
//                // If exclusive and already assigned to a player, and it's not for the same purpose (e.g. re-request),
//                // it might indicate a conflict or need for a new instance.
//                // For simplicity, if found by ID and requested exclusively, but already assigned, we might need a new one or log a warning.
//                // Current logic: if found by ID, use it but ensure assignment status.
//            }
//        } else { // Not found by ID, try to find a suitable one by properties
//            Optional<InstrumentWrapper> foundByProperties = InstrumentManager.getInstance().getInstrumentCache().values()
//                    .stream().filter(cached ->
//                            InternalSynthManager.getInstance().isInternalSynthInstrument(cached) &&
//                                    cached.getChannel() == channel &&
//                                    (!exclusive || !Boolean.TRUE.equals(cached.getAssignedToPlayer())) &&
//                                    cached.getName().startsWith((channel == SequencerConstants.MIDI_DRUM_CHANNEL) ? "Internal Drums" : "Internal Melo " + channel)) // More specific name check
//                    .findFirst();
//            if (foundByProperties.isPresent()) {
//                instrument = foundByProperties.get();
//            }
//        }
//
//        if (instrument != null) { // Found either by ID or properties
//            if (instrument.getReceiver() == null && instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
//                Receiver receiver = ReceiverManager.getInstance().getOrCreateReceiver(instrument.getDeviceName());
//                instrument.setReceiver(receiver);
//                if (receiver == null) {
//                    logger.warn("Could not obtain receiver for existing internal instrument {} (device: {})", instrument.getName(), instrument.getDeviceName());
//                }
//            }
//            if (exclusive) {
//                instrument.setAssignedToPlayer(true); // Mark as assigned
//                // Potentially update the instrument in cache/DB if assignedToPlayer changed
//                // updateInstrument(instrument); // Could cause recursion if called from onCommand.INSTRUMENT_UPDATED
//            }
//            return instrument;
//        }


        // No suitable instrument found, create a new one

        // Use InternalSynthManager to create the basic structure
        MidiDevice device = DeviceManager.getMidiDevice(SequencerConstants.GERVILL);
        InstrumentWrapper instrument = InstrumentManager.getInstance().createInstrumentWrapper(channel, name);


        if (instrument != null && device != null) {
            instrument.setDevice(device);
            Receiver receiver = ReceiverManager.getInstance().getOrCreateReceiver(instrument.getDeviceName());
            instrument.setReceiver(receiver);
            InstrumentManager.getInstance().updateInstrument(instrument); // This saves, caches, and sets receiver
            if (exclusive) {
                instrument.setAssignedToPlayer(true);
            }
        }
        return instrument;
    }

    /**
     * Play a note through the internal synth (optimized path)
     *
     * @param note       MIDI note number
     * @param velocity   Velocity (0-127)
     * @param durationMs Duration in milliseconds
     * @param channel    MIDI channel
     */
    public void playNote(int note, int velocity, int durationMs, int channel) {
        if (synthesizer == null) {
            initializeSynthesizer();
        }

        if (synthesizer == null || !synthesizer.isOpen()) {
            return;
        }

        try {
            // Cache channels for performance
            if (cachedChannels == null) {
                cachedChannels = synthesizer.getChannels();
            }

            // Safety bounds check
            if (channel < 0 || channel >= cachedChannels.length) {
                return;
            }

            final MidiChannel midiChannel = cachedChannels[channel];
            if (midiChannel != null) {
                // Direct method call - much faster than creating threads
                midiChannel.noteOn(note, velocity);

                // Schedule note off with the shared executor
                noteOffScheduler.schedule(() -> {
                    try {
                        midiChannel.noteOff(note);
                    } catch (Exception e) {
                        // Ignore errors in note-off
                    }
                }, durationMs, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Just log at trace level - don't slow down playback
            logger.trace("Error playing note: {}", e.getMessage());
        }
    }

    /**
     * All notes off for a specific channel
     *
     * @param channel The MIDI channel
     */
    public void allNotesOff(int channel) {
        if (synthesizer == null || !synthesizer.isOpen()) {
            return;
        }

        try {
            // Cache channels array for performance
            if (cachedChannels == null) {
                cachedChannels = synthesizer.getChannels();
            }

            // Check bounds
            if (channel >= 0 && channel < cachedChannels.length) {
                // Send all notes off message
                cachedChannels[channel].controlChange(123, 0);
                logger.debug("Sent All Notes Off to channel {}", channel);
            }
        } catch (Exception e) {
            logger.error("Error sending all notes off: {}", e.getMessage(), e);
        }
    }

    /**
     * Load the default soundbank and any custom soundbanks
     */
    public void loadDefaultSoundbank() {
        if (synthesizer == null) {
            initializeSynthesizer();
            if (synthesizer == null) {
                logger.error("Failed to initialize synthesizer to load soundbanks");
                return;
            }
        }

        // Delegate to SoundbankManager
        SoundbankManager.getInstance().initializeSoundbanks();
    }

    /**
     * Get available soundbank names
     */
    public List<String> getSoundbankNames() {
        return SoundbankManager.getInstance().getSoundbankNames();
    }

    /**
     * Get banks for a soundbank
     */
    public List<Integer> getAvailableBanksByName(String soundbankName) {
        return SoundbankManager.getInstance().getAvailableBanksByName(soundbankName);
    }

    /**
     * Get preset names for a soundbank and bank
     */
    public List<String> getPresetNames(String soundbankName, int bankIndex) {
        return SoundbankManager.getInstance().getPresetNames(soundbankName, bankIndex);
    }

    /**
     * Get General MIDI preset names
     */
    public List<String> getGeneralMIDIPresetNames() {
        return SoundbankManager.getInstance().getGeneralMIDIPresetNames();
    }

    /**
     * Get drum items
     */
    public List<DrumItem> getDrumItems() {
        return SoundbankManager.getInstance().getDrumItems();
    }

    /**
     * Get drum name for a note
     */
    public String getDrumName(int noteNumber) {
        return SoundbankManager.getInstance().getDrumName(noteNumber);
    }

    /**
     * Load a soundbank file
     */
    public String loadSoundbank(File file) {
        return SoundbankManager.getInstance().loadSoundbank(file);
    }


    public void setControlChange(int channel, int ccNumber, int value) {
        if (synthesizer == null || !synthesizer.isOpen()) {
            logger.warn("Cannot send CC - synthesizer is not available");
            return;
        }

        try {
            MidiChannel midiChannel = synthesizer.getChannels()[channel];
            if (midiChannel != null) {
                midiChannel.controlChange(ccNumber, value);
            }
        } catch (Exception e) {
            logger.error("Error setting control change: " + e.getMessage(), e);
        }
    }

    /**
     * Check if an instrument is from the internal synthesizer
     *
     * @param instrument The instrument to check
     * @return true if the instrument is from the internal synthesizer
     */
    public boolean isInternalSynth(InstrumentWrapper instrument) {
        if (instrument == null) {
            return false;
        }

        // Simple name check - fastest approach
        if (instrument.getDeviceName() != null &&
                (instrument.getDeviceName().equals(SequencerConstants.GERVILL) ||
                        instrument.getDeviceName().contains("Java Sound Synthesizer"))) {
            return true;
        }

        // Direct device instance comparison if available
        if (synthesizer != null && instrument.getDevice() == synthesizer) {
            return true;
        }

        // Last resort - check device info name
        if (instrument.getDevice() != null &&
                instrument.getDevice().getDeviceInfo() != null) {
            String deviceName = instrument.getDevice().getDeviceInfo().getName();
            return deviceName.equals(SequencerConstants.GERVILL) ||
                    deviceName.contains("Java Sound Synthesizer");
        }

        return false;
    }

    /**
     * Check if the internal synthesizer is available and ready for use
     *
     * @return true if the synthesizer is available and open
     */
    public boolean checkInternalSynthAvailable() {
        // Try to ensure the synthesizer is initialized
        if (synthesizer == null) {
            initializeSynthesizer();
        }

        // Check if synthesizer is available and open
        boolean isAvailable = synthesizer != null && synthesizer.isOpen();

        // Optional: Check if we can actually play notes
        if (isAvailable) {
            try {
                // Access channels as a simple test
                MidiChannel[] channels = synthesizer.getChannels();
                isAvailable = channels != null && channels.length > 0;
            } catch (Exception e) {
                logger.warn("Synthesizer appears to be open but is not functioning properly: {}", e.getMessage());
                isAvailable = false;
            }
        }

        return isAvailable;
    }
}
