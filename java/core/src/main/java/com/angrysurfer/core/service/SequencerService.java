package com.angrysurfer.core.service;

import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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

    public MelodicSequencer getMelodicSequencer(int id) {
        return melodicSequencers.get(id);
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
