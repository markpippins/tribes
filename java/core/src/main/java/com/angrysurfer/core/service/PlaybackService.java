package com.angrysurfer.core.service;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.*;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified playback service - manages players and instruments.
 * Replaces: PlayerManager, InstrumentManager
 */
public class PlaybackService implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);
    private static PlaybackService instance;

    private final Map<Long, Player> players = new HashMap<>();
    private final Map<Long, InstrumentWrapper> instruments = new HashMap<>();
    private final RedisService redisService;

    private PlaybackService() {
        this.redisService = RedisService.getInstance();
    }

    public static synchronized PlaybackService getInstance() {
        if (instance == null) {
            instance = new PlaybackService();
        }
        return instance;
    }

    public void initialize() {
        CommandBus.getInstance().register(this, new String[]{
                Commands.PLAYER_SELECTION_EVENT,
                Commands.PLAYER_UPDATE_EVENT,
                Commands.PLAYER_PRESET_CHANGE_EVENT,
                Commands.PLAYER_INSTRUMENT_CHANGE_EVENT,
                Commands.PLAYER_REFRESH_EVENT,
                Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST,
                Commands.REFRESH_ALL_INSTRUMENTS
        });
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) return;

        switch (action.getCommand()) {
            case Commands.PLAYER_UPDATE_EVENT -> {
                if (action.getData() instanceof PlayerUpdateEvent event) {
                    handlePlayerUpdate(event.getPlayer());
                }
            }
            case Commands.PLAYER_PRESET_CHANGE_EVENT -> {
                if (action.getData() instanceof PlayerPresetChangeEvent event) {
                    handlePresetChange(event);
                }
            }
            case Commands.PLAYER_INSTRUMENT_CHANGE_EVENT -> {
                if (action.getData() instanceof PlayerInstrumentChangeEvent event) {
                    handleInstrumentChange(event);
                }
            }
            case Commands.PLAYER_REFRESH_EVENT -> {
                if (action.getData() instanceof PlayerRefreshEvent event) {
                    applyPreset(event.getPlayer());
                }
            }
        }
    }

    // ========== Player Management ==========

    public Player getPlayer(Long id) {
        if (id == null) return null;
        
        Player player = players.get(id);
        if (player == null) {
            player = redisService.findPlayerById(id);
            if (player != null) {
                players.put(id, player);
            }
        }
        return player;
    }

    public void savePlayer(Player player) {
        if (player == null) return;
        
        if (Boolean.TRUE.equals(player.getIsDefault())) {
            logger.debug("Skipping save for default player: {}", player.getName());
            return;
        }

        try {
            if (player.getInstrument() != null && !Boolean.TRUE.equals(player.getInstrument().getIsDefault())) {
                redisService.saveInstrument(player.getInstrument());
                player.setInstrumentId(player.getInstrument().getId());
            }
            players.put(player.getId(), player);
        } catch (Exception e) {
            logger.error("Error saving player", e);
        }
    }

    public void initializePlayer(Player player) {
        if (player != null && player.getInstrument() != null) {
            applyPreset(player);
            CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
        }
    }

    public boolean applyPreset(Player player) {
        if (player == null || player.getInstrument() == null) return false;

        InstrumentWrapper instrument = player.getInstrument();
        int channel = player.getChannel();
        Integer bankIndex = instrument.getBankIndex();
        Integer preset = instrument.getPreset();

        return MidiService.getInstance().applyPreset(instrument, channel, bankIndex, preset);
    }

    private void handlePlayerUpdate(Player player) {
        if (Boolean.TRUE.equals(player.getIsDefault())) {
            UserConfigManager.getInstance().updateDefaultPlayer(player);
        } else {
            savePlayer(player);
        }
    }

    private void handlePresetChange(PlayerPresetChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.getInstrument() == null) return;

        InstrumentWrapper instrument = player.getInstrument();
        
        if (instrument.getSoundbankName() != null) {
            SoundbankService.getInstance().applySoundbank(instrument, instrument.getSoundbankName());
        }

        if (event.getBankIndex() != null) instrument.setBankIndex(event.getBankIndex());
        if (event.getPresetNumber() != null) instrument.setPreset(event.getPresetNumber());

        if (applyPreset(player)) {
            handlePlayerUpdate(player);
            CommandBus.getInstance().publish(Commands.PLAYER_PRESET_CHANGED, this, player);
            CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
        }
    }

    private void handleInstrumentChange(PlayerInstrumentChangeEvent event) {
        Player player = event.getPlayer();
        InstrumentWrapper instrument = event.getInstrument();
        if (player == null || instrument == null) return;

        player.setInstrument(instrument);
        player.setInstrumentId(instrument.getId());
        applyPreset(player);

        if (Boolean.TRUE.equals(player.getIsDefault())) {
            UserConfigManager.getInstance().updateDefaultPlayer(player.deepCopy());
            if (Boolean.TRUE.equals(instrument.getIsDefault())) {
                UserConfigManager.getInstance().updateDefaultInstrument(instrument);
            }
        } else {
            savePlayer(player);
        }

        CommandBus.getInstance().publish(Commands.PLAYER_INSTRUMENT_CHANGED, this,
                new Object[]{player.getId(), instrument.getId()});
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
    }

    public void handleRuleUpdate(Player player, Rule rule, PlayerRuleUpdateEvent.RuleUpdateType updateType) {
        if (player == null) return;

        savePlayer(player);
        CommandBus.getInstance().publish(Commands.PLAYER_RULE_UPDATE_EVENT, this, 
                new PlayerRuleUpdateEvent(player, rule, updateType));
        CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));
    }

    public void ensureChannelConsistency() {
        logger.info("Ensuring channel consistency");
        
        Map<Integer, Long> channelToPlayer = new HashMap<>();
        ChannelManager channelManager = ChannelManager.getInstance();

        for (Player player : players.values()) {
            if (player != null && player.getChannel() != null) {
                Integer channel = player.getChannel();
                
                if (channel == SequencerConstants.MIDI_DRUM_CHANNEL) continue;

                if (channelToPlayer.containsKey(channel)) {
                    int newChannel = channelManager.getNextAvailableMelodicChannel();
                    player.setDefaultChannel(newChannel);
                    savePlayer(player);
                    if (player.getInstrument() != null) {
                        applyPreset(player);
                    }
                } else {
                    channelToPlayer.put(channel, player.getId());
                    channelManager.reserveChannel(channel);
                }
            }
        }
    }

    // ========== Instrument Management ==========

    public InstrumentWrapper getInstrument(Long id) {
        if (id == null) return null;
        
        InstrumentWrapper instrument = instruments.get(id);
        if (instrument == null) {
            instrument = redisService.findInstrumentById(id);
            if (instrument != null) {
                instruments.put(id, instrument);
            }
        }
        return instrument;
    }

    public void saveInstrument(InstrumentWrapper instrument) {
        if (instrument == null) return;
        
        try {
            redisService.saveInstrument(instrument);
            instruments.put(instrument.getId(), instrument);
        } catch (Exception e) {
            logger.error("Error saving instrument", e);
        }
    }

    public InstrumentWrapper getOrCreateInternalInstrument(int channel, boolean exclusive, int tag) {
        for (InstrumentWrapper cached : instruments.values()) {
            if (MidiService.getInstance().isInternalSynth(cached) &&
                    cached.getChannel() != null &&
                    cached.getChannel() == channel &&
                    (!exclusive || !cached.getAssignedToPlayer())) {
                cached.setAssignedToPlayer(exclusive);
                return cached;
            }
        }

        String name = (channel == SequencerConstants.MIDI_DRUM_CHANNEL) ? 
                "Internal Drums " + tag : "Internal Melo " + channel + "-" + tag;
        
        InstrumentWrapper instrument = createInternalInstrument(name, channel);
        if (instrument != null) {
            instrument.setAssignedToPlayer(exclusive);
            instruments.put(instrument.getId(), instrument);
        }
        return instrument;
    }

    private InstrumentWrapper createInternalInstrument(String name, int channel) {
        try {
            javax.sound.midi.Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null) return null;

            InstrumentWrapper instrument = new InstrumentWrapper(name, synth, channel);
            instrument.setInternal(true);
            instrument.setInternalSynth(true);
            instrument.setDeviceName(SequencerConstants.GERVILL);
            instrument.setBankIndex(0);
            instrument.setPreset(0);
            instrument.setId(redisService.getInstrumentHelper().getNextInstrumentId());

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

    public void initializeInternalInstrument(Player player, boolean exclusive, int tag) {
        if (player == null) return;

        try {
            InstrumentWrapper instrument = getOrCreateInternalInstrument(player.getChannel(), exclusive, tag);
            if (instrument != null) {
                player.setInstrument(instrument);
                player.setInstrumentId(instrument.getId());
                player.setUsingInternalSynth(true);
                savePlayer(player);
                applyPreset(player);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize internal instrument", e);
        }
    }
}
