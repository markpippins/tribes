package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequenceModifier;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;

/**
 * Unified sequencer service - manages both drum and melodic sequencers.
 * Replaces: DrumSequencerManager, MelodicSequencerManager
 */
public class SequencerService {
    private static final Logger logger = LoggerFactory.getLogger(SequencerService.class);
    private static SequencerService instance;

    private final List<DrumSequencer> drumSequencers = new ArrayList<>();
    private final Map<Integer, MelodicSequencer> melodicSequencers = new ConcurrentHashMap<>();
    private final RedisService redisService;
    private int selectedPadIndex = 0;

    private SequencerService() {
        this.redisService = RedisService.getInstance();
    }

    public static synchronized SequencerService getInstance() {
        if (instance == null) {
            instance = new SequencerService();
        }
        return instance;
    }

    // ========== Drum Sequencer Methods ==========

    public synchronized DrumSequencer createDrumSequencer() {
        DrumSequencer sequencer = new DrumSequencer();
        sequencer.setId(drumSequencers.size());
        try {
            sequencer.initialize();
        } catch (Exception e) {
            logger.error("Error initializing drum sequencer", e);
        }
        drumSequencers.add(sequencer);
        return sequencer;
    }

    public synchronized DrumSequencer createDrumSequencer(Consumer<NoteEvent> noteListener, 
                                                          Consumer<DrumStepUpdateEvent> stepListener) {
        DrumSequencer sequencer = new DrumSequencer();
        sequencer.setNoteEventListener(noteListener);
        sequencer.setStepUpdateListener(stepListener);
        try {
            sequencer.initialize();
        } catch (Exception e) {
            logger.error("Error initializing drum sequencer", e);
        }
        drumSequencers.add(sequencer);
        return sequencer;
    }

    public synchronized DrumSequencer newSequencer(Consumer<NoteEvent> noteListener, 
                                                   Consumer<DrumStepUpdateEvent> stepListener) {
        return createDrumSequencer(noteListener, stepListener);
    }

    public DrumSequencer getDrumSequencer(int index) {
        if (index >= 0 && index < drumSequencers.size()) {
            return drumSequencers.get(index);
        }
        return null;
    }

    public List<DrumSequencer> getAllDrumSequencers() {
        return Collections.unmodifiableList(drumSequencers);
    }

    public int getSelectedPadIndex() {
        return selectedPadIndex;
    }

    public void setSelectedPadIndex(int index) {
        if (index >= 0 && index < SequencerConstants.DRUM_PAD_COUNT) {
            selectedPadIndex = index;
            drumSequencers.forEach(seq -> seq.setSelectedPadIndex(index));
        }
    }

    public Long saveDrumSequence(DrumSequencer sequencer) {
        try {
            redisService.saveDrumSequence(sequencer);
            return sequencer.getSequenceData().getId();
        } catch (Exception e) {
            logger.error("Error saving drum sequence", e);
            return null;
        }
    }

    public DrumSequenceData loadDrumSequence(Long id) {
        try {
            return redisService.findDrumSequenceById(id);
        } catch (Exception e) {
            logger.error("Error loading drum sequence", e);
            return null;
        }
    }

    public boolean loadSequence(Long id, DrumSequencer sequencer) {
        try {
            DrumSequenceData data = loadDrumSequence(id);
            if (data != null) {
                sequencer.setSequenceData(data);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error loading sequence into sequencer", e);
            return false;
        }
    }

    public Long getNextSequenceId(Long currentId) {
        try {
            return redisService.getNextDrumSequenceId(currentId);
        } catch (Exception e) {
            logger.error("Error getting next sequence ID", e);
            return null;
        }
    }

    public Long getPreviousSequenceId(Long currentId) {
        try {
            return redisService.getPreviousDrumSequenceId(currentId);
        } catch (Exception e) {
            logger.error("Error getting previous sequence ID", e);
            return null;
        }
    }

    public Long getLastSequenceId() {
        try {
            return redisService.getMaximumDrumSequenceId();
        } catch (Exception e) {
            logger.error("Error getting last sequence ID", e);
            return null;
        }
    }

    public List<DrumSequencer> getAllSequencers() {
        return getAllDrumSequencers();
    }

    public Long saveSequence(DrumSequencer sequencer) {
        return saveDrumSequence(sequencer);
    }

    public Long saveSequence(MelodicSequencer sequencer) {
        return saveMelodicSequence(sequencer);
    }

    public void refreshSequenceList() {
        // Placeholder - UI can refresh from Redis directly
        logger.debug("Sequence list refresh requested");
    }

    public DrumSequencer createNewSequence() {
        return createDrumSequencer();
    }

    public DrumSequenceData createNewSequenceData() {
        DrumSequenceData data = new DrumSequenceData();
        Long maxId = redisService.getMaximumDrumSequenceId();
        Long nextId = (maxId != null) ? maxId + 1 : 1L;
        data.setId(nextId);
        return data;
    }

    public MelodicSequenceData getSequenceData(Integer sequencerId, Long sequenceId) {
        return loadMelodicSequence(sequenceId, sequencerId);
    }

    public java.util.List<Long> getAllMelodicSequenceIds(Integer sequencerId) {
        try {
            return redisService.getAllMelodicSequenceIds(sequencerId);
        } catch (Exception e) {
            logger.error("Error getting melodic sequence IDs", e);
            return java.util.Collections.emptyList();
        }
    }

    public java.util.List<Long> getAllDrumSequenceIds() {
        try {
            return redisService.getAllDrumSequenceIds();
        } catch (Exception e) {
            logger.error("Error getting drum sequence IDs", e);
            return java.util.Collections.emptyList();
        }
    }

    public MelodicSequencer getActiveSequencer() {
        // Return the first melodic sequencer or null
        if (!melodicSequencers.isEmpty()) {
            return melodicSequencers.values().iterator().next();
        }
        return null;
    }

    public boolean hasSequences() {
        try {
            List<Long> ids = redisService.getAllDrumSequenceIds();
            return ids != null && !ids.isEmpty();
        } catch (Exception e) {
            logger.error("Error checking for drum sequences", e);
            return false;
        }
    }

    public Long getFirstSequenceId() {
        try {
            return redisService.getMinimumDrumSequenceId();
        } catch (Exception e) {
            logger.error("Error getting first drum sequence ID", e);
            return null;
        }
    }

    public Long getLastSequenceId(Integer sequencerId) {
        try {
            return redisService.getMaximumMelodicSequenceId(sequencerId);
        } catch (Exception e) {
            logger.error("Error getting last melodic sequence ID", e);
            return null;
        }
    }

    public Long getPreviousSequenceId(Integer sequencerId, Long currentId) {
        try {
            return redisService.getPreviousMelodicSequenceId(sequencerId, currentId);
        } catch (Exception e) {
            logger.error("Error getting previous melodic sequence ID", e);
            return null;
        }
    }

    public Long getNextSequenceId(Integer sequencerId, Long currentId) {
        try {
            return redisService.getNextMelodicSequenceId(sequencerId, currentId);
        } catch (Exception e) {
            logger.error("Error getting next melodic sequence ID", e);
            return null;
        }
    }

    // ========== Melodic Sequencer Methods ==========

    public MelodicSequencer createMelodicSequencer(int id) {
        MelodicSequencer sequencer = new MelodicSequencer(id);
        sequencer.setSequenceData(new MelodicSequenceData());
        try {
            sequencer.initialize();
        } catch (Exception e) {
            logger.error("Error initializing melodic sequencer", e);
        }
        melodicSequencers.put(id, sequencer);
        return sequencer;
    }

    public MelodicSequencer newSequencer(int id) {
        return createMelodicSequencer(id);
    }

    public MelodicSequencer getMelodicSequencer(int id) {
        return melodicSequencers.get(id);
    }

    public MelodicSequencer getSequencer(int id) {
        return getMelodicSequencer(id);
    }

    public int getSequencerCount() {
        return melodicSequencers.size();
    }

    public boolean hasSequences(Integer sequencerId) {
        try {
            List<Long> ids = redisService.getAllMelodicSequenceIds(sequencerId);
            return ids != null && !ids.isEmpty();
        } catch (Exception e) {
            logger.error("Error checking for sequences", e);
            return false;
        }
    }

    public Long getFirstSequenceId(Integer sequencerId) {
        try {
            return redisService.getMinimumMelodicSequenceId(sequencerId);
        } catch (Exception e) {
            logger.error("Error getting first sequence ID", e);
            return null;
        }
    }

    public List<MelodicSequencer> getAllMelodicSequencers() {
        return new ArrayList<>(melodicSequencers.values());
    }

    public Long saveMelodicSequence(MelodicSequencer sequencer) {
        try {
            MelodicSequenceModifier.updateInstrumentSettingsInSequenceData(sequencer);
            redisService.saveMelodicSequence(sequencer);
            return sequencer.getSequenceData().getId();
        } catch (Exception e) {
            logger.error("Error saving melodic sequence", e);
            return null;
        }
    }

    public MelodicSequenceData loadMelodicSequence(Long id, int sequencerId) {
        try {
            return redisService.findMelodicSequenceById(id, sequencerId);
        } catch (Exception e) {
            logger.error("Error loading melodic sequence", e);
            return null;
        }
    }

    // ========== Common Methods ==========

    public void updateTempo(float tempoInBPM, int ticksPerBeat) {
        drumSequencers.forEach(seq -> seq.getSequenceData().setMasterTempo(ticksPerBeat));
        melodicSequencers.values().forEach(seq -> seq.setMasterTempo(ticksPerBeat));
    }

    public void repairMidiConnections() {
        logger.info("Repairing MIDI connections for all sequencers");
        MidiService.getInstance().clearAllReceivers();
        
        drumSequencers.forEach(this::repairDrumSequencer);
        melodicSequencers.values().forEach(this::repairMelodicSequencer);
    }

    private void repairDrumSequencer(DrumSequencer sequencer) {
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            Player player = sequencer.getPlayer(i);
            if (player != null && player.getInstrument() != null) {
                repairInstrument(player);
            }
        }
    }

    private void repairMelodicSequencer(MelodicSequencer sequencer) {
        Player player = sequencer.getPlayer();
        if (player != null && player.getInstrument() != null) {
            repairInstrument(player);
        }
    }

    private void repairInstrument(Player player) {
        String deviceName = player.getInstrument().getDeviceName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = SequencerConstants.GERVILL;
            player.getInstrument().setDeviceName(deviceName);
        }

        MidiDevice device = MidiService.getInstance().getDevice(deviceName);
        if (device == null) {
            device = MidiService.getInstance().getDefaultOutputDevice();
            if (device != null) {
                deviceName = device.getDeviceInfo().getName();
                player.getInstrument().setDeviceName(deviceName);
            }
        }

        if (device != null) {
            MidiService.getInstance().openDevice(device);
            player.getInstrument().setDevice(device);
            Receiver receiver = MidiService.getInstance().getReceiver(deviceName);
            if (receiver != null) {
                player.getInstrument().setReceiver(receiver);
            }
        }
    }
}
