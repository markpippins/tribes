package com.angrysurfer.core.util.demo;
// package com.angrysurfer.core;

// import javax.sound.midi.InvalidMidiDataException;
// import javax.sound.midi.MetaEventListener;
// import javax.sound.midi.MetaMessage;
// import javax.sound.midi.MidiDevice;
// import javax.sound.midi.MidiEvent;
// import javax.sound.midi.MidiMessage;
// import javax.sound.midi.MidiSystem;
// import javax.sound.midi.MidiUnavailableException;
// import javax.sound.midi.Receiver;
// import javax.sound.midi.Sequence;
// import javax.sound.midi.Sequencer;
// import javax.sound.midi.ShortMessage;
// import javax.sound.midi.Track;
// import javax.sound.midi.Transmitter;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.angrysurfer.core.util.PreciseTimer;
// import com.angrysurfer.spring.service.MIDIService;

// public class SequencerDemo {
//     static final Logger logger = LoggerFactory.getLogger(SequencerDemo.class);

//     public static class TimingListener implements MetaEventListener {
//         private static final int TIMING_CLOCK = 0x58;
//         private static final int TEMPO_CHANGE = 0x51;
        
//         @Override
//         public void meta(MetaMessage meta) {
//             if (meta.getType() == TIMING_CLOCK) {
//                 byte[] data = meta.getData();
//                 int numerator = data[0];
//                 int denominator = (int) Math.pow(2, data[1]);
//                 int clocksPerClick = data[2];
//                 int thirtySecondPer24Clocks = data[3];
//                 logger.info("Time Signature: {}/{}, Clocks/Click: {}, 32nd/24Clocks: {}", 
//                     numerator, denominator, clocksPerClick, thirtySecondPer24Clocks);
//             }
//             else if (meta.getType() == TEMPO_CHANGE) {
//                 byte[] data = meta.getData();
//                 int tempo = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
//                 float bpm = 60000000f / tempo;
//                 logger.info("Tempo change: {} BPM", bpm);
//             }
//         }
//     }

//     public static class ClockListener implements Receiver {
//         private long tickCount = 0;
//         private static final int MIDI_TIMING_CLOCK = 0xF8;
        
//         @Override
//         public void send(MidiMessage message, long timeStamp) {
//             if (message instanceof ShortMessage) {
//                 ShortMessage sm = (ShortMessage) message;
//                 if (sm.getStatus() == MIDI_TIMING_CLOCK) {
//                     tickCount++;
//                     if (tickCount % 24 == 0) {
//                         logger.info("Beat: {}", tickCount / 24);
//                     } else {
//                         logger.debug("Tick: {}", tickCount);
//                     }
//                 }
//             }
//         }

//         @Override
//         public void close() {}
//     }

//     public static void main(String[] args) {
//         try {
//             Sequence sequence = new Sequence(Sequence.PPQ, 24);
//             Track track1 = sequence.createTrack();

//             // First track - C major scale
//             int channel = MIDIService.midiChannel(2);
//             int velocity = 100;
//             int ticksPerWholeNote = 24 * 4;

//             for (int note = 36; note < 36 + 8; note++) {
//                 track1.add(new MidiEvent(
//                         new ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity),
//                         (note - 36) * ticksPerWholeNote));
//                 track1.add(new MidiEvent(
//                         new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0),
//                         ((note - 36) * ticksPerWholeNote) + ticksPerWholeNote - 1));
//             }

//             // List all MIDI devices
//             // System.out.println("\nAvailable MIDI Devices:");
//             MIDIService.getMidiOutDevices().forEach(device -> // System.out.println(device.getDeviceInfo().getName()));

//             // Get the Tracker device using the new method
//             MidiDevice outputDevice = MIDIService.getMidiDevice("Tracker");
//             if (outputDevice == null) {
//                 // System.out.println("Tracker MIDI device not found!");
//                 return;
//             }

//             // Open the output device
//             if (!outputDevice.isOpen()) {
//                 outputDevice.open();
//             }

//             // Get and setup the sequencer
//             Sequencer sequencer = MidiSystem.getSequencer(false); // false = don't connect to default synthesizer
//             sequencer.open();
//             sequencer.setSequence(sequence);
//             sequencer.addMetaEventListener(new TimingListener());

//             // Add timing metadata to sequence
//             // Add time signature (4/4)
//             byte[] timeSignature = {0x04, 0x02, 24, 0x08};
//             MetaMessage tsMsg = new MetaMessage(0x58, timeSignature, timeSignature.length);
//             track1.add(new MidiEvent(tsMsg, 0));
            
//             // Add tempo (120 BPM)
//             int tempo = 500000; // microseconds per quarter note (120 BPM)
//             byte[] tempoBytes = {(byte)(tempo >> 16), (byte)(tempo >> 8), (byte)tempo};
//             MetaMessage tempoMsg = new MetaMessage(0x51, tempoBytes, 3);
//             track1.add(new MidiEvent(tempoMsg, 0));

//             // Connect sequencer to external device
//             Transmitter transmitter = sequencer.getTransmitter();
//             Receiver receiver = outputDevice.getReceiver();
//             transmitter.setReceiver(receiver);

//             // Setup sequencer with both timing and clock listeners
//             sequencer.addMetaEventListener(new TimingListener());
            
//             // Create a splitter to send MIDI to both the device and our clock listener
//             Receiver deviceReceiver = outputDevice.getReceiver();
//             Receiver clockListener = new ClockListener();
            
//             Receiver splitter = new Receiver() {
//                 @Override
//                 public void send(MidiMessage message, long timeStamp) {
//                     deviceReceiver.send(message, timeStamp);
//                     clockListener.send(message, timeStamp);
//                 }
                
//                 @Override
//                 public void close() {
//                     deviceReceiver.close();
//                     clockListener.close();
//                 }
//             };
            
//             transmitter.setReceiver(splitter);

//             // Create and start the precise timer
//             PreciseTimer timer = new PreciseTimer(120, 24); // 120 BPM, 24 PPQ
//             // timer.addTickListener(() -> logger.debug("Tick: {}", timer.getCurrentTick()));
//             // timer.addBeatListener(() -> logger.info("Beat: {}", timer.getCurrentTick() / 24));
            
//             Thread timerThread = new Thread(timer);
//             timerThread.setPriority(Thread.MAX_PRIORITY);
//             timerThread.start();

//             // Start playback as before
//             sequencer.start();

//             // Wait 4 seconds then add second track
//             Thread.sleep(4000);

//             // Add second track while sequencer is running
//             Track track2 = sequence.createTrack();
//             // channel = 1; // Use different channel

//             // Add some higher notes on second track
//             for (int note = 48; note < 48 + 8; note++) {
//                 track2.add(new MidiEvent(
//                         new ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity),
//                         (note - 48) * ticksPerWholeNote));
//                 track2.add(new MidiEvent(
//                         new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0),
//                         ((note - 48) * ticksPerWholeNote) + ticksPerWholeNote - 1));
//             }

//             // Wait until playback is done
//             while (sequencer.isRunning()) {
//                 Thread.sleep(100);
//             }

//             // Cleanup
//             timer.stop();
//             timerThread.join();
//             transmitter.close();
//             splitter.close();
//             sequencer.close();
//             outputDevice.close();

//         } catch (InvalidMidiDataException | MidiUnavailableException | InterruptedException e) {
//             logger.error("Sequencer demo failed", e);
//         }
//     }
// }
