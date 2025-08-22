package com.angrysurfer.core.service;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentInfo;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class InstrumentManager implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentManager.class);
    private static InstrumentManager instance;
    private final InstrumentHelper instrumentHelper;
    private final Map<Long, InstrumentWrapper> instrumentCache = new HashMap<>();
    private final ReceiverManager receiverManager; // ADDED

    private boolean needsRefresh = true;
    private boolean isInitializing = false;

    // Private constructor for singleton pattern
    private InstrumentManager() {
        this.instrumentHelper = RedisService.getInstance().getInstrumentHelper();
        this.receiverManager = ReceiverManager.getInstance(); // ADDED - Initialize ReceiverManager
        // Register for command events
        CommandBus.getInstance().register(this, new String[]{
                Commands.INSTRUMENT_UPDATED, Commands.INSTRUMENTS_REFRESHED, Commands.USER_CONFIG_LOADED
        });

        // Initial cache load
        refreshInstruments();
    }

    // Static method to get the singleton instance
    public static synchronized InstrumentManager getInstance() {
        if (instance == null) {
            instance = new InstrumentManager();
        }
        return instance;
    }

    private static InstrumentWrapper getInternalInstrument(int channel) {

        InstrumentInfo instrumentInfo = new InstrumentInfo(SequencerConstants.GERVILL + "Ch: " + channel,
                channel, SequencerConstants.GERVILL, null, null);

        InstrumentWrapper internalInstrument = new InstrumentWrapper(instrumentInfo);

        // Configure as internal instrument
        // Device name for internal synth should be consistent, e.g., Gervill
        internalInstrument.setDeviceName(SequencerConstants.GERVILL);
        internalInstrument.setSoundBank(SequencerConstants.DEFAULT_SOUNDBANK);
        internalInstrument.setBankIndex(0);
        internalInstrument.setPreset(0); // Piano
        internalInstrument.setId(9985L + channel); // Ensure unique ID
        // Receiver will be set by the manager when this instrument is fully processed/cached
        return internalInstrument;
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        switch (action.getCommand()) {
            case Commands.USER_CONFIG_LOADED -> {
                if (!isInitializing) {
                    refreshInstruments();
                } else {
                    logger.debug("Skipping refresh during initialization");
                }
            }
            case Commands.INSTRUMENT_UPDATED -> {
                if (action.getData() instanceof InstrumentWrapper instrument) {
                    // Ensure receiver is up-to-date if deviceName changed or receiver is missing
                    if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
                        Receiver currentReceiver = instrument.getReceiver();
                        // Simple check: if receiver is null or device name implies a different one might be needed.
                        // More sophisticated checks could compare current receiver's identity if possible.
                        if (currentReceiver == null) { // Simplified check for this example
                            Receiver newReceiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                            instrument.setReceiver(newReceiver);
                            if (newReceiver == null) {
                                logger.warn("Could not obtain receiver for updated instrument {} (device: {})", instrument.getName(), instrument.getDeviceName());
                            }
                        }
                    }
                    instrumentCache.put(instrument.getId(), instrument);
                    logger.info("Updated instrument in cache: {} (ID: {})", instrument.getName(), instrument.getId());
                }
            }
            case Commands.INSTRUMENTS_REFRESHED -> {
                refreshInstruments();
            }
        }
    }

    public void initializeCache() {
        logger.info("Initializing instrument cache");
        isInitializing = true;
        try {
            List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();
            instrumentCache.clear();

            if (instruments != null) {
                for (InstrumentWrapper instrument : instruments) {
                    if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
                        Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                        instrument.setReceiver(receiver);
                        if (receiver == null) {
                            logger.warn("Could not obtain receiver for instrument {} (device: {}) during cache initialization.", instrument.getName(), instrument.getDeviceName());
                        }
                    }
                    instrumentCache.put(instrument.getId(), instrument);
                }
                logger.info("Cached {} instruments and attempted to set receivers.", instrumentCache.size());
            } else {
                logger.warn("No instruments found in UserConfigManager");
            }
            needsRefresh = false;
        } finally {
            isInitializing = false;
        }
    }


    public InstrumentWrapper createInstrumentWrapper(int channel, String name) {
        InstrumentInfo info = new InstrumentInfo(name, channel, SequencerConstants.GERVILL, null, null);
        // Create the instrument wrapper
        InstrumentWrapper instrument = new InstrumentWrapper(info);

        // Configure instrument properties
        instrument.setDeviceName(SequencerConstants.GERVILL);
        instrument.setSoundBank(SequencerConstants.DEFAULT_SOUNDBANK);
        instrument.setBankIndex(0);

        // Default to appropriate preset based on channel
        instrument.setPreset(0); // 0 is grand piano for melodic channels

        // Generate ID consistently
        instrument.setId(RedisService.getInstance().getNextInstrumentId());
        return instrument;
    }


    public void refreshInstruments() {
        if (isInitializing) {
            logger.debug("Skipping recursive refresh call");
            return;
        }
        logger.info("Refreshing instruments cache");
        initializeCache(); // This will now also handle setting receivers
        needsRefresh = false;
    }

    /**
     * Get instruments that can be used on a specific channel
     */
    public List<InstrumentWrapper> getInstrumentByChannel(int channel) {
        return instrumentCache.values().stream()
                .filter(instrument -> {
                    // Safety check for null instrument
                    if (instrument == null)
                        return false;

                    try {
                        // Use safe receivesOn method
                        return instrument.receivesOn(channel);
                    } catch (Exception e) {
                        // Fallback for any unexpected errors
                        logger.warn("Error checking if instrument {} receives on channel {}: {} - falling back to direct channel check",
                                instrument.getName(), channel, e.getMessage());
                        return instrument.getChannel() == channel;
                    }
                })
                .sorted(Comparator.comparing(InstrumentWrapper::getName))
                .collect(Collectors.toList());
    }

    /**
     * Get an instrument by ID
     *
     * @param id The instrument ID to look up
     * @return The instrument with the specified ID, or null if not found
     */
    public InstrumentWrapper getInstrumentById(Long id) {
        if (id == null) {
            logger.warn("getInstrumentById called with null ID");
            return null;
        }

        logger.debug("Looking up instrument by ID: {}", id);

        InstrumentWrapper instrument = instrumentCache.get(id);

        if (instrument == null) {
            logger.info("Instrument with ID {} not found in cache, checking database", id);
            try {
                instrument = RedisService.getInstance().getInstrumentById(id);

                if (instrument != null) {
                    logger.info("Found instrument in database: {} (ID: {})",
                            instrument.getName(), instrument.getId());
                    if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
                        Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                        instrument.setReceiver(receiver);
                        if (receiver == null) {
                            logger.warn("Could not obtain receiver for instrument {} (device: {}) loaded from database.", instrument.getName(), instrument.getDeviceName());
                        }
                    }
                    instrumentCache.put(id, instrument); // Add to cache after loading and setting receiver
                } else {
                    logger.warn("Instrument with ID {} not found in database", id);
                }
            } catch (Exception e) {
                logger.error("Error retrieving instrument with ID {}: {}", id, e.getMessage(), e);
            }
        } else {
            // For cached instrument, ensure receiver is still valid or re-acquire if necessary
            if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
                if (instrument.getReceiver() == null) { // Or a more robust validation like receiverManager.isReceiverValid(instrument.getReceiver())
                    logger.warn("Receiver for cached instrument {} (device: {}) is missing/invalid. Attempting to re-acquire.", instrument.getName(), instrument.getDeviceName());
                    Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                    instrument.setReceiver(receiver);
                    if (receiver == null) {
                        logger.warn("Could not re-acquire receiver for cached instrument {} (device: {}).", instrument.getName(), instrument.getDeviceName());
                    }
                }
            }
            logger.debug("Found instrument in cache: {} (ID: {})", instrument.getName(), instrument.getId());
        }

        return instrument;
    }

    public List<String> getInstrumentNames() {
        if (needsRefresh) {
            refreshInstruments();
        }
        return instrumentCache.values().stream()
                .map(InstrumentWrapper::getName)
                .collect(Collectors.toList());
    }

    public InstrumentWrapper findByName(String name) {
        if (needsRefresh) {
            refreshInstruments();
        }
        // This might return an instrument without a guaranteed valid receiver if not accessed by ID first
        // Consider adding receiver check/refresh here too if direct findByName is common and needs live receiver
        return instrumentCache.values().stream()
                .filter(i -> i.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(instrument -> { // Ensure receiver is checked/set when found by name
                    if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty() && instrument.getReceiver() == null) {
                        Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                        instrument.setReceiver(receiver);
                        if (receiver == null) {
                            logger.warn("Could not obtain receiver for instrument {} (device: {}) found by name.", instrument.getName(), instrument.getDeviceName());
                        }
                    }
                    return instrument;
                })
                .orElse(null);
    }

    public InstrumentWrapper getInstrumentFromCache(Long instrumentId) {
        if (needsRefresh) {
            refreshInstruments();
        }
        InstrumentWrapper instrument = instrumentCache.get(instrumentId);
        if (instrument == null) {
            logger.warn("Cache miss for instrument ID: {}, refreshing cache", instrumentId);
            refreshInstruments(); // This will load and attempt to set receivers
            instrument = instrumentCache.get(instrumentId);
        } else {
            // Ensure receiver is set for cached instrument if it was missed or became invalid
            if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty() && instrument.getReceiver() == null) {
                Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                instrument.setReceiver(receiver);
                if (receiver == null) {
                    logger.warn("Could not obtain receiver for cached instrument {} (device: {}) on getInstrumentFromCache.", instrument.getName(), instrument.getDeviceName());
                }
            }
        }
        return instrument;
    }

    public List<InstrumentWrapper> getCachedInstruments() {
        if (instrumentCache.isEmpty() && !isInitializing) { // Avoid refresh if already initializing
            logger.info("Cache is empty, initializing...");
            refreshInstruments(); // Calls initializeCache which sets receivers
        }
        return new ArrayList<>(instrumentCache.values());
    }

    /**
     * Update an instrument with special handling for default instruments
     */
    public void updateInstrument(InstrumentWrapper instrument) {
        if (instrument == null) {
            logger.warn("Cannot update null instrument");
            return;
        }
        try {
            // Ensure receiver is set/updated before saving
            if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
                Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                instrument.setReceiver(receiver);
                if (receiver == null && !instrument.isInternalSynth()) { // Internal synth might not always have a receiver immediately if Gervill is problematic
                    logger.warn("Could not obtain receiver for instrument {} (device: {}) during update. It might not function correctly.", instrument.getName(), instrument.getDeviceName());
                }
            }

            if (Boolean.TRUE.equals(instrument.getDefaultInstrument())) {
                logger.info("Updating default instrument in UserConfig: {}", instrument.getName());
                UserConfigManager.getInstance().updateDefaultInstrument(instrument);
            } else {
                RedisService.getInstance().saveInstrument(instrument);
            }
            instrumentCache.put(instrument.getId(), instrument);
            CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, instrument);
        } catch (Exception e) {
            logger.error("Error updating instrument {}: {}", instrument.getName(), e.getMessage(), e);
        }
    }

    /**
     * Removes an instrument from the cache and from UserConfigManager
     *
     * @param instrumentId The ID of the instrument to remove
     */
    public void removeInstrument(Long instrumentId) {
        if (instrumentId == null) {
            logger.warn("Attempt to remove instrument with null ID");
            return;
        }
        InstrumentWrapper instrument = instrumentCache.get(instrumentId);
        String name = instrument != null ? instrument.getName() : "Unknown ID: " + instrumentId;
        instrumentCache.remove(instrumentId);
        UserConfigManager.getInstance().removeInstrument(instrumentId); // Assuming this handles DB removal or marks as inactive
        logger.info("Instrument removed: {} (ID: {})", name, instrumentId);
        // Associated receiver in ReceiverManager is managed by deviceName and will be closed if no longer used by any InstrumentWrapper.
        // No explicit call to ReceiverManager.closeReceiver(instrument.getDeviceName()) here, as the receiver might be shared.
    }

    /**
     * Find or create an internal instrument for the specified channel
     *
     * @param channel The MIDI channel
     * @return An InstrumentWrapper for the internal synth
     */
    public InstrumentWrapper findOrCreateInternalInstrument(int channel) {
        // Look for an unassigned internal instrument on the specified channel
        Optional<InstrumentWrapper> existing = getCachedInstruments().stream()
                .filter(inst -> Boolean.TRUE.equals(inst.isInternalSynth()) &&
                        !Boolean.TRUE.equals(inst.getAssignedToPlayer()) && // Check assignedToPlayer
                        inst.getChannel() == channel)
                .findFirst();

        if (existing.isPresent()) {
            InstrumentWrapper instrument = existing.get();
            // Ensure receiver is set
            if (instrument.getReceiver() == null && instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
                Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                instrument.setReceiver(receiver);
            }
            return instrument;
        }

        // Create a new internal instrument
        InstrumentWrapper internalInstrument = getInternalInstrument(channel); // This sets deviceName to Gervill
        // Receiver will be set by updateInstrument -> which calls getOrCreateReceiver
        updateInstrument(internalInstrument); // This will cache it and attempt to set its receiver
        return internalInstrument;
    }

    /**
     * Connect an instrument to a MIDI device
     *
     * @param drumIndex     The drum pad index (for logging)
     * @param instrument    The instrument to connect
     * @param defaultDevice The default device to use if preferred device isn't available
     * @return true if successfully connected, false otherwise
     */
    public boolean connectInstrumentToDevice(int drumIndex, InstrumentWrapper instrument, MidiDevice defaultDevice) {
        // Skip if already connected
        if (instrument.getReceiver() != null) {
            logger.debug("Drum {} already has a valid receiver", drumIndex);
            return true;
        }

        try {
            // Strategy 1: Try to get device by name
            MidiDevice device = null;
            String deviceName = instrument.getDeviceName();

            if (deviceName != null && !deviceName.isEmpty()) {
                device = DeviceManager.getInstance().acquireDevice(deviceName);
                if (device != null && !device.isOpen()) {
                    try {
                        device.open();
                        logger.debug("Opened device {} for drum {}", deviceName, drumIndex);
                    } catch (Exception e) {
                        logger.warn("Could not open device {} for drum {}: {}",
                                deviceName, drumIndex, e.getMessage());
                        device = null;
                    }
                }
            }

            // Strategy 2: Use default device if specific device not available
            if (device == null && defaultDevice != null) {
                device = defaultDevice;
                deviceName = defaultDevice.getDeviceInfo().getName();
                instrument.setDeviceName(deviceName);
                logger.debug("Using default device {} for drum {}", deviceName, drumIndex);
            }

            // Strategy 3: Try Gervill specifically as last resort
            if (device == null) {
                device = DeviceManager.getMidiDevice(SequencerConstants.GERVILL);
                if (device != null) {
                    if (!device.isOpen()) {
                        device.open();
                    }
                    deviceName = SequencerConstants.GERVILL;
                    instrument.setDeviceName(deviceName);
                    logger.debug("Using Gervill synthesizer for drum {}", drumIndex);
                }
            }

            // Now get a receiver for the device
            if (device != null) {
                instrument.setDevice(device);

                Receiver receiver = ReceiverManager.getInstance()
                        .getOrCreateReceiver(deviceName);

                if (receiver != null) {
                    instrument.setReceiver(receiver);
                    instrument.setChannel(SequencerConstants.MIDI_DRUM_CHANNEL);

                    // Find player that owns this instrument to apply preset
                    Player player = DrumSequencerManager.getInstance().findPlayerByInstrument(instrument);
                    if (player != null) {
                        SoundbankManager.getInstance().applyInstrumentPreset(player);
                        logger.info("Successfully connected drum {} to device {}", drumIndex, deviceName);
                        PlayerManager.getInstance().savePlayerProperties(player);
                        return true;
                    } else {
                        logger.warn("Could not find player for instrument to apply preset");
                    }
                } else {
                    logger.warn("Failed to get receiver for drum {} from device {}", drumIndex, deviceName);
                }
            } else {
                logger.warn("Could not find any valid device for drum {}", drumIndex);
            }
        } catch (Exception e) {
            logger.error("Error connecting drum {}: {}", drumIndex, e.getMessage());
        }

        return false;
    }

    /**
     * Refresh the instrument cache with the provided list of instruments
     */
    public void refreshCache(List<InstrumentWrapper> instruments) {
        if (instruments == null) return;
        instrumentCache.clear();
        for (InstrumentWrapper instrument : instruments) {
            if (instrument != null && instrument.getId() != null) {
                if (instrument.getDeviceName() != null && !instrument.getDeviceName().isEmpty()) {
                    Receiver receiver = receiverManager.getOrCreateReceiver(instrument.getDeviceName());
                    instrument.setReceiver(receiver);
                    if (receiver == null) {
                        logger.warn("Could not obtain receiver for instrument {} (device: {}) during refreshCache.", instrument.getName(), instrument.getDeviceName());
                    }
                }
                instrumentCache.put(instrument.getId(), instrument);
            }
        }
        logger.debug("Refreshed instrument cache with {} instruments", instruments.size());
    }

    /**
     * Determine who owns/uses an instrument
     *
     * @param instrument The instrument to check
     * @return A string description of the owner(s)
     */
    public String determineInstrumentOwner(InstrumentWrapper instrument) {
        if (instrument == null || instrument.getId() == null) {
            return "";
        }

        List<String> owners = new ArrayList<>();

        try {
            // Check session players
            Set<Player> sessionPlayers = SessionManager.getInstance().getActiveSession().getPlayers();
            if (sessionPlayers != null) {
                for (Player player : sessionPlayers) {
                    if (player != null &&
                            player.getInstrumentId() != null &&
                            player.getInstrumentId().equals(instrument.getId())) {
                        owners.add("Session: " + player.getName());
                    }
                }
            }

            // Check melodic sequencers
            for (MelodicSequencer sequencer : MelodicSequencerManager.getInstance().getAllSequencers()) {
                if (sequencer != null && sequencer.getPlayer() != null &&
                        sequencer.getPlayer().getInstrumentId() != null &&
                        sequencer.getPlayer().getInstrumentId().equals(instrument.getId())) {
                    owners.add("Melodic: " + sequencer.getClass().getSimpleName() + " (" + sequencer.getPlayer().getName() + ")");
                }
            }

            // Check drum sequencers (which have multiple players)
            for (DrumSequencer sequencer : DrumSequencerManager.getInstance().getAllSequencers()) {
                if (sequencer != null && sequencer.getPlayers() != null) {
                    for (Player player : sequencer.getPlayers()) {
                        if (player != null &&
                                player.getInstrumentId() != null &&
                                player.getInstrumentId().equals(instrument.getId())) {
                            owners.add("Drum: " + sequencer.getClass().getSimpleName() + " (" + player.getName() + ")");
                        }
                    }
                }
            }

            if (owners.isEmpty()) {
                return "None";
            } else if (owners.size() <= 2) {
                return String.join(", ", owners);
            } else {
                // If there are many owners, show a count
                return owners.get(0) + " and " + (owners.size() - 1) + " more";
            }
        } catch (Exception e) {
            logger.error("Error determining instrument owner for {}: {}", instrument.getName(), e.getMessage(), e);
            return "Error";
        }
    }

    /**
     * Get available drum instruments
     *
     * @return List of drum instruments
     */
    public List<InstrumentWrapper> getDrumInstruments() {
        List<InstrumentWrapper> drumInstruments = new ArrayList<>();

        try {
            // Get all instruments from the database
            List<InstrumentWrapper> allInstruments = getCachedInstruments(); // Ensures receivers are attempted

            // Filter for drum instruments (typically bank 128 or those with drum-related names)
            for (InstrumentWrapper instrument : allInstruments) {
                // Check if it's a drum bank (128) or has drum-related keywords in the name
                if (instrument.getBankIndex() == 128 ||
                        isDrumInstrumentByName(instrument.getName())) {
                    drumInstruments.add(instrument);
                }
            }

            // If no drum instruments found, return all instruments as fallback
            if (drumInstruments.isEmpty() && !allInstruments.isEmpty()) { // Avoid returning all if allInstruments is also empty
                logger.warn("No specific drum instruments found, returning all available instruments as fallback.");
                return allInstruments;
            }

            // Sort by name for easier selection
            drumInstruments.sort(Comparator.comparing(InstrumentWrapper::getName));

            logger.info("Found {} drum instruments.", drumInstruments.size());
            return drumInstruments;
        } catch (Exception e) {
            logger.error("Error getting drum instruments: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if an instrument is a drum based on its name
     *
     * @param name The instrument name
     * @return true if it appears to be a drum instrument
     */
    private boolean isDrumInstrumentByName(String name) {
        if (name == null) return false;

        String lowerName = name.toLowerCase();
        return lowerName.contains("drum") || lowerName.contains("kit") || lowerName.contains("perc");
    }

    /**
     * Get default drum kit instruments
     *
     * @return List of default drum instruments
     */
    public List<InstrumentWrapper> getDefaultDrumKit() {
        List<InstrumentWrapper> defaultKit = new ArrayList<>();

        try {
            // Get all drum instruments
            List<InstrumentWrapper> allDrums = getDrumInstruments();

            // If no instruments found, return empty list
            if (allDrums.isEmpty()) {
                logger.warn("No instruments found for default drum kit");
                return defaultKit;
            }

            // Define preferred drum names for a standard kit
            String[] standardDrumNames = {
                    "Kick", "Snare", "Closed Hi-hat", "Open Hi-hat",
                    "Tom Low", "Tom Mid", "Tom High", "Crash",
                    "Ride", "Clap", "Rim", "Percussion",
                    "Cowbell", "Conga", "Shaker", "Tambourine"
            };

            // Try to find drums that match these standard names
            for (String drumName : standardDrumNames) {
                boolean found = false;

                // First try exact matches
                for (InstrumentWrapper instr : allDrums) {
                    if (instr.getName().equalsIgnoreCase(drumName) ||
                            instr.getName().contains(drumName)) {
                        defaultKit.add(instr);
                        found = true;
                        break;
                    }
                }

                // If not found, try partial matches
                if (!found) {
                    String lowerDrumName = drumName.toLowerCase();
                    for (InstrumentWrapper instr : allDrums) {
                        String lowerInstrName = instr.getName().toLowerCase();
                        if (lowerInstrName.contains(lowerDrumName.split(" ")[0])) {
                            defaultKit.add(instr);
                            found = true;
                            break;
                        }
                    }
                }

                // Still not found, add any drum
                if (!found && !allDrums.isEmpty()) {
                    // Take the first available
                    defaultKit.add(allDrums.get(0));
                    // Remove to avoid duplicates
                    allDrums.remove(0);
                }

                // If we've filled all 16 slots, stop
                if (defaultKit.size() >= SequencerConstants.DRUM_PAD_COUNT) {
                    break;
                }
            }

            // Fill remaining slots if needed
            while (defaultKit.size() < SequencerConstants.DRUM_PAD_COUNT && !allDrums.isEmpty()) {
                defaultKit.add(allDrums.get(0));
                allDrums.remove(0);
            }

            logger.info("Created default drum kit with {} instruments", defaultKit.size());
            return defaultKit;
        } catch (Exception e) {
            logger.error("Error creating default drum kit: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

}
