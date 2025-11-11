package com.angrysurfer.core.service;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.preset.DrumItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.File;
import java.util.*;

/**
 * Manages soundbanks and presets.
 * Replaces: SoundbankManager
 */
public class SoundbankService {
    private static final Logger logger = LoggerFactory.getLogger(SoundbankService.class);
    private static SoundbankService instance;

    private final Map<String, Soundbank> soundbanks = new LinkedHashMap<>();
    private final Map<String, List<Integer>> availableBanksMap = new HashMap<>();
    private final List<DrumItem> drumItems = new ArrayList<>();

    private SoundbankService() {
    }

    public static synchronized SoundbankService getInstance() {
        if (instance == null) {
            instance = new SoundbankService();
        }
        return instance;
    }

    public void initialize() {
        loadDefaultSoundbank();
        initializeDrumItems();
    }

    private void loadDefaultSoundbank() {
        try {
            Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null) return;

            Soundbank defaultSoundbank = synth.getDefaultSoundbank();
            if (defaultSoundbank != null) {
                String name = "Java Internal Soundbank";
                soundbanks.put(name, defaultSoundbank);
                
                Set<Integer> banks = new HashSet<>();
                for (Instrument inst : defaultSoundbank.getInstruments()) {
                    banks.add(inst.getPatch().getBank());
                }
                availableBanksMap.put(name, new ArrayList<>(banks));
                
                logger.info("Loaded default soundbank with {} instruments", 
                           defaultSoundbank.getInstruments().length);
            }
        } catch (Exception e) {
            logger.error("Error loading default soundbank", e);
        }
    }

    private void initializeDrumItems() {
        String[] drumNames = {
            "Acoustic Bass Drum", "Bass Drum 1", "Side Stick", "Acoustic Snare",
            "Hand Clap", "Electric Snare", "Low Floor Tom", "Closed Hi-Hat",
            "High Floor Tom", "Pedal Hi-Hat", "Low Tom", "Open Hi-Hat",
            "Low-Mid Tom", "Hi-Mid Tom", "Crash Cymbal 1", "High Tom",
            "Ride Cymbal 1", "Chinese Cymbal", "Ride Bell", "Tambourine",
            "Splash Cymbal", "Cowbell", "Crash Cymbal 2", "Vibraslap",
            "Ride Cymbal 2", "Hi Bongo", "Low Bongo", "Mute Hi Conga",
            "Open Hi Conga", "Low Conga", "High Timbale", "Low Timbale",
            "High Agogo", "Low Agogo", "Cabasa", "Maracas",
            "Short Whistle", "Long Whistle", "Short Guiro", "Long Guiro",
            "Claves", "Hi Wood Block", "Low Wood Block", "Mute Cuica",
            "Open Cuica", "Mute Triangle", "Open Triangle"
        };

        for (int i = 0; i < drumNames.length; i++) {
            int noteNumber = 35 + i;
            drumItems.add(new DrumItem(noteNumber, drumNames[i]));
        }
    }

    public List<String> getSoundbankNames() {
        return new ArrayList<>(soundbanks.keySet());
    }

    public List<Integer> getAvailableBanks(String soundbankName) {
        return availableBanksMap.getOrDefault(soundbankName, Collections.emptyList());
    }

    public List<String> getPresetNames(String soundbankName, int bankIndex) {
        Soundbank soundbank = soundbanks.get(soundbankName);
        if (soundbank == null) return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (Instrument inst : soundbank.getInstruments()) {
            if (inst.getPatch().getBank() == bankIndex) {
                names.add(inst.getName());
            }
        }
        return names;
    }

    public List<String> getGeneralMIDIPresetNames() {
        List<String> names = new ArrayList<>();
        String[] gmNames = {
            "Acoustic Grand Piano", "Bright Acoustic Piano", "Electric Grand Piano", "Honky-tonk Piano",
            "Electric Piano 1", "Electric Piano 2", "Harpsichord", "Clavinet",
            "Celesta", "Glockenspiel", "Music Box", "Vibraphone",
            "Marimba", "Xylophone", "Tubular Bells", "Dulcimer",
            "Drawbar Organ", "Percussive Organ", "Rock Organ", "Church Organ",
            "Reed Organ", "Accordion", "Harmonica", "Tango Accordion",
            "Acoustic Guitar (nylon)", "Acoustic Guitar (steel)", "Electric Guitar (jazz)", "Electric Guitar (clean)",
            "Electric Guitar (muted)", "Overdriven Guitar", "Distortion Guitar", "Guitar Harmonics",
            "Acoustic Bass", "Electric Bass (finger)", "Electric Bass (pick)", "Fretless Bass",
            "Slap Bass 1", "Slap Bass 2", "Synth Bass 1", "Synth Bass 2",
            "Violin", "Viola", "Cello", "Contrabass",
            "Tremolo Strings", "Pizzicato Strings", "Orchestral Harp", "Timpani",
            "String Ensemble 1", "String Ensemble 2", "Synth Strings 1", "Synth Strings 2",
            "Choir Aahs", "Voice Oohs", "Synth Choir", "Orchestra Hit",
            "Trumpet", "Trombone", "Tuba", "Muted Trumpet",
            "French Horn", "Brass Section", "Synth Brass 1", "Synth Brass 2",
            "Soprano Sax", "Alto Sax", "Tenor Sax", "Baritone Sax",
            "Oboe", "English Horn", "Bassoon", "Clarinet",
            "Piccolo", "Flute", "Recorder", "Pan Flute",
            "Blown bottle", "Shakuhachi", "Whistle", "Ocarina",
            "Lead 1 (square)", "Lead 2 (sawtooth)", "Lead 3 (calliope)", "Lead 4 (chiff)",
            "Lead 5 (charang)", "Lead 6 (voice)", "Lead 7 (fifths)", "Lead 8 (bass + lead)",
            "Pad 1 (new age)", "Pad 2 (warm)", "Pad 3 (polysynth)", "Pad 4 (choir)",
            "Pad 5 (bowed)", "Pad 6 (metallic)", "Pad 7 (halo)", "Pad 8 (sweep)",
            "FX 1 (rain)", "FX 2 (soundtrack)", "FX 3 (crystal)", "FX 4 (atmosphere)",
            "FX 5 (brightness)", "FX 6 (goblins)", "FX 7 (echoes)", "FX 8 (sci-fi)",
            "Sitar", "Banjo", "Shamisen", "Koto",
            "Kalimba", "Bagpipe", "Fiddle", "Shanai",
            "Tinkle Bell", "Agogo", "Steel Drums", "Woodblock",
            "Taiko Drum", "Melodic Tom", "Synth Drum", "Reverse Cymbal",
            "Guitar Fret Noise", "Breath Noise", "Seashore", "Bird Tweet",
            "Telephone Ring", "Helicopter", "Applause", "Gunshot"
        };
        Collections.addAll(names, gmNames);
        return names;
    }

    public List<DrumItem> getDrumItems() {
        return new ArrayList<>(drumItems);
    }

    public String getDrumName(int noteNumber) {
        return drumItems.stream()
                .filter(item -> item.getNoteNumber() == noteNumber)
                .map(DrumItem::getName)
                .findFirst()
                .orElse("Drum " + noteNumber);
    }

    public String loadSoundbank(File file) {
        try {
            Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null) return null;

            Soundbank soundbank = MidiSystem.getSoundbank(file);
            if (soundbank == null) return null;

            boolean loaded = synth.loadAllInstruments(soundbank);
            if (loaded) {
                String name = soundbank.getName();
                soundbanks.put(name, soundbank);
                
                Set<Integer> banks = new HashSet<>();
                for (Instrument inst : soundbank.getInstruments()) {
                    banks.add(inst.getPatch().getBank());
                }
                availableBanksMap.put(name, new ArrayList<>(banks));
                
                logger.info("Loaded soundbank: {}", name);
                return name;
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank from file", e);
        }
        return null;
    }

    public boolean applySoundbank(InstrumentWrapper instrument, String soundbankName) {
        if (instrument == null || soundbankName == null) return false;

        Soundbank soundbank = soundbanks.get(soundbankName);
        if (soundbank == null) return false;

        try {
            Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null) return false;

            boolean loaded = synth.loadAllInstruments(soundbank);
            if (loaded) {
                instrument.setSoundbankName(soundbankName);
                logger.debug("Applied soundbank {} to instrument {}", soundbankName, instrument.getName());
                return true;
            }
        } catch (Exception e) {
            logger.error("Error applying soundbank", e);
        }
        return false;
    }

    public List<Integer> getAvailableBanksByName(String soundbankName) {
        return getAvailableBanks(soundbankName);
    }

    public boolean updatePlayerSound(com.angrysurfer.core.model.Player player, String soundbankName, Integer bankIndex, Integer presetNumber) {
        if (player == null || player.getInstrument() == null) return false;
        
        com.angrysurfer.core.model.InstrumentWrapper instrument = player.getInstrument();
        if (soundbankName != null) {
            applySoundbank(instrument, soundbankName);
        }
        if (bankIndex != null) {
            instrument.setBankIndex(bankIndex);
        }
        if (presetNumber != null) {
            instrument.setPreset(presetNumber);
        }
        
        return PlaybackService.getInstance().applyPreset(player);
    }

    public void playPreviewNote(com.angrysurfer.core.model.Player player, int durationMs) {
        if (player == null || player.getInstrument() == null) return;
        
        int note = player.getRootNote() != null ? player.getRootNote() : 60;
        int velocity = 100;
        int channel = player.getChannel();
        
        MidiService.getInstance().playNote(note, velocity, durationMs, channel);
    }

    public void updateInstrumentUIComponents(com.angrysurfer.core.model.InstrumentWrapper instrument,
                                            javax.swing.JComboBox<String> soundbankCombo,
                                            javax.swing.JComboBox<Integer> bankCombo,
                                            javax.swing.JComboBox<com.angrysurfer.core.model.preset.PresetItem> presetCombo) {
        // Placeholder - UI components should be updated by the UI code
        logger.debug("UI component update requested for instrument: {}", instrument != null ? instrument.getName() : "null");
    }

    public java.util.List<com.angrysurfer.core.model.preset.SoundbankItem> getPlayerSoundbanks(com.angrysurfer.core.model.Player player) {
        java.util.List<com.angrysurfer.core.model.preset.SoundbankItem> items = new java.util.ArrayList<>();
        for (String name : getSoundbankNames()) {
            items.add(new com.angrysurfer.core.model.preset.SoundbankItem(name));
        }
        return items;
    }

    public java.util.List<com.angrysurfer.core.model.preset.BankItem> getPlayerBanks(com.angrysurfer.core.model.Player player, String soundbankName) {
        java.util.List<com.angrysurfer.core.model.preset.BankItem> items = new java.util.ArrayList<>();
        for (Integer bank : getAvailableBanks(soundbankName)) {
            items.add(new com.angrysurfer.core.model.preset.BankItem(bank, "Bank " + bank));
        }
        return items;
    }

    public java.util.List<com.angrysurfer.core.model.preset.PresetItem> getPlayerPresets(com.angrysurfer.core.model.Player player, String soundbankName, Integer bankIndex) {
        java.util.List<com.angrysurfer.core.model.preset.PresetItem> items = new java.util.ArrayList<>();
        java.util.List<String> names = getPresetNames(soundbankName, bankIndex);
        for (int i = 0; i < names.size(); i++) {
            items.add(new com.angrysurfer.core.model.preset.PresetItem(i, names.get(i)));
        }
        return items;
    }

    public java.util.List<com.angrysurfer.core.model.preset.PresetItem> getDrumPresets() {
        java.util.List<com.angrysurfer.core.model.preset.PresetItem> items = new java.util.ArrayList<>();
        for (DrumItem drum : drumItems) {
            items.add(new com.angrysurfer.core.model.preset.PresetItem(drum.getNoteNumber(), drum.getName()));
        }
        return items;
    }

    public List<String> getPresetNames(String soundbankName, Integer bankIndex) {
        if (bankIndex == null) return Collections.emptyList();
        return getPresetNames(soundbankName, bankIndex.intValue());
    }

    public String getPresetName(Long instrumentId, Long presetNumber) {
        // Simplified - return preset number as string
        return presetNumber != null ? "Preset " + presetNumber : "Unknown";
    }

    public Soundbank getSoundbank(String soundbankName) {
        return soundbanks.get(soundbankName);
    }

    public String loadSoundbankFile(java.io.File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null) return null;

            Soundbank soundbank = MidiSystem.getSoundbank(file);
            if (soundbank != null) {
                String name = file.getName().replace(".sf2", "").replace(".dls", "");
                soundbanks.put(name, soundbank);
                
                // Extract available banks
                Set<Integer> banks = new java.util.HashSet<>();
                for (Instrument inst : soundbank.getInstruments()) {
                    banks.add(inst.getPatch().getBank());
                }
                availableBanksMap.put(name, new ArrayList<>(banks));
                
                logger.info("Loaded soundbank: {} with {} instruments", name, soundbank.getInstruments().length);
                return name;
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank file", e);
        }
        return null;
    }
}
