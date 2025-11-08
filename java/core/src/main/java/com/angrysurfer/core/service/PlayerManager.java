package com.angrysurfer.core.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.PlayerInstrumentChangeEvent;
import com.angrysurfer.core.event.PlayerPresetChangeEvent;
import com.angrysurfer.core.event.PlayerRefreshEvent;
import com.angrysurfer.core.event.PlayerRuleUpdateEvent;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.SequencerConstants;

import lombok.Getter;
import lombok.Setter;

/**
 * The central source of truth for all player-related operations.
 */
@Getter
@Setter
public class PlayerManager implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
    private static PlayerManager instance;
    private final Map<Long, Player> playerCache = new HashMap<>();
    private final RedisService redisService;

    private PlayerManager() {
        this.redisService = RedisService.getInstance();
        // Keep constructor lightweight; call initialize() from App to register events
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
     * Explicit initialization entrypoint for PlayerManager. Registers for
     * command events. Call during application startup when ordering is known.
     */
    public synchronized void initialize() {
        registerForEvents();
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
                        Player player = event.getPlayer();

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

    public void initializePlayer(Player player) {
        if (player != null && player.getInstrument() != null) {
            applyInstrumentPreset(player);
            CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
        }
    }

    private void handlePlayerSelectionEvent(Command action) {
//        if (action.getData() instanceof PlayerSelectionEvent event && event.getPlayer() != null) {
//            logger.info("Player selected for UI: {} (ID: {})",
//                    event.getPlayer().getName(), event.getPlayerId());
//            CommandBus.getInstance().publish(Commands.PLAYER_ACTIVATED, this, event.getPlayer());
//        }
    }

    private void handlePlayerPresetChangeEvent(PlayerPresetChangeEvent event) {
        Player player = event.getPlayer();
        Integer presetNumber = event.getPresetNumber();
        Integer bankIndex = event.getBankIndex();

        if (player == null || player.getInstrument() == null || presetNumber == null) {
            logger.warn("Invalid preset change event - missing required data");
            return;
        }

        InstrumentWrapper instrument = player.getInstrument();
        
        if (instrument.getSoundbankName() != null) {
            SoundbankService.getInstance().applySoundbank(instrument, instrument.getSoundbankName());
        }

        if (bankIndex != null) instrument.setBankIndex(bankIndex);
        if (presetNumber != null) instrument.setPreset(presetNumber);

        if (applyInstrumentPreset(player)) {
            if (Boolean.TRUE.equals(player.getIsDefault())) {
                UserConfigManager.getInstance().updateDefaultPlayer(player);
                if (Boolean.TRUE.equals(instrument.getIsDefault())) {
                    UserConfigManager.getInstance().updateDefaultInstrument(instrument);
                }
            } else {
                savePlayerProperties(player);
            }

            CommandBus.getInstance().publish(Commands.PLAYER_PRESET_CHANGED, this, player);
            CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
        }
    }

    private void handlePlayerInstrumentChangeEvent(PlayerInstrumentChangeEvent event) {
        Player player = event.getPlayer();
        InstrumentWrapper instrument = event.getInstrument();

        if (player == null || instrument == null) {
            logger.warn("Invalid instrument change event - missing player or instrument");
            return;
        }

        player.setInstrument(instrument);
        player.setInstrumentId(instrument.getId());
        applyInstrumentPreset(player);

        if (Boolean.TRUE.equals(player.getIsDefault())) {
            UserConfigManager.getInstance().updateDefaultPlayer(player.deepCopy());
            if (Boolean.TRUE.equals(instrument.getIsDefault())) {
                UserConfigManager.getInstance().updateDefaultInstrument(instrument);
            }
        } else {
            savePlayerProperties(player);
        }

        CommandBus.getInstance().publish(Commands.PLAYER_INSTRUMENT_CHANGED, this,
                new Object[]{player.getId(), instrument.getId()});
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
    }

    private void handlePlayerRefreshEvent(Command action) {
        if (action.getData() instanceof PlayerRefreshEvent event && event.getPlayer() != null) {
            Player player = event.getPlayer();

            if (player.getInstrument() != null) {
                logger.info("Refreshing preset for player: {} (ID: {})",
                        player.getName(), player.getId());

                applyInstrumentPreset(player);
            }
        }
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
            player = redisService.findPlayerById(id);
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
                if (!Boolean.TRUE.equals(player.getInstrument().getIsDefault())) {
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

    public boolean applyInstrumentPreset(Player player) {
        if (player == null || player.getInstrument() == null) return false;

        InstrumentWrapper instrument = player.getInstrument();
        int channel = player.getChannel();
        Integer bankIndex = instrument.getBankIndex();
        Integer preset = instrument.getPreset();

        logger.debug("Applying preset for {} on channel {}: bank={}, program={}",
                player.getName(), channel, bankIndex, preset);

        return MidiService.getInstance().applyPreset(instrument, channel, bankIndex, preset);
    }

    public void applyPlayerInstrument(Player player) {
        applyInstrumentPreset(player);
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
        if (player == null) return;

        try {
            InstrumentWrapper instrument = InstrumentManager.getInstance()
                    .getOrCreateInternalSynthInstrument(player.getChannel(), exclusive, tag);

            if (instrument != null) {
                player.setInstrument(instrument);
                player.setInstrumentId(instrument.getId());
                player.setUsingInternalSynth(true);
                savePlayerProperties(player);
                applyInstrumentPreset(player);
                logger.info("Player {} initialized with internal instrument", player.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize internal instrument", e);
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
    public void handleRuleUpdate(Player player, Rule rule, PlayerRuleUpdateEvent.RuleUpdateType updateType) {
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
            redisService.savePlayer(player);

            logger.info("Saved rules for player {}", player.getId());
        } catch (Exception e) {
            logger.error("Error saving player rules: {}", e.getMessage(), e);
        }
    }
}
