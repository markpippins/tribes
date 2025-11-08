package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentManager implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentManager.class);
    private static InstrumentManager instance;
    private final InstrumentHelper instrumentHelper;
    private final Map<Long, InstrumentWrapper> instrumentCache = new HashMap<>();

    private List<MidiDevice> midiDevices = new ArrayList<>();
    private List<String> devices = new ArrayList<>();
    private boolean needsRefresh = true;
    private boolean isInitializing = false;

    // Private constructor for singleton pattern
    private InstrumentManager() {
    this.instrumentHelper = RedisService.getInstance().getInstrumentHelper();
    // Keep constructor lightweight; heavy work moved to initialize().
    }

    // Static method to get the singleton instance
    public static synchronized InstrumentManager getInstance() {
        if (instance == null) {
            instance = new InstrumentManager();
        }
        return instance;
    }

    private static InstrumentWrapper getInternalInstrument(int channel) {
        InstrumentWrapper internalInstrument = new InstrumentWrapper(
                "Internal Synth",
                null, // Internal synth uses null device
                channel);

        // Configure as internal instrument
        internalInstrument.setInternal(true);
        internalInstrument.setDeviceName(SequencerConstants.GERVILL);
        internalInstrument.setSoundbankName("Default");
        internalInstrument.setBankIndex(0);
        internalInstrument.setPreset(0); // Piano
        internalInstrument.setId(9985L + channel);
        return internalInstrument;
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        switch (action.getCommand()) {
            case Commands.USER_CONFIG_LOADED -> {
                // Check if we're already initializing to prevent recursion
                if (!isInitializing) {
                    refreshInstruments();
                } else {
                    logger.debug("Skipping refresh during initialization");
                }
            }
            case Commands.INSTRUMENT_UPDATED -> {
                // Update single instrument in cache
                if (action.getData() instanceof InstrumentWrapper instrument) {
                    instrumentCache.put(instrument.getId(), instrument);
                    logger.info("Updated instrument in cache: {}", instrument.getName());
                }
            }

            case Commands.INSTRUMENTS_REFRESHED -> {
                // Force cache refresh
                refreshInstruments();
            }
        }
    }

    public void initializeCache() {
        logger.info("Initializing instrument cache");

        // Set flag to prevent recursion
        isInitializing = true;

        try {
            // Get instruments from UserConfigManager which is the source of truth
            List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();
            instrumentCache.clear();

            if (instruments != null) {
                for (InstrumentWrapper instrument : instruments) {
                    instrumentCache.put(instrument.getId(), instrument);
                }
                logger.info("Cached {} instruments", instrumentCache.size());
            } else {
                logger.warn("No instruments found in UserConfigManager");
            }

            needsRefresh = false;
        } finally {
            // Always reset the flag when done
            isInitializing = false;
        }
    }

    public void refreshInstruments() {
        // Skip if we're already initializing
        if (isInitializing) {
            logger.debug("Skipping recursive refresh call");
            return;
        }

        logger.info("Refreshing instruments cache");
        initializeCache();
        needsRefresh = false;
    }

    /**
     * Explicit initialization entrypoint. Registers the manager for commands
     * and performs the initial cache load. Call during application startup.
     */
    public synchronized void initialize() {
        // Register for command events
        CommandBus.getInstance().register(this, new String[]{
                Commands.INSTRUMENT_UPDATED, Commands.INSTRUMENTS_REFRESHED, Commands.USER_CONFIG_LOADED
        });

        // Initial cache load
        refreshInstruments();
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
                        logger.warn("Error checking if instrument receives on channel {}: {}",
                                channel, e.getMessage());

                        // Check channel directly as fallback
                        return instrument.getChannel() != null &&
                                instrument.getChannel() == channel;
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

        // Check cache first
        InstrumentWrapper instrument = instrumentCache.get(id);

        // If not in cache, try to load from Redis
        if (instrument == null) {
            logger.info("Instrument with ID {} not found in cache, checking database", id);
            try {
                instrument = RedisService.getInstance().getInstrumentById(id);

                // Add to cache if found
                if (instrument != null) {
                    logger.info("Found instrument in database: {} (ID: {})",
                            instrument.getName(), instrument.getId());
                    instrumentCache.put(id, instrument);
                } else {
                    logger.warn("Instrument with ID {} not found in database", id);
                }
            } catch (Exception e) {
                logger.error("Error retrieving instrument with ID {}: {}", id, e.getMessage(), e);
            }
        } else {
            logger.debug("Found instrument in cache: {} (ID: {})",
                    instrument.getName(), instrument.getId());
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
        return instrumentCache.values().stream()
                .filter(i -> i.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public InstrumentWrapper getInstrumentFromCache(Long instrumentId) {
        if (needsRefresh) {
            refreshInstruments();
        }

        InstrumentWrapper instrument = instrumentCache.get(instrumentId);
        if (instrument == null) {
            logger.warn("Cache miss for instrument ID: {}, refreshing cache", instrumentId);
            refreshInstruments();
            instrument = instrumentCache.get(instrumentId);
        }
        return instrument;
    }

    public List<InstrumentWrapper> getCachedInstruments() {
        if (instrumentCache.isEmpty()) {
            logger.info("Cache is empty, initializing...");
            initializeCache();
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
            // Check if this is a default instrument
            if (Boolean.TRUE.equals(instrument.getIsDefault())) {
                logger.info("Updating default instrument in UserConfig: {}", instrument.getName());
                // Save to UserConfig
                UserConfigManager.getInstance().updateDefaultInstrument(instrument);
            } else {
                // Regular instrument - save to instrument storage
                RedisService.getInstance().saveInstrument(instrument);
            }

            // Update the in-memory cache
            instrumentCache.put(instrument.getId(), instrument);

            // Notify listeners
            CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, instrument);

        } catch (Exception e) {
            logger.error("Error updating instrument: {}", e.getMessage(), e);
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

        // Get instrument for logging before removal
        InstrumentWrapper instrument = instrumentCache.get(instrumentId);
        String name = instrument != null ? instrument.getName() : "Unknown";

        // Remove from cache
        instrumentCache.remove(instrumentId);

        // Remove from UserConfigManager
        UserConfigManager.getInstance().removeInstrument(instrumentId);

        logger.info("Instrument removed: {} (ID: {})", name, instrumentId);
    }

    /**
     * Find or create an internal instrument for the specified channel
     *
     * @param channel The MIDI channel
     * @return An InstrumentWrapper for the internal synth
     */
    public InstrumentWrapper findOrCreateInternalInstrument(int channel) {
        // Try to find an existing internal instrument for this channel
        for (InstrumentWrapper instrument : getCachedInstruments()) {
            if (Boolean.TRUE.equals(instrument.getInternal()) && !instrument.getAssignedToPlayer() &&
                    instrument.getChannel() == channel) {
                return instrument;
            }
        }

        // Create a new internal instrument
        InstrumentWrapper internalInstrument = getInternalInstrument(channel);

        // Add to cache and persist
        updateInstrument(internalInstrument);

        return internalInstrument;
    }

    /**
     * Get instrument for internal synthesizer on a specific channel
     *
     * @param channel MIDI channel
     * @return The instrument, creating it if necessary
     */
    public InstrumentWrapper getOrCreateInternalSynthInstrument(int channel, boolean exclusive, int tag) {
        // First try to find an existing instrument
        Long id = 9985L + tag;

        // Try to find by ID first
        InstrumentWrapper instrument = instrumentCache.get(id);
        if (instrument != null && (!exclusive || !instrument.getAssignedToPlayer())) {
            if (exclusive) {
                instrument.setAssignedToPlayer(true);
            }
            return instrument;
        }

        for (InstrumentWrapper cached : instrumentCache.values()) {
            if (MidiService.getInstance().isInternalSynth(cached) &&
                    cached.getChannel() != null &&
                    cached.getChannel() == channel &&
                    (!exclusive || !cached.getAssignedToPlayer())) {

                cached.setAssignedToPlayer(exclusive);
                return cached;
            }
        }

        boolean isDrumChannel = (channel == SequencerConstants.MIDI_DRUM_CHANNEL);
        String name = isDrumChannel ? "Internal Drums " + tag : "Internal Melo " + channel + "-" + tag;
        instrument = createInternalInstrument(name, channel);

        if (instrument != null) {
            // Store in our cache
            updateInstrument(instrument);

            // Mark as assigned if exclusive
            if (exclusive) {
                instrument.setAssignedToPlayer(true);
            }
        }

        return instrument;
    }

    /**
     * Refresh the instrument cache with the provided list of instruments
     */
    public void refreshCache(List<InstrumentWrapper> instruments) {
        if (instruments == null) return;

        // Clear the existing cache
        instrumentCache.clear();

        // Add all instruments to the cache
        for (InstrumentWrapper instrument : instruments) {
            if (instrument != null && instrument.getId() != null) {
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
                    owners.add("Melodic: " + sequencer.getClass().getName());
                }
            }

            // Check drum sequencers (which have multiple players)
            for (DrumSequencer sequencer : DrumSequencerManager.getInstance().getAllSequencers()) {
                if (sequencer != null && sequencer.getPlayers() != null) {
                    for (Player player : sequencer.getPlayers()) {
                        if (player != null &&
                                player.getInstrumentId() != null &&
                                player.getInstrumentId().equals(instrument.getId())) {
                            owners.add(sequencer.getClass().getName() + " (" + player.getName() + ")");
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
                return owners.getFirst() + " and " + (owners.size() - 1) + " more";
            }
        } catch (Exception e) {
            logger.error("Error determining instrument owner: {}", e.getMessage(), e);
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
            List<InstrumentWrapper> allInstruments = getCachedInstruments();

            // Filter for drum instruments (typically bank 128 or those with drum-related names)
            for (InstrumentWrapper instrument : allInstruments) {
                // Check if it's a drum bank (128) or has drum-related keywords in the name
                if (instrument.getBankIndex() == 128 ||
                        isDrumInstrumentByName(instrument.getName())) {
                    drumInstruments.add(instrument);
                }
            }

            // If no drum instruments found, return all instruments as fallback
            if (drumInstruments.isEmpty()) {
                logger.warn("No drum instruments found, returning all instruments");
                return allInstruments;
            }

            // Sort by name for easier selection
            drumInstruments.sort(Comparator.comparing(InstrumentWrapper::getName));

            logger.info("Found {} drum instruments", drumInstruments.size());
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
        String[] drumKeywords = {
                "drum", "kick", "snare", "tom", "cymbal", "hat", "clap", "rim",
                "percussion", "conga", "bongo", "tabla", "timbale", "wood", "stick",
                "shaker", "guiro", "bell", "tambourine", "triangle"
        };

        for (String keyword : drumKeywords) {
            if (lowerName.contains(keyword)) {
                return true;
            }
        }

        return false;
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
                    defaultKit.add(allDrums.getFirst());
                    // Remove to avoid duplicates
                    if (!allDrums.isEmpty()) {
                        allDrums.removeFirst();
                    }
                }

                // If we've filled all 16 slots, stop
                if (defaultKit.size() >= SequencerConstants.DRUM_PAD_COUNT) {
                    break;
                }
            }

            // Fill remaining slots if needed
            while (defaultKit.size() < SequencerConstants.DRUM_PAD_COUNT && !allDrums.isEmpty()) {
                defaultKit.add(allDrums.getFirst());
                allDrums.removeFirst();
            }

            logger.info("Created default drum kit with {} instruments", defaultKit.size());
            return defaultKit;
        } catch (Exception e) {
            logger.error("Error creating default drum kit: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private InstrumentWrapper createInternalInstrument(String name, int channel) {
        try {
            Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null) return null;

            InstrumentWrapper instrument = new InstrumentWrapper(name, synth, channel);
            instrument.setInternal(true);
            instrument.setInternalSynth(true);
            instrument.setDeviceName(SequencerConstants.GERVILL);
            instrument.setBankIndex(0);
            instrument.setPreset(0);
            instrument.setId(instrumentHelper.getNextInstrumentId());

            try {
                instrument.setReceiver(synth.getReceiver());
            } catch (Exception e) {
                logger.warn("Could not get receiver for internal instrument", e);
            }

            return instrument;
        } catch (Exception e) {
            logger.error("Error creating internal instrument", e);
            return null;
        }
    }

}
