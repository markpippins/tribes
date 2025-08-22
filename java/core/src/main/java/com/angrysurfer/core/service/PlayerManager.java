package com.angrysurfer.core.service;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.midi.MidiControlMessageEnum;
import com.angrysurfer.core.event.*;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.SequencerConstants;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.Synthesizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The central source of truth for all player-related operations.
 */
@Getter
@Setter
public class PlayerManager implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
    private static PlayerManager instance;
    private final Map<Long, Player> playerCache = new HashMap<>();

    private PlayerManager() {
        registerForEvents();
    }

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    /**
     * Register for specific player-related events only
     */
    private void registerForEvents() {
        // Register only for commands we actually handle
        CommandBus.getInstance().register(this, new String[]{
                Commands.DRUM_PAD_SELECTED,
                Commands.PLAYER_SELECTION_EVENT,
                Commands.PLAYER_UPDATE_EVENT,
                Commands.PLAYER_PRESET_CHANGE_EVENT,
                Commands.PLAYER_INSTRUMENT_CHANGE_EVENT,
                Commands.PLAYER_REFRESH_EVENT,
                Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST,
                Commands.REFRESH_ALL_INSTRUMENTS
        });

        logger.info("PlayerManager registered for specific events");
    }

    /**
     * Update handler in onAction method to handle player update events
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        try {
            switch (action.getCommand()) {
                case Commands.PLAYER_SELECTION_EVENT -> handlePlayerSelectionEvent(action);
                // case Commands.DRUM_PAD_SELECTED -> CommandBus.getInstance().publish(Commands.PLAYER_SELECTION_EVENT, this, action.getData());
                case Commands.PLAYER_UPDATE_EVENT -> {
                    if (action.getData() instanceof PlayerUpdateEvent event) {
                        Player player = event.player();

                        // Check if this is a default player that needs to be saved to UserConfig
                        if (Boolean.TRUE.equals(player.getIsDefault())) {
                            logger.info("Handling update for default player: {}", player.getName());
                            // Update the player in UserConfig
                            UserConfigManager.getInstance().updateDefaultPlayer(player);

                            // Also update the player cache
                            playerCache.put(player.getId(), player);
                        } else {
                            // Handle regular player update
                            savePlayerProperties(player);
                        }
                    }
                }
                case Commands.PLAYER_PRESET_CHANGE_EVENT -> {
                    if (action.getData() instanceof PlayerPresetChangeEvent event) {
                        handlePlayerPresetChangeEvent(event);
                    }
                }
                case Commands.PLAYER_INSTRUMENT_CHANGE_EVENT -> {
                    if (action.getData() instanceof PlayerInstrumentChangeEvent event) {
                        handlePlayerInstrumentChangeEvent(event);
                    }
                }
                case Commands.PLAYER_REFRESH_EVENT -> handlePlayerRefreshEvent(action);
                case Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST -> handleLegacyInstrumentChangeRequest(action);
                case Commands.REFRESH_ALL_INSTRUMENTS -> handleLegacyRefreshRequest(action);
            }
        } catch (Exception e) {
            logger.error("Error processing player action: {}", e.getMessage(), e);
        }
    }

//    public void initializePlayer(Player player) {
//        PlayerManager.getInstance().applyInstrumentPreset(player);
//
//        // Add this explicit program change to ensure the preset is applied:
//        if (player != null && player.getInstrument() != null) {
//            try {
//                // Force program change through both regular channel and direct MIDI
//                InstrumentWrapper instrument = player.getInstrument();
//                int channel = player.getChannel();
//                int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
//                int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
//
//                player.getInstrument().controlChange(0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON);
//                player.getInstrument().controlChange(32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON);
//                player.getInstrument().programChange(preset, 0);
//
//
//                CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
//                logger.info("Explicitly set instrument {} to bank {} program {} on channel {}",
//                        instrument.getName(), bankIndex, preset, channel);
//            } catch (Exception e) {
//                logger.error("Error applying program change: {}", e.getMessage(), e);
//            }
//        }
//    }

    private void handlePlayerSelectionEvent(Command action) {
//        if (action.getData() instanceof PlayerSelectionEvent event && event.player() != null) {
//            logger.info("Player selected for UI: {} (ID: {})",
//                    event.player().getName(), event.player()Id());
//            CommandBus.getInstance().publish(Commands.PLAYER_ACTIVATED, this, event.player());
//        }
    }

    /**
     * Handle player preset change events with special handling for default
     * players
     */
    private void handlePlayerPresetChangeEvent(PlayerPresetChangeEvent event) {
        Player player = event.player();
        Integer presetNumber = event.presetNumber();
        Integer bankIndex = event.bankIndex();

        if (player == null || player.getInstrument() == null || presetNumber == null) {
            logger.warn("Invalid preset change event - missing required data");
            return;
        }

        // Update the instrument preset
        InstrumentWrapper instrument = player.getInstrument();
        logger.info("Processing preset change for {} - bank:{}, preset:{}, soundbank:{}",
                player.getName(), bankIndex, presetNumber, instrument.getSoundBank());        // The soundbank should already be applied by SoundParametersPanel
        // We'll just ensure the preset change is applied, not re-applying the soundbank
        // This avoids duplicate soundbank application which could cause hanging
        logger.info("Applying preset change with existing soundbank: {}",
                instrument.getSoundBank());

        // Update the instrument preset
        if (bankIndex != null) {
            instrument.setBankIndex(bankIndex);
        }
        if (presetNumber != null) {
            instrument.setPreset(presetNumber);
        }

        // Apply the preset change with additional log output
        boolean success = SoundbankManager.getInstance().applyInstrumentPreset(player);
        logger.info("Applied preset change: {}", success ? "SUCCESS" : "FAILED");

        if (success) {
            // If this is a default player, update in UserConfig
            if (Boolean.TRUE.equals(player.getIsDefault())) {
                logger.info("Updating default player preset: {} -> Bank: {}, Preset: {}",
                        player.getName(), bankIndex, presetNumber);

                // Update default player
                UserConfigManager.getInstance().updateDefaultPlayer(player);

                // Also update default instrument if it's a default instrument
                if (Boolean.TRUE.equals(instrument.getDefaultInstrument())) {
                    UserConfigManager.getInstance().updateDefaultInstrument(instrument);
                }
            } else {
                // Regular player - save to player storage
                savePlayerProperties(player);
            }

            // Notify listeners
            CommandBus.getInstance().publish(Commands.PLAYER_PRESET_CHANGED, this, player);
            CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
        }
    }

    /**
     * Apply instrument changes to the player and save
     */
    private void handlePlayerInstrumentChangeEvent(PlayerInstrumentChangeEvent event) {
        Player player = event.player();
        InstrumentWrapper instrument = event.instrument();

        if (player == null || instrument == null) {
            logger.warn("Invalid instrument change event - missing player or instrument");
            return;
        }

        // Capture the original values for logging
        Long oldInstrumentId = player.getInstrumentId();
        String oldInstrumentName = player.getInstrument() != null ? player.getInstrument().getName() : "none";

        // Update player with new instrument
        player.setInstrument(instrument);
        player.setInstrumentId(instrument.getId());

        // Apply the instrument preset
        boolean presetApplied = SoundbankManager.getInstance().applyInstrumentPreset(player);

        // Check if this is a default player that needs special handling
        if (Boolean.TRUE.equals(player.getIsDefault())) {
            logger.info("Updating default player instrument: {} -> {} (success: {})",
                    player.getName(), instrument.getName(), presetApplied);

            // Get the UserConfigManager instance directly
            UserConfigManager configManager = UserConfigManager.getInstance();

            // Create a clean copy to avoid reference issues
            Player playerCopy = player.deepCopy();

            // Update in UserConfig and verify success
            boolean updated = configManager.updateDefaultPlayer(playerCopy);

            if (!updated) {
                logger.error("Failed to update default player in UserConfig: {}", player.getName());
            } else {
                logger.info("Successfully updated default player in UserConfig: {}", player.getName());
            }

            // Also update default instrument if it's a default instrument
            if (Boolean.TRUE.equals(instrument.getDefaultInstrument())) {
                boolean instrumentUpdated = configManager.updateDefaultInstrument(instrument);
                if (!instrumentUpdated) {
                    logger.error("Failed to update default instrument in UserConfig: {}", instrument.getName());
                }
            }
        } else {
            // Regular player - save to player storage
            savePlayerProperties(player);
        }

        // Log change details for debugging
        logger.info("Player instrument changed: {} - {} ({}) -> {} ({})",
                player.getId(), oldInstrumentName, oldInstrumentId,
                instrument.getName(), instrument.getId());

        // Notify listeners about the change
        CommandBus.getInstance().publish(Commands.PLAYER_INSTRUMENT_CHANGED, this,
                new Object[]{player.getId(), instrument.getId()});
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
    }

    private void handlePlayerRefreshEvent(Command action) {
        if (action.getData() instanceof PlayerRefreshEvent event && event.player() != null) {
            Player player = event.player();

            if (player.getInstrument() != null) {
                logger.info("Refreshing preset for player: {} (ID: {})",
                        player.getName(), player.getId());

                SoundbankManager.getInstance().applyInstrumentPreset(player);
            }
        }
    }

    @Deprecated
    private void handleLegacyPlayerActivationRequest(Command action) {
        if (action.getData() instanceof Player player)
            CommandBus.getInstance().publish(Commands.PLAYER_SELECTION_EVENT, this, player);
    }

    @Deprecated
    private void handleLegacyInstrumentChangeRequest(Command action) {
        if (action.getData() instanceof Object[] data && data.length >= 2) {
            Long playerId = (Long) data[0];
            InstrumentWrapper instrument = (InstrumentWrapper) data[1];

            Player player = getPlayerById(playerId);
            if (player != null && instrument != null) {
                PlayerInstrumentChangeEvent event = new PlayerInstrumentChangeEvent(this, player, instrument);
                CommandBus.getInstance().publish(Commands.PLAYER_INSTRUMENT_CHANGE_EVENT, this, event);
            }
        }
    }

    @Deprecated
    private void handleLegacyRefreshRequest(Command action) {
        if (action.getData() instanceof Player player) {
            PlayerRefreshEvent event = new PlayerRefreshEvent(this, player);
            CommandBus.getInstance().publish(Commands.PLAYER_REFRESH_EVENT, this, event);
        }
    }

    public Player getPlayerById(Long id) {
        if (id == null) {
            return null;
        }

        Player player = playerCache.get(id);

        if (player == null) {
            player = RedisService.getInstance().findPlayerById(id);
            if (player != null) {
                playerCache.put(id, player);
            }
        }

        return player;
    }

    public void savePlayerProperties(Player player) {
        if (player == null) {
            logger.warn("Cannot save null player");
            return;
        }

        // Skip saving default players
        if (Boolean.TRUE.equals(player.getIsDefault())) {
            logger.debug("Skipping save for default player: {} (ID: {})", player.getName(), player.getId());
            return;
        }

        try {
            logger.info("Saving player: {} (ID: {})", player.getName(), player.getId());

            if (player.getInstrument() != null) {
                // Skip saving default instruments
                if (!Boolean.TRUE.equals(player.getInstrument().getDefaultInstrument())) {
                    RedisService.getInstance().saveInstrument(player.getInstrument());
                    player.setInstrumentId(player.getInstrument().getId());

                    logger.debug("Saved instrument: {} (ID: {}) with preset {}",
                            player.getInstrument().getName(),
                            player.getInstrument().getId(),
                            player.getInstrument().getPreset());
                } else {
                    logger.debug("Skipping save for default instrument: {}",
                            player.getInstrument().getName());
                }
            }

            playerCache.put(player.getId(), player);

            logger.debug("Player saved successfully");
        } catch (Exception e) {
            logger.error("Error saving player: {}", e.getMessage(), e);
        }
    }


    /**
     * Apply a player's instrument settings to MIDI system
     */
    public void applyPlayerInstrument(Player player) {
        if (player == null || player.getInstrument() == null) {
            return;
        }

        try {
            InstrumentWrapper instrument = player.getInstrument();

            // Check if this is an internal synth instrument
            if (InternalSynthManager.getInstance().isInternalSynthInstrument(instrument)) {
                // Use InternalSynthManager for internal instruments
                InternalSynthManager.getInstance().initializeInstrumentState(instrument);
            } else {
                // Use standard method for external instruments
                if (instrument.getBankIndex() != null && instrument.getPreset() != null) {
                    instrument.controlChange(0, (instrument.getBankIndex() >> 7) & MidiControlMessageEnum.POLY_MODE_ON);
                    instrument.controlChange(32, instrument.getBankIndex() & MidiControlMessageEnum.POLY_MODE_ON);
                    instrument.programChange(instrument.getPreset(), 0);
                }
            }

            logger.debug("Applied instrument settings for player {}", player.getName());
        } catch (Exception e) {
            logger.error("Error applying player instrument: {}", e.getMessage(), e);
        }
    }

    public Rule addRule(Player player, int beat, int equals, double v, int i) {
        return null;
    }

    public void removeRule(Player player, Long ruleId) {
    }

    public Player updatePlayer(Session session, Long playerId, int updateType, int updateValue) {
        return null;
    }

    public Set<Player> removePlayer(Session session, Long playerId) {
        return Collections.emptySet();
    }

    public void clearPlayers(Session session) {
    }

    public void clearPlayersWithNoRules(Session session) {
    }

    public void removeAllPlayers(Session session) {
    }

    /**
     * Apply a player's preset to MIDI system
     */
    public void applyPlayerPreset(Player player) {
        if (player == null || player.getInstrument() == null) {
            return;
        }

        try {
            InstrumentWrapper instrument = player.getInstrument();

            // Get device type and ensure it's open
            boolean isInternalSynth = InternalSynthManager.getInstance().isInternalSynthInstrument(instrument);
            boolean deviceOpen = false;

            if (instrument.getDevice() != null) {
                if (!instrument.getDevice().isOpen()) {
                    try {
                        instrument.getDevice().open();
                    } catch (Exception e) {
                        logger.warn("Could not open device: {}", e.getMessage());
                    }
                }
                deviceOpen = instrument.getDevice().isOpen();
            }

            // Ensure receiver is available
            if (instrument.getReceiver() == null && instrument.getDevice() != null && instrument.getDevice().isOpen()) {
                try {
                    instrument.setReceiver(instrument.getDevice().getReceiver());
                } catch (Exception e) {
                    logger.warn("Could not get receiver: {}", e.getMessage());
                }
            }

            // Debug info
            logger.info(
                    "Applying preset for {} on channel {}: bank={}, program={}, device={}, isInternal={}, deviceOpen={}",
                    player.getName(), player.getChannel(),
                    instrument.getBankIndex(), instrument.getPreset(),
                    instrument.getDeviceName(), isInternalSynth, deviceOpen);

            // For internal synth, use InternalSynthManager for best reliability
            if (isInternalSynth) {
                InternalSynthManager.getInstance().updateInstrumentPreset(
                        instrument,
                        instrument.getBankIndex(),
                        instrument.getPreset());

                // Double-check by directly accessing the Java Synthesizer
                try {
                    Synthesizer synth = InternalSynthManager.getInstance().getSynthesizer();
                    if (synth != null && synth.isOpen()) {
                        int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
                        int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
                        int channel = player.getChannel();

                        // Get the MidiChannel for this instrument's channel
                        javax.sound.midi.MidiChannel[] channels = synth.getChannels();
                        if (channels != null && channel < channels.length) {
                            // Send bank select and program change directly to the MidiChannel
                            channels[channel].controlChange(0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON); // Bank MSB
                            channels[channel].controlChange(32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON); // Bank LSB
                            channels[channel].programChange(preset);

                            logger.info("Directly applied program change to synth channel {}: bank={}, program={}",
                                    channel, bankIndex, preset);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not apply direct synth program change: {}", e.getMessage());
                }
            } else {
                // Use standard method for external instruments
                int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
                int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
                int channel = player.getChannel();

                try {
                    // Apply bank and program changes through the instrument
                    instrument.controlChange(0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON); // Bank MSB
                    instrument.controlChange(32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON); // Bank LSB
                    instrument.programChange(preset, 0);

                    // Also try alternate way with raw MIDI messages if available
                    if (instrument.getReceiver() != null) {
                        // Bank select MSB
                        javax.sound.midi.ShortMessage bankMSB = new javax.sound.midi.ShortMessage();
                        bankMSB.setMessage(0xB0 | channel, 0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON);
                        instrument.getReceiver().send(bankMSB, -1);

                        // Bank select LSB
                        javax.sound.midi.ShortMessage bankLSB = new javax.sound.midi.ShortMessage();
                        bankLSB.setMessage(0xB0 | channel, 32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON);
                        instrument.getReceiver().send(bankLSB, -1);

                        // Program change
                        javax.sound.midi.ShortMessage pc = new javax.sound.midi.ShortMessage();
                        pc.setMessage(0xC0 | channel, preset, 0);
                        instrument.getReceiver().send(pc, -1);

                        logger.info("Sent raw MIDI program change messages to channel {}", channel);
                    }
                } catch (Exception e) {
                    logger.warn("Error sending MIDI program change: {}", e.getMessage());
                }
            }

            logger.info("Applied preset for player: {}", player.getName());
        } catch (Exception e) {
            logger.error("Error applying player preset: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensures that all players have consistent channel assignments and resolves
     * any potential channel conflicts
     */
    public void ensureChannelConsistency() {
        logger.info("Ensuring channel consistency across all players");

        // Reset the channel manager's state
        ChannelManager channelManager = ChannelManager.getInstance();

        // First pass: collect all channel assignments
        Map<Integer, Long> channelToPlayerId = new HashMap<>();
        Map<Integer, Integer> channelConflicts = new HashMap<>();

        // Check for conflicts
        for (Player player : playerCache.values()) {
            if (player != null && player.getChannel() != null) {
                Integer channel = player.getChannel();

                // Skip drum channel 9 which can have multiple assignments
                if (channel == SequencerConstants.MIDI_DRUM_CHANNEL) {
                    continue;
                }

                if (channelToPlayerId.containsKey(channel)) {
                    // Conflict detected - track it
                    channelConflicts.put(channel, channelConflicts.getOrDefault(channel, 1) + 1);
                    logger.warn("Channel conflict detected for channel {}: players {} and {}",
                            channel, channelToPlayerId.get(channel), player.getId());
                } else {
                    // First player using this channel
                    channelToPlayerId.put(channel, player.getId());

                    // Reserve this channel
                    channelManager.reserveChannel(channel);
                }
            }
        }

        // Second pass: resolve conflicts if any
        if (!channelConflicts.isEmpty()) {
            logger.info("Resolving {} channel conflicts", channelConflicts.size());

            for (Player player : playerCache.values()) {
                if (player != null && player.getChannel() != null) {
                    Integer channel = player.getChannel();

                    // Skip drum channel
                    if (channel == SequencerConstants.MIDI_DRUM_CHANNEL) {
                        continue;
                    }

                    // If this player's channel has a conflict
                    if (channelConflicts.containsKey(channel)) {
                        // Only reassign if this isn't the first player that claimed the channel
                        if (!player.getId().equals(channelToPlayerId.get(channel))) {
                            // Assign a new channel
                            int newChannel = channelManager.getNextAvailableMelodicChannel();

                            logger.info("Reassigning player {} from channel {} to channel {}",
                                    player.getId(), channel, newChannel);

                            player.setDefaultChannel(newChannel);

                            // Save the updated player
                            savePlayerProperties(player);

                            // Update any associated instruments
                            if (player.getInstrument() != null) {
                                applyPlayerInstrument(player);
                            }
                        }
                    }
                }
            }
        }

        // Final pass: ensure all players have valid channels
        for (Player player : playerCache.values()) {
            if (player != null && (player.getChannel() == null
                    || (!player.isDrumPlayer() && player.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL))) {
                // Assign an appropriate channel
                int newChannel = player.isDrumPlayer() ? 9 : channelManager.getNextAvailableMelodicChannel();

                logger.info("Assigning channel {} to player {} with missing/invalid channel",
                        newChannel, player.getId());

                player.setDefaultChannel(newChannel);

                // Save the updated player
                savePlayerProperties(player);

                // Update any associated instruments
                if (player.getInstrument() != null) {
                    applyPlayerInstrument(player);
                }
            }
        }

        logger.info("Channel consistency check completed");
    }

    public void initializeInternalInstrument(Player player, boolean exclusive, int tag) {
        if (player == null) {
            logger.warn("Cannot initialize internal instrument for null player");
            return;
        }

        try {
            // Get an internal instrument from InstrumentManager
            InstrumentWrapper internalInstrument;
            if (player.getInstrumentId() != null)
                internalInstrument = InstrumentManager.getInstance().getInstrumentCache().get(player.getInstrumentId());
            else
                internalInstrument = InternalSynthManager.getInstance().createInternalInstrument(player.getName(),
                        player.getDefaultChannel(), DeviceManager.getMidiDevice(SequencerConstants.GERVILL));

            if (internalInstrument != null) {
                // Assign to player
                player.setInstrument(internalInstrument);
                player.setInstrumentId(internalInstrument.getId());
                player.setUsingInternalSynth(true);

                // Save the player
                savePlayerProperties(player);

                // Initialize the instrument state
                InternalSynthManager.getInstance().initializeInstrumentState(internalInstrument);

                logger.info("Player {} initialized with internal instrument", player.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize internal instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle a rule update and broadcast an event
     *
     * @param player     The player whose rules are updated
     * @param rule       The specific rule that was modified (can be null for bulk
     *                   operations)
     * @param updateType The type of update that occurred
     */
    public void handleRuleUpdate(Player player, Rule rule, RuleUpdateType updateType) {
        if (player == null) {
            logger.warn("Cannot handle rule update - player is null");
            return;
        }

        // Save the player state
        savePlayerRules(player);

        // Create and publish the rule update event
        PlayerRuleUpdateEvent event = new PlayerRuleUpdateEvent(player, rule, updateType);
        CommandBus.getInstance().publish(Commands.PLAYER_RULE_UPDATE_EVENT, this, event);

        // Also publish a player update event for backward compatibility
        CommandBus.getInstance().publish(
                Commands.PLAYER_UPDATE_EVENT,
                this,
                new PlayerUpdateEvent(this, player)
        );
    }

    /**
     * Save the rules for a player
     *
     * @param player The player whose rules to save
     */
    public void savePlayerRules(Player player) {
        if (player == null) {
            logger.warn("Cannot save rules - player is null");
            return;
        }

        try {
            // Update the player in our cache
            playerCache.put(player.getId(), player);

            // Update the player in the session
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null) {
                session.addOrUpdatePlayer(player);
            }

            // Persist to storage
            RedisService.getInstance().savePlayer(player);

            logger.info("Saved rules for player {}", player.getId());
        } catch (Exception e) {
            logger.error("Error saving player rules: {}", e.getMessage(), e);
        }
    }
}
