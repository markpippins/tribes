package com.angrysurfer.core.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.JComboBox;

import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.api.midi.MidiControlMessageEnum;
import com.angrysurfer.core.event.PlayerPresetChangeEvent;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.preset.BankItem;
import com.angrysurfer.core.model.preset.DrumItem;
import com.angrysurfer.core.model.preset.PresetItem;
import com.angrysurfer.core.model.preset.SoundbankItem;
import com.angrysurfer.core.model.preset.SynthData;
import com.angrysurfer.core.sequencer.SequencerConstants;

public class SoundbankManager implements IBusListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SoundbankManager.class);

    private static SoundbankManager instance;
    // Map of synth IDs to preset information
    private final Map<Long, SynthData> synthDataMap = new HashMap<>();
    // Use LinkedHashMap to preserve insertion order
    private final LinkedHashMap<String, Soundbank> soundbanks = new LinkedHashMap<>();
    private final int defaultMidiChannel = 15; // Default channel for melodic sounds
    // Map to store available banks for each soundbank (by name)
    private final Map<String, List<Integer>> availableBanksMap = new HashMap<>();
    // Add synthesizer as a central instance
    private Synthesizer synthesizer;

    private SoundbankManager() {
        // Register with command bus
        CommandBus.getInstance().register(this, new String[]{
                Commands.REFRESH_SOUNDBANKS, Commands.LOAD_SOUNDBANK});
    }

    public static SoundbankManager getInstance() {
        if (instance == null) {
            instance = new SoundbankManager();
        }
        return instance;
    }

    /**
     * Initialize available soundbanks
     *
     * @return true if initialization was successful
     */
    public boolean initializeSoundbanks() {
        try {
            logger.info("Initializing soundbanks...");

            // Clear existing collections first
            soundbanks.clear();
            availableBanksMap.clear();

            // Make sure we have a synthesizer
            if (synthesizer == null || !synthesizer.isOpen()) {
                initializeSynthesizer();
                if (synthesizer == null) {
                    logger.error("Failed to initialize synthesizer");
                    return false;
                }
            }

            // Add default Java soundbank
            Soundbank defaultSoundbank = synthesizer.getDefaultSoundbank();
            if (defaultSoundbank != null) {
                String sbName = "Java Internal Soundbank";
                soundbanks.put(sbName, defaultSoundbank);

                // Get or create SynthData for this soundbank
                long synthId = System.identityHashCode(synthesizer);
                SynthData synthData = synthDataMap.get(synthId);
                if (synthData == null) {
                    // This should have been initialized in initializeSynthData
                    // but in case it wasn't, do it now
                    synthData = new SynthData(sbName);
                    for (Instrument instrument : defaultSoundbank.getInstruments()) {
                        synthData.addInstrument(instrument);
                    }
                    synthDataMap.put(synthId, synthData);
                }

                // Update available banks map
                availableBanksMap.put(sbName, synthData.getAvailableBanks());

                logger.info("Added default soundbank with {} instruments",
                        defaultSoundbank.getInstruments().length);
            } else {
                logger.warn("No default soundbank available in synthesizer");
            }

            // Check for additional user soundbanks in the app's data directory
            File soundbankDir = getUserSoundbankDirectory();
            if (soundbankDir.exists() && soundbankDir.isDirectory()) {
                File[] files = soundbankDir.listFiles(
                        (dir, name) -> name.toLowerCase().endsWith(".sf2") || name.toLowerCase().endsWith(".dls"));

                if (files != null) {
                    for (File file : files) {
                        try {
                            // Try to load the soundbank
                            Soundbank sb = MidiSystem.getSoundbank(file);
                            if (sb != null) {
                                String name = file.getName();
                                soundbanks.put(name, sb);

                                // Create SynthData for this soundbank
                                SynthData sbData = new SynthData(name);
                                for (Instrument instrument : sb.getInstruments()) {
                                    sbData.addInstrument(instrument);
                                }

                                // Store with unique ID
                                synthDataMap.put((long) sb.hashCode(), sbData);

                                // Update available banks
                                availableBanksMap.put(name, sbData.getAvailableBanks());

                                logger.info("Loaded soundbank from file: {} with {} instruments",
                                        name, sb.getInstruments().length);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to load soundbank from file: {}", file.getName(), e);
                        }
                    }
                }
            }

            logger.info("Soundbank initialization complete. Total soundbanks: {}", soundbanks.size());
            return true;
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the directory for user soundbank files
     */
    private File getUserSoundbankDirectory() {
        // First try user home
        String userHome = System.getProperty("user.home");
        File soundbanksDir = new File(userHome, ".beatsapp/soundbanks");

        // Create directory if it doesn't exist
        if (!soundbanksDir.exists()) {
            soundbanksDir.mkdirs();
        }

        return soundbanksDir;
    }

    /**
     * Load a soundbank from a file
     *
     * @param file The soundbank file (.sf2 or .dls)
     * @return The name of the loaded soundbank, or null if loading failed
     */
    public String loadSoundbank(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            logger.error("Invalid soundbank file: {}", file);
            return null;
        }

        try {
            // Load the soundbank
            Soundbank soundbank = MidiSystem.getSoundbank(file);
            if (soundbank == null) {
                logger.error("Failed to load soundbank from file: {}", file);
                return null;
            }

            // Use the filename as the soundbank name
            String name = file.getName();

            // Add to collections
            soundbanks.put(name, soundbank);

            // Create SynthData for this soundbank
            SynthData sbData = new SynthData(name);
            for (Instrument instrument : soundbank.getInstruments()) {
                sbData.addInstrument(instrument);
            }

            // Store with unique ID
            synthDataMap.put((long) soundbank.hashCode(), sbData);

            // Update available banks
            availableBanksMap.put(name, sbData.getAvailableBanks());

            // Copy file to user soundbank directory for persistence
            File userDir = getUserSoundbankDirectory();
            File destFile = new File(userDir, file.getName());
            if (!destFile.equals(file)) {
                java.nio.file.Files.copy(
                        file.toPath(),
                        destFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Loaded soundbank: {} with {} instruments",
                    name, soundbank.getInstruments().length);

            return name;
        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Load a soundbank from a file (alias for loadSoundbank)
     *
     * @param file The soundbank file (.sf2 or .dls)
     * @return The name of the loaded soundbank, or null if loading failed
     */
    public String loadSoundbankFile(File file) {
        return loadSoundbank(file);
    }

    /**
     * Delete a soundbank and its associated file
     *
     * @param name The name of the soundbank to delete
     * @return true if deletion was successful
     */
    public boolean deleteSoundbank(String name) {
        if ("Java Internal Soundbank".equals(name)) {
            logger.warn("Cannot delete the default Java soundbank");
            return false;
        }

        try {
            // Remove from collections
            Soundbank removed = soundbanks.remove(name);
            if (removed != null) {
                // Also remove from bank maps
                availableBanksMap.remove(name);

                // Find and remove associated SynthData
                long removeKey = -1;
                for (Map.Entry<Long, SynthData> entry : synthDataMap.entrySet()) {
                    if (entry.getValue().getName().equals(name)) {
                        removeKey = entry.getKey();
                        break;
                    }
                }
                if (removeKey >= 0) {
                    synthDataMap.remove(removeKey);
                }

                // Try to delete the file if it exists
                File userDir = getUserSoundbankDirectory();
                File soundbankFile = new File(userDir, name);
                if (soundbankFile.exists()) {
                    boolean deleted = soundbankFile.delete();
                    if (!deleted) {
                        logger.warn("Could not delete soundbank file: {}", soundbankFile);
                    }
                }

                logger.info("Deleted soundbank: {}", name);
                return true;
            } else {
                logger.warn("Soundbank not found: {}", name);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting soundbank: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get a list of available soundbank names
     */
    public List<String> getSoundbankNames() {
        return new ArrayList<>(soundbanks.keySet());
    }

    /**
     * Get a soundbank by name
     */
    public Soundbank getSoundbank(String name) {
        return soundbanks.get(name);
    }

    /**
     * Get a soundbank by name (alias for getSoundbank)
     *
     * @param name The name of the soundbank to retrieve
     * @return The Soundbank object, or null if not found
     */
    public Soundbank getSoundbankByName(String name) {
        return getSoundbank(name);
    }

    /**
     * Find instruments by name across all soundbanks
     *
     * @param nameFragment The name fragment to search for
     * @return List of matching instruments
     */
    public List<Instrument> findInstrumentsByName(String nameFragment) {
        List<Instrument> results = new ArrayList<>();
        if (nameFragment == null || nameFragment.isEmpty()) {
            return results;
        }

        String lowerCaseSearch = nameFragment.toLowerCase();

        for (Soundbank soundbank : soundbanks.values()) {
            for (Instrument instrument : soundbank.getInstruments()) {
                if (instrument.getName().toLowerCase().contains(lowerCaseSearch)) {
                    results.add(instrument);
                }
            }
        }

        return results;
    }

    /**
     * Load an instrument into the synthesizer
     *
     * @param instrument The instrument to load
     * @return true if successful, false otherwise
     */
    public boolean loadInstrument(Instrument instrument) {
        try {
            Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null || !synth.isOpen()) {
                return false;
            }

            return synth.loadInstrument(instrument);
        } catch (Exception e) {
            logger.error("Error loading instrument: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get available banks for a soundbank by name
     */
    public List<Integer> getAvailableBanksByName(String soundbankName) {
        return availableBanksMap.getOrDefault(soundbankName, new ArrayList<>());
    }

    /**
     * Get preset names for a specific soundbank and bank
     */
    public List<String> getPresetNames(String soundbankName, int bankIndex) {
        // Find the SynthData for this soundbank
        for (SynthData synthData : synthDataMap.values()) {
            if (synthData.getName().equals(soundbankName)) {
                return synthData.getPresetNamesForBank(bankIndex);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Get preset names for a specific instrument ID
     *
     * @param instrumentId The instrument ID to look up presets for
     * @return List of preset names for the instrument
     */
    public List<String> getPresetNames(Long instrumentId) {
        if (instrumentId == null) {
            return getGeneralMIDIPresetNames(); // Fall back to standard GM names
        }

        try {
            // Get the instrument from InstrumentManager
            InstrumentWrapper instrument = InstrumentManager.getInstance().getInstrumentById(instrumentId);
            if (instrument == null) {
                logger.warn("Instrument not found for ID: {}", instrumentId);
                return getGeneralMIDIPresetNames();
            }

            // Get soundbank name from instrument
            String soundbankName = instrument.getSoundbankName();
            if (soundbankName == null || soundbankName.isEmpty()) {
                soundbankName = "Java Internal Soundbank";
            }

            // Get bank index from instrument
            int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;

            // Call the original method with the extracted parameters
            return getPresetNames(soundbankName, bankIndex);
        } catch (Exception e) {
            logger.error("Error getting preset names for instrument {}: {}",
                    instrumentId, e.getMessage());
            return getGeneralMIDIPresetNames(); // Fall back to standard GM names
        }
    }

    /**
     * Get a preset name for an instrument and preset number
     *
     * @param instrumentId The ID of the instrument (used to find the right
     *                     soundbank)
     * @param presetNumber The preset number to look up
     * @return The name of the preset, or a default name if not found
     */
    public String getPresetName(Long instrumentId, long presetNumber) {
        if (instrumentId == null || presetNumber < 0 || presetNumber > 127) {
            return "Unknown Preset";
        }

        try {
            // First, get the instrument from InstrumentManager
            InstrumentWrapper instrument = InstrumentManager.getInstance().getInstrumentById(instrumentId);
            if (instrument == null) {
                return "Program " + presetNumber;
            }

            // Get the soundbank name from the instrument
            String soundbankName = instrument.getSoundbankName();
            if (soundbankName == null || soundbankName.isEmpty()) {
                // Fall back to default soundbank
                soundbankName = "Java Internal Soundbank";
            }

            // Get the bank index from the instrument or default to 0
            int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;

            // Find the SynthData for this soundbank
            for (SynthData synthData : synthDataMap.values()) {
                if (synthData.getName().equals(soundbankName)) {
                    // Get the presets for this bank
                    Map<Integer, String> presets = synthData.getPresetsForBank(bankIndex);

                    // Look up the preset name
                    String presetName = presets.get((int) presetNumber);
                    if (presetName != null && !presetName.isEmpty()) {
                        return presetName;
                    } else {
                        // Return a default name with the number if not found
                        return "Program " + presetNumber;
                    }
                }
            }

            // If we get here, the soundbank wasn't found
            return "Program " + presetNumber;
        } catch (Exception e) {
            logger.error("Error getting preset name: {}", e.getMessage());
            return "Program " + presetNumber;
        }
    }

    /**
     * Get a list of standard General MIDI drum items
     *
     * @return List of DrumItem objects for all standard GM drum sounds
     */
    public List<DrumItem> getDrumItems() {
        List<DrumItem> drums = new ArrayList<>();

        // Standard General MIDI drum mappings
        drums.add(new DrumItem(35, "Acoustic Bass Drum"));
        drums.add(new DrumItem(36, "Bass Drum 1"));
        drums.add(new DrumItem(37, "Side Stick"));
        drums.add(new DrumItem(38, "Acoustic Snare"));
        drums.add(new DrumItem(39, "Hand Clap"));
        drums.add(new DrumItem(40, "Electric Snare"));
        drums.add(new DrumItem(41, "Low Floor Tom"));
        drums.add(new DrumItem(42, "Closed Hi-Hat"));
        drums.add(new DrumItem(43, "High Floor Tom"));
        drums.add(new DrumItem(44, "Pedal Hi-Hat"));
        drums.add(new DrumItem(45, "Low Tom"));
        drums.add(new DrumItem(46, "Open Hi-Hat"));
        drums.add(new DrumItem(47, "Low-Mid Tom"));
        drums.add(new DrumItem(48, "Hi-Mid Tom"));
        drums.add(new DrumItem(49, "Crash Cymbal 1"));
        drums.add(new DrumItem(50, "High Tom"));
        drums.add(new DrumItem(51, "Ride Cymbal 1"));
        drums.add(new DrumItem(52, "Chinese Cymbal"));
        drums.add(new DrumItem(53, "Ride Bell"));
        drums.add(new DrumItem(54, "Tambourine"));
        drums.add(new DrumItem(55, "Splash Cymbal"));
        drums.add(new DrumItem(56, "Cowbell"));
        drums.add(new DrumItem(57, "Crash Cymbal 2"));
        drums.add(new DrumItem(58, "Vibraslap"));
        drums.add(new DrumItem(59, "Ride Cymbal 2"));
        drums.add(new DrumItem(60, "Hi Bongo"));
        drums.add(new DrumItem(61, "Low Bongo"));
        drums.add(new DrumItem(62, "Mute Hi Conga"));
        drums.add(new DrumItem(63, "Open Hi Conga"));
        drums.add(new DrumItem(64, "Low Conga"));
        drums.add(new DrumItem(65, "High Timbale"));
        drums.add(new DrumItem(66, "Low Timbale"));
        drums.add(new DrumItem(67, "High Agogo"));
        drums.add(new DrumItem(68, "Low Agogo"));
        drums.add(new DrumItem(69, "Cabasa"));
        drums.add(new DrumItem(70, "Maracas"));
        drums.add(new DrumItem(71, "Short Whistle"));
        drums.add(new DrumItem(72, "Long Whistle"));
        drums.add(new DrumItem(73, "Short Guiro"));
        drums.add(new DrumItem(74, "Long Guiro"));
        drums.add(new DrumItem(75, "Claves"));
        drums.add(new DrumItem(76, "Hi Wood Block"));
        drums.add(new DrumItem(77, "Low Wood Block"));
        drums.add(new DrumItem(78, "Mute Cuica"));
        drums.add(new DrumItem(79, "Open Cuica"));
        drums.add(new DrumItem(80, "Mute Triangle"));
        drums.add(new DrumItem(81, "Open Triangle"));

        return drums;
    }

    /**
     * Get the name of a drum sound by its MIDI note number
     *
     * @param noteNumber The MIDI note number to look up
     * @return The name of the drum sound, or "Unknown Drum" if not found
     */
    public String getDrumName(int noteNumber) {
        // Get all drum items
        List<DrumItem> drums = getDrumItems();

        // Find the matching drum by note number
        for (DrumItem drum : drums) {
            if (drum.getNoteNumber() == noteNumber) {
                return drum.getName();
            }
        }

        // Return a placeholder for unknown drums
        return "Unknown Drum (" + noteNumber + ")";
    }

    /**
     * Get the standard General MIDI instrument preset names
     *
     * @return List of 128 preset names as defined by the General MIDI specification
     */
    public List<String> getGeneralMIDIPresetNames() {
        List<String> presets = new ArrayList<>(128);

        // Piano Family (0-7)
        presets.add("Acoustic Grand Piano");
        presets.add("Bright Acoustic Piano");
        presets.add("Electric Grand Piano");
        presets.add("Honky-tonk Piano");
        presets.add("Electric Piano 1");
        presets.add("Electric Piano 2");
        presets.add("Harpsichord");
        presets.add("Clavinet");

        // Chromatic Percussion (8-15)
        presets.add("Celesta");
        presets.add("Glockenspiel");
        presets.add("Music Box");
        presets.add("Vibraphone");
        presets.add("Marimba");
        presets.add("Xylophone");
        presets.add("Tubular Bells");
        presets.add("Dulcimer");

        // Organ (16-23)
        presets.add("Drawbar Organ");
        presets.add("Percussive Organ");
        presets.add("Rock Organ");
        presets.add("Church Organ");
        presets.add("Reed Organ");
        presets.add("Accordion");
        presets.add("Harmonica");
        presets.add("Tango Accordion");

        // Guitar (24-31)
        presets.add("Acoustic Guitar (nylon)");
        presets.add("Acoustic Guitar (steel)");
        presets.add("Electric Guitar (jazz)");
        presets.add("Electric Guitar (clean)");
        presets.add("Electric Guitar (muted)");
        presets.add("Overdriven Guitar");
        presets.add("Distortion Guitar");
        presets.add("Guitar Harmonics");

        // Bass (32-39)
        presets.add("Acoustic Bass");
        presets.add("Electric Bass (finger)");
        presets.add("Electric Bass (pick)");
        presets.add("Fretless Bass");
        presets.add("Slap Bass 1");
        presets.add("Slap Bass 2");
        presets.add("Synth Bass 1");
        presets.add("Synth Bass 2");

        // Strings (40-47)
        presets.add("Violin");
        presets.add("Viola");
        presets.add("Cello");
        presets.add("Contrabass");
        presets.add("Tremolo Strings");
        presets.add("Pizzicato Strings");
        presets.add("Orchestral Harp");
        presets.add("Timpani");

        // Ensemble (48-55)
        presets.add("String Ensemble 1");
        presets.add("String Ensemble 2");
        presets.add("Synth Strings 1");
        presets.add("Synth Strings 2");
        presets.add("Choir Aahs");
        presets.add("Voice Oohs");
        presets.add("Synth Choir");
        presets.add("Orchestra Hit");

        // Brass (56-63)
        presets.add("Trumpet");
        presets.add("Trombone");
        presets.add("Tuba");
        presets.add("Muted Trumpet");
        presets.add("French Horn");
        presets.add("Brass Section");
        presets.add("Synth Brass 1");
        presets.add("Synth Brass 2");

        // Reed (64-71)
        presets.add("Soprano Sax");
        presets.add("Alto Sax");
        presets.add("Tenor Sax");
        presets.add("Baritone Sax");
        presets.add("Oboe");
        presets.add("English Horn");
        presets.add("Bassoon");
        presets.add("Clarinet");

        // Pipe (72-79)
        presets.add("Piccolo");
        presets.add("Flute");
        presets.add("Recorder");
        presets.add("Pan Flute");
        presets.add("Blown Bottle");
        presets.add("Shakuhachi");
        presets.add("Whistle");
        presets.add("Ocarina");

        // Synth Lead (80-87)
        presets.add("Lead 1 (square)");
        presets.add("Lead 2 (sawtooth)");
        presets.add("Lead 3 (calliope)");
        presets.add("Lead 4 (chiff)");
        presets.add("Lead 5 (charang)");
        presets.add("Lead 6 (voice)");
        presets.add("Lead 7 (fifths)");
        presets.add("Lead 8 (bass + lead)");

        // Synth Pad (88-95)
        presets.add("Pad 1 (new age)");
        presets.add("Pad 2 (warm)");
        presets.add("Pad 3 (polysynth)");
        presets.add("Pad 4 (choir)");
        presets.add("Pad 5 (bowed)");
        presets.add("Pad 6 (metallic)");
        presets.add("Pad 7 (halo)");
        presets.add("Pad 8 (sweep)");

        // Synth Effects (96-103)
        presets.add("FX 1 (rain)");
        presets.add("FX 2 (soundtrack)");
        presets.add("FX 3 (crystal)");
        presets.add("FX 4 (atmosphere)");
        presets.add("FX 5 (brightness)");
        presets.add("FX 6 (goblins)");
        presets.add("FX 7 (echoes)");
        presets.add("FX 8 (sci-fi)");

        // Ethnic (104-111)
        presets.add("Sitar");
        presets.add("Banjo");
        presets.add("Shamisen");
        presets.add("Koto");
        presets.add("Kalimba");
        presets.add("Bagpipe");
        presets.add("Fiddle");
        presets.add("Shanai");

        // Percussive (112-119)
        presets.add("Tinkle Bell");
        presets.add("Agogo");
        presets.add("Steel Drums");
        presets.add("Woodblock");
        presets.add("Taiko Drum");
        presets.add("Melodic Tom");
        presets.add("Synth Drum");
        presets.add("Reverse Cymbal");

        // Sound Effects (120-127)
        presets.add("Guitar Fret Noise");
        presets.add("Breath Noise");
        presets.add("Seashore");
        presets.add("Bird Tweet");
        presets.add("Telephone Ring");
        presets.add("Helicopter");
        presets.add("Applause");
        presets.add("Gunshot");

        return presets;
    }

    /**
     * Apply a preset change to an instrument
     *
     * @param instrument   The instrument to apply the preset to
     * @param bankIndex    The bank index to select
     * @param presetNumber The preset number to select
     */
//    public void applyPresetChange(InstrumentWrapper instrument, String soundbank, int bankIndex, int presetNumber) {
//        if (instrument == null) {
//            return;
//        }
//
//        try {
//            // Store the settings in the instrument
//            instrument.setSoundbankName(soundbank);
//            instrument.setBankIndex(bankIndex);
//            instrument.setPreset(presetNumber);
//
//            // Get the MIDI channel from the instrument or default to 0
//            int channel = 0;
//            if (instrument.getReceivedChannels() != null && instrument.getReceivedChannels().length > 0) {
//                channel = instrument.getReceivedChannels()[0];
//            }
//
//            // Apply the bank and program change to the instrument's device
//            if (instrument.getDevice() instanceof Synthesizer synth) {
//                if (!synth.isOpen()) {
//                    synth.open();
//                }
//
//                // Get the MIDI channel
//                MidiChannel[] channels = synth.getChannels();
//                if (channels != null && channel < channels.length) {
//                    // Calculate bank MSB and LSB
//                    int bankMSB = (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON;
//                    int bankLSB = bankIndex & MidiControlMessageEnum.POLY_MODE_ON;
//
//                    // Send bank select and program change
//                    channels[channel].controlChange(0, bankMSB);
//                    channels[channel].controlChange(32, bankLSB);
//                    channels[channel].programChange(presetNumber);
//
//                    logger.debug("Applied preset change to synthesizer: bank={}, preset={}, channel={}",
//                            bankIndex, presetNumber, channel);
//                    return;
//                }
//            }
//
//            // Fall back to standard MIDI messages via InstrumentWrapper
//            instrument.controlChange(0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON); // Bank MSB
//            instrument.controlChange(32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON); // Bank LSB
//            instrument.programChange(presetNumber, 0);
//
//        } catch (Exception e) {
//            logger.error("Failed to apply preset change: {}", e.getMessage(), e);
//        }
//    }

    /**
     * Apply a preset change to a specific player's instrument
     *
     * @param player    The player to update
     * @param bankIndex The bank index
     * @param preset    The preset number
     */
    public void applyPresetChangeToPlayer(Player player, String soundbank, Integer bankIndex, Integer preset) {
        if (player == null || player.getInstrument() == null) {
            logger.warn("Cannot apply preset change - player or instrument is null");
            return;
        }

        InstrumentWrapper instrument = player.getInstrument();
        int channel = player.getChannel();

        try {
            // Store the specific MIDI device for this player
            MidiDevice device = instrument.getDevice();

            // Apply bank and program changes specifically to this instrument
            if (bankIndex != null) {
                instrument.setBankIndex(bankIndex);
            }

            if (preset != null) {
                instrument.setPreset(preset);
            }

            if (MidiService.getInstance().isInternalSynth(instrument)) {
                Synthesizer synth = MidiService.getInstance().getSynthesizer();
                if (synth != null && synth.isOpen()) {
                    MidiChannel[] channels = synth.getChannels();
                    if (channels != null && channel < channels.length) {
                        // Direct channel access to avoid affecting other instruments
                        channels[channel].controlChange(0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON);  // Bank MSB
                        channels[channel].controlChange(32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON);        // Bank LSB
                        channels[channel].programChange(preset);

                        logger.info("Applied preset change directly to player {} on channel {}: bank={}, program={}",
                                player.getId(), channel, bankIndex, preset);
                    }
                }
            } else if (device != null && device.isOpen() && instrument.getReceiver() != null) {
                // For external device, send MIDI message directly to this device's receiver
                Receiver receiver = instrument.getReceiver();

                // Use timestamped messages to ensure proper sequencing
                long timestamp = -1; // -1 means "as soon as possible"

                // Bank select MSB
                ShortMessage bankMSB = new javax.sound.midi.ShortMessage();
                bankMSB.setMessage(0xB0 | channel, 0, (bankIndex >> 7) & MidiControlMessageEnum.POLY_MODE_ON);
                receiver.send(bankMSB, timestamp);

                // Bank select LSB
                ShortMessage bankLSB = new javax.sound.midi.ShortMessage();
                bankLSB.setMessage(0xB0 | channel, 32, bankIndex & MidiControlMessageEnum.POLY_MODE_ON);
                receiver.send(bankLSB, timestamp);

                // Program change
                ShortMessage progChange = new javax.sound.midi.ShortMessage();
                progChange.setMessage(0xC0 | channel, preset, 0);
                receiver.send(progChange, timestamp);

                logger.info("Applied preset change to player {}'s external device on ch {}: bank={}, program={}",
                        player.getId(), channel, bankIndex, preset);
            }
        } catch (Exception e) {
            logger.error("Error applying preset change to player {}: {}", player.getId(), e.getMessage());
        }
    }

    /**
     * Apply a soundbank to an instrument
     *
     * @param instrument    The instrument to apply the soundbank to
     * @param soundbankName The name of the soundbank to apply
     * @return true if successful, false otherwise
     */
    public boolean applySoundbank(InstrumentWrapper instrument, String soundbankName) {
        if (instrument == null || soundbankName == null || soundbankName.isEmpty()) {
            logger.warn("Cannot apply soundbank: Invalid parameters");
            return false;
        }

        try {
            // Get the actual Soundbank object by name
            Soundbank soundbank = soundbanks.get(soundbankName);
            if (soundbank == null) {
                logger.warn("Soundbank not found: {}", soundbankName);
                return false;
            }

            boolean isInternalSynth = MidiService.getInstance().isInternalSynth(instrument);

            if (isInternalSynth) {
                Synthesizer synth = MidiService.getInstance().getSynthesizer();
                if (synth == null || !synth.isOpen()) {
                    logger.warn("Synthesizer not available");
                    return false;
                }

                // Check if soundbank is supported
                if (!synth.isSoundbankSupported(soundbank)) {
                    logger.warn("Soundbank not supported by synthesizer: {}", soundbankName);
                    return false;
                }

                // Load the soundbank (unload previous if needed)
                synth.unloadAllInstruments(synth.getDefaultSoundbank());
                boolean success = synth.loadAllInstruments(soundbank);

                // Store the soundbank name in the instrument
                if (success) {
                    instrument.setSoundbankName(soundbankName);
                    logger.info("Successfully loaded soundbank: {}", soundbankName);
                }

                return success;
            } else {
                // For external devices, just store the soundbank name
                instrument.setSoundbankName(soundbankName);
                logger.info("Set soundbank name for external instrument: {}", soundbankName);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error applying soundbank: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if soundbanks are properly loaded and load them if not
     *
     * @return true if soundbanks are available
     */
    public boolean ensureSoundbanksLoaded() {
        try {
            if (getSoundbankNames().isEmpty()) {
                logger.warn("No soundbanks available, attempting to reload");
                // Add your soundbank loading code here
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Error checking soundbanks: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initialize synthesizer for soundbank operations
     */
    public boolean initializeSynthesizer() {
        try {
            if (synthesizer == null || !synthesizer.isOpen()) {
                // Get default synthesizer
                synthesizer = MidiSystem.getSynthesizer();
                synthesizer.open();
                logger.info("Synthesizer initialized: {}", synthesizer.getDeviceInfo().getName());
                return true;
            }
            return true;
        } catch (Exception e) {
            logger.error("Error initializing synthesizer: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all available soundbanks for a player
     * Returns a list of SoundbankItem objects ready for use in a JComboBox
     *
     * @param player The player to get soundbanks for
     * @return List of available soundbanks formatted for display
     */
    public List<SoundbankItem> getPlayerSoundbanks(Player player) {
        List<SoundbankItem> result = new ArrayList<>();

        if (player == null) {
            logger.warn("Cannot get soundbanks for null player");
            return result;
        }

        // Get all soundbank names
        List<String> soundbanks = getSoundbankNames();

        // Convert to SoundbankItem objects
        for (String soundbank : soundbanks) {
            result.add(new SoundbankItem(soundbank));
        }

        return result;
    }

    /**
     * Get all available banks for a player's selected soundbank
     * Returns a list of BankItem objects ready for use in a JComboBox
     *
     * @param player        The player to get banks for
     * @param soundbankName The name of the selected soundbank
     * @return List of available banks formatted for display
     */
    public List<BankItem> getPlayerBanks(Player player, String soundbankName) {
        List<BankItem> result = new ArrayList<>();

        if (player == null || soundbankName == null || soundbankName.isEmpty()) {
            logger.warn("Cannot get banks for null player or empty soundbank name");
            return result;
        }

        // Get available banks for this soundbank
        List<Integer> banks = getAvailableBanksByName(soundbankName);

        // Convert to BankItem objects
        for (Integer bankIndex : banks) {
            String bankName = "Bank " + bankIndex;
            result.add(new BankItem(bankIndex, bankName));
        }

        return result;
    }

    /**
     * Get all available presets for a player's selected soundbank and bank
     * Returns a list of PresetItem objects ready for use in a JComboBox
     *
     * @param player        The player to get presets for
     * @param soundbankName The name of the selected soundbank
     * @param bankIndex     The index of the selected bank
     * @return List of available presets formatted for display
     */
    public List<PresetItem> getPlayerPresets(Player player, String soundbankName, Integer bankIndex) {
        List<PresetItem> result = new ArrayList<>();

        if (player == null) {
            logger.warn("Cannot get presets for null player");
            return result;
        }

        // Handle special case for drum channel (channel 9)
        if (player.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL) {
            return getDrumPresets();
        }

        // For non-drum channels, get presets from soundbank and bank
        if (soundbankName != null && !soundbankName.isEmpty() && bankIndex != null) {
            List<String> presetNames = getPresetNames(soundbankName, bankIndex);

            // If no presets found in the specified bank/soundbank, use general MIDI presets
            if (presetNames.isEmpty()) {
                presetNames = getGeneralMIDIPresetNames();
            }

            // Create PresetItems
            for (int i = 0; i < presetNames.size(); i++) {
                String name = presetNames.get(i);
                if (name != null && !name.isEmpty()) {
                    result.add(new PresetItem(i, name));
                }
            }
        } else {
            // Fall back to general MIDI presets if soundbank or bank not specified
            List<String> presetNames = getGeneralMIDIPresetNames();
            for (int i = 0; i < presetNames.size(); i++) {
                result.add(new PresetItem(i, presetNames.get(i)));
            }
        }

        return result;
    }

    /**
     * Get drum presets for a player on channel 9
     * Returns a list of PresetItem objects ready for use in a JComboBox
     *
     * @return List of available drum presets formatted for display
     */
    public List<PresetItem> getDrumPresets() {
        List<PresetItem> result = new ArrayList<>();

        // Get all drum items
        List<DrumItem> drums = getDrumItems();

        // Convert to PresetItems
        for (DrumItem drum : drums) {
            result.add(new PresetItem(drum.getNoteNumber(), drum.getName()));
        }

        return result;
    }

    /**
     * Update a player's instrument to use the selected soundbank, bank, and preset
     * This method handles all the necessary updates and sends the appropriate events
     *
     * @param player        The player to update
     * @param soundbankName The name of the selected soundbank
     * @param bankIndex     The index of the selected bank
     * @param presetNumber  The number of the selected preset
     * @return true if the update was successful
     */

    // TODO: handle external vs internal instrument thing
    public boolean updatePlayerSound(Player player, String soundbankName, Integer bankIndex, Integer presetNumber) {
        if (player == null || player.getInstrument() == null) {
            logger.warn("Cannot update sound for null player or instrument");
            return false;
        }

        try {
            InstrumentWrapper instrument = player.getInstrument();

            // Update soundbank if specified
            if (soundbankName != null && !soundbankName.isEmpty()) {
                instrument.setSoundbankName(soundbankName);
            }

            // Update bank if specified
            if (bankIndex != null) {
                instrument.setBankIndex(bankIndex);
            }

            // Update preset if specified
            if (presetNumber != null && instrument.getChannel() != SequencerConstants.MIDI_DRUM_CHANNEL) {
                instrument.setPreset(presetNumber);

                // Apply the changes
                if (bankIndex != null && presetNumber != null) {
                    applyPresetChangeToPlayer(player, soundbankName, bankIndex, presetNumber);
                }

                // Create a preset change event
                CommandBus.getInstance().publish(
                        Commands.PLAYER_PRESET_CHANGE_EVENT,
                        this,
                        new PlayerPresetChangeEvent(this, player, soundbankName, bankIndex, presetNumber)
                );
            } else if (presetNumber != null && instrument.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL) {
                player.setRootNote(presetNumber);
                player.setName(SoundbankService.getInstance().getDrumName(presetNumber));
                // Create a preset change event
                CommandBus.getInstance().publish(
                        Commands.PLAYER_UPDATE_EVENT,
                        this,
                        new PlayerUpdateEvent(this, player)
                );

            }

            return true;
        } catch (Exception e) {
            logger.error("Error updating player sound: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Update instrument UI components with current instrument settings
     *
     * @param instrument     The instrument to get settings from
     * @param soundbankCombo Soundbank combo box to update
     * @param bankCombo      Bank combo box to update
     * @param presetCombo    Preset combo box to update
     */
    public void updateInstrumentUIComponents(InstrumentWrapper instrument,
                                             JComboBox<String> soundbankCombo,
                                             JComboBox<Integer> bankCombo,
                                             JComboBox<PresetItem> presetCombo) {
        if (instrument == null) {
            return;
        }

        // Remember current state to prevent events
        String currentSoundbank = soundbankCombo.getSelectedItem() != null ?
                soundbankCombo.getSelectedItem().toString() : null;
        Integer currentBank = bankCombo.getSelectedItem() != null ?
                (Integer) bankCombo.getSelectedItem() : null;
        int currentPreset = presetCombo.getSelectedItem() instanceof PresetItem ?
                ((PresetItem) presetCombo.getSelectedItem()).getNumber() : -1;

        try {
            // Update soundbank combo
            soundbankCombo.removeAllItems();
            List<String> soundbanks = getSoundbankNames();
            for (String name : soundbanks) {
                soundbankCombo.addItem(name);
            }

            // Select instrument's soundbank
            if (instrument.getSoundbankName() != null) {
                soundbankCombo.setSelectedItem(instrument.getSoundbankName());
            } else if (soundbankCombo.getItemCount() > 0) {
                soundbankCombo.setSelectedIndex(0);
            }

            // Update bank combo
            bankCombo.removeAllItems();
            String selectedSoundbank = soundbankCombo.getSelectedItem() != null ?
                    soundbankCombo.getSelectedItem().toString() : null;

            if (selectedSoundbank != null) {
                List<Integer> banks = getAvailableBanksByName(selectedSoundbank);
                for (Integer bank : banks) {
                    bankCombo.addItem(bank);
                }

                // Select instrument's bank
                if (instrument.getBankIndex() != null) {
                    bankCombo.setSelectedItem(instrument.getBankIndex());
                } else if (bankCombo.getItemCount() > 0) {
                    bankCombo.setSelectedIndex(0);
                }
            }

            // Update preset combo
            presetCombo.removeAllItems();
            Integer selectedBank = bankCombo.getSelectedItem() != null ?
                    (Integer) bankCombo.getSelectedItem() : null;

            if (selectedSoundbank != null && selectedBank != null) {
                List<String> presetNames = getPresetNames(selectedSoundbank, selectedBank);

                // If no presets for this bank, use General MIDI
                if (presetNames.isEmpty()) {
                    presetNames = getGeneralMIDIPresetNames();
                }

                // Add presets to combo
                for (int i = 0; i < presetNames.size(); i++) {
                    presetCombo.addItem(new PresetItem(i, presetNames.get(i)));
                }

                // Select instrument's preset
                if (instrument.getPreset() != null) {
                    for (int i = 0; i < presetCombo.getItemCount(); i++) {
                        PresetItem item = presetCombo.getItemAt(i);
                        if (item.getNumber() == instrument.getPreset()) {
                            presetCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                } else if (presetCombo.getItemCount() > 0) {
                    presetCombo.setSelectedIndex(0);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating instrument UI components: {}", e.getMessage(), e);
        }
    }

    /**
     * Play a preview note for a specific player
     *
     * @param player     The player to preview
     * @param durationMs Duration of preview note in milliseconds
     */
    public void playPreviewNote(Player player, int durationMs) {
        if (player == null) {
            return;
        }

        try {
            // First apply the current preset
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument != null) {
                // Apply current preset settings
                Integer bankIndex = instrument.getBankIndex();
                Integer preset = instrument.getPreset();
                String soundbank = instrument.getSoundbankName();

                // Apply changes directly to player
                applyPresetChangeToPlayer(player, soundbank, bankIndex, preset);

                // For drum channel, play root note or default kick drum
                if (player.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL) {
                    int noteNumber = player.getRootNote() != null ? player.getRootNote() : 36; // Default to kick
                    MidiService.getInstance().playNote(noteNumber, 100, 9);
                    player.noteOn(noteNumber, 100, durationMs);
                } else {
                    // For melodic instruments, play middle C
                    MidiService.getInstance().playNote(60, 100, player.getChannel());
                    player.noteOn(60, 100, durationMs);
                }
            }
        } catch (Exception e) {
            logger.error("Error playing preview note: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.LOAD_SOUNDBANK:
                if (action.getData() instanceof File) {
                    String name = loadSoundbank((File) action.getData());
                    if (name != null) {
                        CommandBus.getInstance().publish(
                                Commands.SOUNDBANK_LOADED,
                                this,
                                name);
                    } else {
                        CommandBus.getInstance().publish(
                                Commands.STATUS_UPDATE,
                                this,
                                new StatusUpdate("SoundbankManager", "Error", "Failed to load soundbank"));
                    }
                }
                break;

            case Commands.REFRESH_SOUNDBANKS:
                boolean success = initializeSoundbanks();
                CommandBus.getInstance().publish(
                        Commands.SOUNDBANKS_REFRESHED,
                        this,
                        success);
                break;
        }
    }
}