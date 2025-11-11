package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.sequencer.SequencerConstants;

/**
 * Unified MIDI service - handles devices, receivers, and the internal synthesizer.
 * Replaces: DeviceManager, ReceiverManager, InternalSynthManager
 */
public class MidiService {
    private static final Logger logger = LoggerFactory.getLogger(MidiService.class);
    private static MidiService instance;

    // Device and receiver management
    private final Map<String, MidiDevice> deviceCache = new ConcurrentHashMap<>();
    private final Map<String, Receiver> receiverCache = new ConcurrentHashMap<>();
    
    // Internal synthesizer
    private Synthesizer synthesizer;
    private MidiChannel[] cachedChannels;

    private MidiService() {
    }

    public static synchronized MidiService getInstance() {
        if (instance == null) {
            instance = new MidiService();
        }
        return instance;
    }

    public void initialize() {
        initializeSynthesizer();
    }

    // ========== Synthesizer Management ==========

    private void initializeSynthesizer() {
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                if (info.getName().contains(SequencerConstants.GERVILL)) {
                    synthesizer = (Synthesizer) MidiSystem.getMidiDevice(info);
                    break;
                }
            }

            if (synthesizer == null) {
                synthesizer = MidiSystem.getSynthesizer();
            }

            if (synthesizer != null && !synthesizer.isOpen()) {
                synthesizer.open();
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                cachedChannels = synthesizer.getChannels();
                logger.info("Synthesizer initialized: {}", synthesizer.getDeviceInfo().getName());
            }
        } catch (Exception e) {
            logger.error("Error initializing synthesizer", e);
        }
    }

    public Synthesizer getSynthesizer() {
        if (synthesizer == null || !synthesizer.isOpen()) {
            initializeSynthesizer();
        }
        return synthesizer;
    }

    public boolean isInternalSynth(InstrumentWrapper instrument) {
        if (instrument == null) return false;
        
        String deviceName = instrument.getDeviceName();
        return deviceName != null && 
               (deviceName.equals(SequencerConstants.GERVILL) || 
                deviceName.contains("Java Sound Synthesizer"));
    }

    public String getDrumName(Integer noteNumber) {
        if (noteNumber == null || noteNumber < 35 || noteNumber > 81) {
            return "Unknown";
        }
        
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
        
        int index = noteNumber - 35;
        if (index >= 0 && index < drumNames.length) {
            return drumNames[index];
        }
        return "Unknown";
    }

    public boolean isInternalSynthInstrument(InstrumentWrapper instrument) {
        return isInternalSynth(instrument);
    }

    public Receiver getOrCreateReceiver(String deviceName, MidiDevice device) {
        Receiver cached = receiverCache.get(deviceName);
        if (cached != null) {
            return cached;
        }

        if (device == null) {
            device = getDevice(deviceName);
        }

        if (device != null && openDevice(device)) {
            try {
                if (device.getMaxReceivers() != 0) {
                    Receiver receiver = device.getReceiver();
                    receiverCache.put(deviceName, receiver);
                    return receiver;
                }
            } catch (Exception e) {
                logger.error("Failed to get receiver for {}", deviceName, e);
            }
        }

        return null;
    }

    public void playNote(int note, int velocity, int channel) {
        if (synthesizer == null || !synthesizer.isOpen()) return;
        if (cachedChannels == null) cachedChannels = synthesizer.getChannels();
        if (channel < 0 || channel >= cachedChannels.length) return;

        MidiChannel midiChannel = cachedChannels[channel];
        if (midiChannel != null) {
            midiChannel.noteOn(note, velocity);
        }
    }

    public void playNote(int note, int velocity, int durationMs, int channel) {
        playNote(note, velocity, channel);
        
        // Schedule note off after duration
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
                stopNote(note, channel);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void stopNote(int note, int channel) {
        if (synthesizer == null || !synthesizer.isOpen()) return;
        if (cachedChannels == null) cachedChannels = synthesizer.getChannels();
        if (channel < 0 || channel >= cachedChannels.length) return;

        MidiChannel midiChannel = cachedChannels[channel];
        if (midiChannel != null) {
            midiChannel.noteOff(note);
        }
    }

    public void allNotesOff(int channel) {
        if (synthesizer == null || !synthesizer.isOpen()) return;
        if (cachedChannels == null) cachedChannels = synthesizer.getChannels();
        if (channel >= 0 && channel < cachedChannels.length) {
            cachedChannels[channel].controlChange(123, 0);
        }
    }

    public void setControlChange(int channel, int ccNumber, int value) {
        if (synthesizer == null || !synthesizer.isOpen()) return;
        if (cachedChannels == null) cachedChannels = synthesizer.getChannels();
        if (channel >= 0 && channel < cachedChannels.length) {
            MidiChannel midiChannel = cachedChannels[channel];
            if (midiChannel != null) {
                midiChannel.controlChange(ccNumber, value);
            }
        }
    }

    // ========== Device Management ==========

    public MidiDevice getDevice(String name) {
        MidiDevice cached = deviceCache.get(name);
        if (cached != null && cached.isOpen()) {
            return cached;
        }

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            if (info.getName().contains(name)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    deviceCache.put(name, device);
                    return device;
                } catch (MidiUnavailableException e) {
                    logger.debug("Error accessing MIDI device: {}", name);
                }
            }
        }
        return null;
    }

    public MidiDevice getDevice(MidiDevice.Info info) {
        if (info == null) return null;
        
        try {
            MidiDevice device = MidiSystem.getMidiDevice(info);
            if (device != null) {
                deviceCache.put(info.getName(), device);
            }
            return device;
        } catch (MidiUnavailableException e) {
            logger.debug("Error accessing MIDI device: {}", info.getName());
            return null;
        }
    }

    public List<MidiDevice> getOutputDevices() {
        List<MidiDevice> devices = new ArrayList<>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxReceivers() != 0) {
                    devices.add(device);
                }
            } catch (MidiUnavailableException e) {
                logger.debug("Error accessing device: {}", info.getName());
            }
        }
        return devices;
    }

    public List<String> getOutputDeviceNames() {
        return getOutputDevices().stream()
                .map(device -> device.getDeviceInfo().getName())
                .toList();
    }

    public MidiDevice getDefaultOutputDevice() {
        MidiDevice device = getDevice(SequencerConstants.GERVILL);
        if (device != null) {
            openDevice(device);
            if (device.isOpen()) return device;
        }

        List<MidiDevice> devices = getOutputDevices();
        for (MidiDevice d : devices) {
            if (d.getMaxReceivers() != 0 && 
                !d.getDeviceInfo().getName().contains("Real Time Sequencer")) {
                openDevice(d);
                if (d.isOpen()) return d;
            }
        }

        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            openDevice(synth);
            return synth;
        } catch (Exception e) {
            logger.error("Could not get default synthesizer", e);
        }

        return null;
    }

    public boolean openDevice(MidiDevice device) {
        if (device == null) return false;
        
        try {
            if (!device.isOpen()) {
                device.open();
            }
            return device.isOpen();
        } catch (MidiUnavailableException e) {
            logger.error("Failed to open device", e);
            return false;
        }
    }

    // ========== Receiver Management ==========

    public Receiver getReceiver(String deviceName) {
        Receiver cached = receiverCache.get(deviceName);
        if (cached != null) {
            return cached;
        }

        MidiDevice device = getDevice(deviceName);
        if (device == null) return null;

        if (!openDevice(device)) return null;

        try {
            if (device.getMaxReceivers() != 0) {
                Receiver receiver = device.getReceiver();
                receiverCache.put(deviceName, receiver);
                return receiver;
            }
        } catch (Exception e) {
            logger.error("Failed to get receiver for {}", deviceName, e);
        }

        return null;
    }

    public void closeReceiver(String deviceName) {
        Receiver receiver = receiverCache.remove(deviceName);
        if (receiver != null) {
            try {
                receiver.close();
            } catch (Exception e) {
                logger.debug("Error closing receiver", e);
            }
        }
    }

    public void clearAllReceivers() {
        receiverCache.keySet().forEach(this::closeReceiver);
        receiverCache.clear();
    }

    // ========== Preset Application ==========

    public boolean applyPreset(InstrumentWrapper instrument, int channel, Integer bankIndex, Integer preset) {
        if (instrument == null) return false;

        bankIndex = bankIndex != null ? bankIndex : 0;
        preset = preset != null ? preset : 0;

        try {
            if (isInternalSynth(instrument)) {
                return applyPresetToSynth(channel, bankIndex, preset);
            } else {
                return applyPresetToExternal(instrument, channel, bankIndex, preset);
            }
        } catch (Exception e) {
            logger.error("Failed to apply preset", e);
            return false;
        }
    }

    private boolean applyPresetToSynth(int channel, int bankIndex, int preset) {
        if (synthesizer == null || !synthesizer.isOpen()) return false;
        if (cachedChannels == null) cachedChannels = synthesizer.getChannels();
        if (channel < 0 || channel >= cachedChannels.length) return false;

        MidiChannel midiChannel = cachedChannels[channel];
        if (midiChannel != null) {
            midiChannel.controlChange(0, (bankIndex >> 7) & 0x7F);
            midiChannel.controlChange(32, bankIndex & 0x7F);
            midiChannel.programChange(preset);
            return true;
        }
        return false;
    }

    private boolean applyPresetToExternal(InstrumentWrapper instrument, int channel, int bankIndex, int preset) {
        try {
            instrument.controlChange(0, (bankIndex >> 7) & 0x7F);
            instrument.controlChange(32, bankIndex & 0x7F);
            instrument.programChange(preset, 0);
            return true;
        } catch (Exception e) {
            logger.error("Failed to apply external preset", e);
            return false;
        }
    }

    // ========== Cleanup ==========

    public void shutdown() {
        clearAllReceivers();
        
        if (synthesizer != null && synthesizer.isOpen()) {
            synthesizer.close();
        }

        deviceCache.values().forEach(device -> {
            if (device.isOpen()) {
                device.close();
            }
        });
        deviceCache.clear();
    }
}
