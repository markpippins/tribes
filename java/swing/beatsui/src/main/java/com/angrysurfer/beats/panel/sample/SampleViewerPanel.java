package com.angrysurfer.beats.panel.sample;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Sample;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Panel for displaying and editing audio samples
 */
public class SampleViewerPanel extends JPanel implements
        WaveformControlsPanel.AudioControlListener,
        SamplePropertiesPanel.SamplePropertyChangeListener {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SampleViewerPanel.class);

    // UI Components
    private final WaveformPanel waveformPanel;
    private final JLabel fileInfoLabel;
    private final JSplitPane splitPane;
    // Sample data model
    private final Sample sample = new Sample();
    // Background processing
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WaveformControlsPanel controlsPanel;
    private SamplePropertiesPanel propertiesPanel;
    // Audio playback
    private Clip audioClip;
    private double duration;
    // Playhead update timer
    private Timer playheadTimer;

    public SampleViewerPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create file info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        fileInfoLabel = new JLabel("No file loaded");
        infoPanel.add(fileInfoLabel, BorderLayout.WEST);

        // Create component panels
        waveformPanel = new WaveformPanel();
        waveformPanel.setWaveformChangeListener(sample -> {
            // Update properties panel when markers change via waveform interaction
            propertiesPanel.setSample(sample);

            // Update controls display
            controlsPanel.updateSelectionLabel(
                    sample.getSampleStart(),
                    sample.getSampleEnd(),
                    sample.getAudioFormat()
            );
        });
        controlsPanel = new WaveformControlsPanel(this);
        propertiesPanel = new SamplePropertiesPanel(this);

        // Create main content panel for left side of split
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.add(waveformPanel, BorderLayout.CENTER);
        mainContent.add(controlsPanel, BorderLayout.SOUTH);

        // Create split pane with main content on left and properties on right
        splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                mainContent,
                propertiesPanel
        );

        // Configure split pane
        splitPane.setResizeWeight(0.8); // Give more space to waveform by default
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);

        // Add panels to main layout
        add(infoPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // Set divider location after components are laid out
        SwingUtilities.invokeLater(() -> {
            int width = getWidth();
            if (width > 0) {
                // Set initial divider position to ~75% of panel width
                splitPane.setDividerLocation(0.75);
            }
        });
    }

    /**
     * Load audio file and display waveform
     */
    public void loadAudioFile(File file) throws IOException,
            UnsupportedAudioFileException, LineUnavailableException {

        // Store file reference in sample
        sample.setAudioFile(file);

        // Update UI immediately to indicate loading
        fileInfoLabel.setText("Loading: " + file.getName() + "...");
        waveformPanel.resetWaveform();

        // Clear previous audio clip
        stopAndCloseAudio();

        // Load audio data in background
        executor.submit(() -> {
            try {
                // Get audio input stream
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
                AudioFormat audioFormat = audioInputStream.getFormat();
                sample.setAudioFormat(audioFormat);

                // Store audio properties in sample
                sample.setSampleRate((int) audioFormat.getSampleRate());
                sample.setChannels(audioFormat.getChannels());
                sample.setSampleSizeInBits(audioFormat.getSampleSizeInBits());

                // Calculate duration and frame count
                long frameLength = audioInputStream.getFrameLength();
                int totalFrames = (int) frameLength;
                duration = frameLength / audioFormat.getFrameRate();

                // Initialize selection points to select entire sample by default
                sample.setSampleStart(0);
                sample.setSampleEnd(totalFrames);
                sample.setLoopStart(0);
                sample.setLoopEnd(totalFrames);

                // Read audio data
                byte[] audioData = readAllBytes(audioInputStream);
                sample.setAudioData(audioData);

                // Generate waveform data
                int[] waveformData = generateWaveformData(audioData, audioFormat);
                sample.setWaveformData(waveformData);

                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    updateFileInfoLabel();
                    updateControlsWithSample();

                    // Update waveform display
                    waveformPanel.setSample(sample);

                    // Update properties panel
                    propertiesPanel.setSample(sample);

                    // Enable the Create Player button
                    controlsPanel.setCreatePlayerEnabled(true);
                });

                // Prepare audio clip for playback
                prepareAudioClip();

            } catch (Exception e) {
                logger.error("Error loading audio file: {}", e.getMessage(), e);
                SwingUtilities.invokeLater(() -> {
                    fileInfoLabel.setText("Error loading: " + file.getName());
                });
            }
        });
    }

    private void updateControlsWithSample() {
        // Enable playback controls
        controlsPanel.setPlayEnabled(true);
        controlsPanel.setPlaySelectionEnabled(true); // Enable selection playback
        controlsPanel.setStopEnabled(false);

        // Update duration label
        controlsPanel.updateDurationLabel(duration);

        // Update selection label
        controlsPanel.updateSelectionLabel(
                sample.getSampleStart(),
                sample.getSampleEnd(),
                sample.getAudioFormat()
        );
    }

    /**
     * Read all bytes from an audio input stream
     */
    private byte[] readAllBytes(AudioInputStream stream) throws IOException {
        // Get estimated size
        long estimatedSize = stream.getFrameLength() * stream.getFormat().getFrameSize();
        if (estimatedSize > Integer.MAX_VALUE) {
            throw new IOException("Audio file too large");
        }

        // Read all bytes
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) estimatedSize);
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        return out.toByteArray();
    }

    /**
     * Generate waveform data from audio bytes
     */
    private int[] generateWaveformData(byte[] audioData, AudioFormat format) {
        if (audioData == null) return new int[0];

        int frameSize = format.getFrameSize();
        int channels = format.getChannels();
        int sampleSizeInBits = format.getSampleSizeInBits();
        int numFrames = audioData.length / frameSize;

        // For visualization, we'll use fewer points to improve performance
        int downsampleFactor = Math.max(1, numFrames / 2000);
        int waveformSize = numFrames / downsampleFactor;
        int[] waveformData = new int[waveformSize];

        // Process audio data based on format
        for (int i = 0; i < waveformSize; i++) {
            int frameIndex = i * downsampleFactor;
            int sampleValue = 0;

            // Calculate average value for this segment
            for (int j = 0; j < downsampleFactor && (frameIndex + j) * frameSize < audioData.length; j++) {
                int byteIndex = (frameIndex + j) * frameSize;

                // Handle different sample sizes and channels
                if (sampleSizeInBits == 16) {
                    // 16-bit audio (convert bytes to short)
                    for (int ch = 0; ch < channels; ch++) {
                        int chOffset = ch * (sampleSizeInBits / 8);
                        short sample = (short) ((audioData[byteIndex + chOffset] & 0xFF) |
                                ((audioData[byteIndex + chOffset + 1] & 0xFF) << 8));
                        sampleValue += Math.abs(sample);
                    }
                } else if (sampleSizeInBits == 8) {
                    // 8-bit audio
                    for (int ch = 0; ch < channels; ch++) {
                        int chOffset = ch * (sampleSizeInBits / 8);
                        byte sample = audioData[byteIndex + chOffset];
                        sampleValue += Math.abs(sample - 128);
                    }
                }
            }

            // Average across channels and samples
            sampleValue /= (downsampleFactor * channels);

            // Store value (normalized to 0-100 range for display)
            if (sampleSizeInBits == 16) {
                waveformData[i] = sampleValue * 100 / 32768;
            } else {
                waveformData[i] = sampleValue * 100 / 128;
            }
        }

        return waveformData;
    }

    /**
     * Prepare audio clip for playback
     */
    private void prepareAudioClip() {
        try {
            // Create clip for playback
            DataLine.Info info = new DataLine.Info(Clip.class, sample.getAudioFormat());

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Audio format not supported for playback");
            }

            // Get and open the clip
            audioClip = (Clip) AudioSystem.getLine(info);

            // Create new audio input stream from the data
            AudioInputStream ais = new AudioInputStream(
                    new java.io.ByteArrayInputStream(sample.getAudioData()),
                    sample.getAudioFormat(),
                    sample.getAudioData().length / sample.getAudioFormat().getFrameSize());

            audioClip.open(ais);

            // Add listener to update UI when playback stops
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    SwingUtilities.invokeLater(() -> {
                        stopPlayheadUpdate(); // Stop playhead updates
                        controlsPanel.setStopEnabled(false);
                        controlsPanel.setPlayEnabled(true);
                        controlsPanel.setPlaySelectionEnabled(true);
                    });
                }
            });
        } catch (Exception e) {
            logger.error("Error preparing audio clip: {}", e.getMessage(), e);
        }
    }

    /**
     * Update file info label with audio properties
     */
    private void updateFileInfoLabel() {
        if (sample == null || sample.getAudioFile() == null || sample.getAudioFormat() == null) {
            fileInfoLabel.setText("No file loaded");
            return;
        }

        // Format audio properties
        String info = String.format("%s (%d Hz, %d-bit, %s)",
                sample.getAudioFile().getName(),
                sample.getSampleRate(),
                sample.getSampleSizeInBits(),
                sample.getChannels() == 1 ? "Mono" : "Stereo");

        fileInfoLabel.setText(info);
    }

    /**
     * Stop playback and close audio resources
     */
    private void stopAndCloseAudio() {
        if (audioClip != null) {
            if (audioClip.isRunning()) {
                audioClip.stop();
            }
            audioClip.close();
            audioClip = null;
        }
    }

    /**
     * Refresh UI after theme change
     */
    public void refreshUI() {
        SwingUtilities.updateComponentTreeUI(this);
        splitPane.setDividerLocation(splitPane.getDividerLocation()); // Preserve divider position
        waveformPanel.repaint();
        repaint();
    }

    @Override
    public void onPlayRequested() {
        if (audioClip == null) return;

        try {
            // Set clip to current sample start position
            audioClip.setFramePosition(sample.getSampleStart());

            // Check if loop points are set
            if (sample.isLoopEnabled() && sample.getLoopStart() < sample.getLoopEnd()
                    && sample.getLoopStart() >= sample.getSampleStart()
                    && sample.getLoopEnd() <= sample.getSampleEnd()) {
                audioClip.setLoopPoints(sample.getLoopStart(), sample.getLoopEnd());
                audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                // Play once without looping
                audioClip.start();
            }

            // Start playhead updates
            startPlayheadUpdate();

            // Update UI
            controlsPanel.setPlayEnabled(false);
            controlsPanel.setPlaySelectionEnabled(false);
            controlsPanel.setStopEnabled(true);

        } catch (Exception e) {
            logger.error("Error playing audio: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onStopRequested() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
        }

        // Stop playhead updates
        stopPlayheadUpdate();

        // Update UI
        controlsPanel.setPlayEnabled(true);
        controlsPanel.setPlaySelectionEnabled(true);
        controlsPanel.setStopEnabled(false);
    }

    @Override
    public void onZoomChanged(double zoomFactor) {
        waveformPanel.setZoomFactor(zoomFactor);
    }

    @Override
    public void onPropertyChanged(Sample sample) {
        // Update waveform display when properties change
        waveformPanel.repaint();

        // Update selection display
        controlsPanel.updateSelectionLabel(
                sample.getSampleStart(),
                sample.getSampleEnd(),
                sample.getAudioFormat()
        );
    }

    @Override
    public void onPlaySelectionRequested() {
        if (audioClip == null) {
            logger.error("Audio clip not initialized");
            return;
        }

        try {
            // Stop any current playback
            if (audioClip.isRunning()) {
                audioClip.stop();
            }

            // Check if we have a valid selection
            if (sample.getSampleStart() >= sample.getSampleEnd()) {
                logger.warn("Invalid selection range: start({}) >= end({})",
                        sample.getSampleStart(), sample.getSampleEnd());
                return;
            }

            // Debug logging
            logger.debug("Playing selection from frame {} to {}",
                    sample.getSampleStart(), sample.getSampleEnd());

            // Reset any loop settings (important!)
            audioClip.setLoopPoints(0, -1);

            // Set clip to sample start position
            audioClip.setFramePosition(sample.getSampleStart());

            // Check if loop points are set within the selection
            if (sample.isLoopEnabled() &&
                    sample.getLoopStart() >= sample.getSampleStart() &&
                    sample.getLoopEnd() <= sample.getSampleEnd() &&
                    sample.getLoopStart() < sample.getLoopEnd()) {

                // Set loop points relative to clip start
                audioClip.setLoopPoints(sample.getLoopStart(), sample.getLoopEnd());
                audioClip.loop(Clip.LOOP_CONTINUOUSLY);

                // Start the playhead update
                startPlayheadUpdate();
            } else {
                // Play once without looping
                audioClip.start();

                // Start the playhead update
                startPlayheadUpdate();

                // Set up a timer to stop playback when we reach the end of the selection
                Timer playbackTimer = new Timer(10, e -> {
                    if (audioClip == null || !audioClip.isRunning()) {
                        ((Timer) e.getSource()).stop();
                        return;
                    }

                    int currentFrame = audioClip.getFramePosition();
                    if (currentFrame >= sample.getSampleEnd()) {
                        audioClip.stop();
                        ((Timer) e.getSource()).stop();
                        stopPlayheadUpdate();

                        // Update UI when selection playback is done
                        SwingUtilities.invokeLater(() -> {
                            controlsPanel.setPlayEnabled(true);
                            controlsPanel.setPlaySelectionEnabled(true);
                            controlsPanel.setStopEnabled(false);
                        });
                    }
                });
                playbackTimer.start();
            }

            // Update UI
            controlsPanel.setPlayEnabled(false);
            controlsPanel.setPlaySelectionEnabled(false);
            controlsPanel.setStopEnabled(true);

        } catch (Exception e) {
            logger.error("Error playing audio selection: {}", e.getMessage(), e);
        }
    }

    /**
     * Start updating the playhead position during playback
     */
    private void startPlayheadUpdate() {
        // Stop any existing timer
        stopPlayheadUpdate();

        // Create and start a new timer to update playhead
        playheadTimer = new Timer(40, e -> { // 25 fps update rate
            if (audioClip != null && audioClip.isRunning()) {
                int position = audioClip.getFramePosition();
                waveformPanel.setCurrentPlayPosition(position);
            } else {
                stopPlayheadUpdate();
            }
        });
        playheadTimer.start();
    }

    /**
     * Stop updating the playhead
     */
    private void stopPlayheadUpdate() {
        if (playheadTimer != null && playheadTimer.isRunning()) {
            playheadTimer.stop();
        }
        waveformPanel.resetPlayhead();
    }

    /**
     * Clean up resources when panel is closed
     */
    public void cleanup() {
        stopAndCloseAudio();
        stopPlayheadUpdate(); // Stop playhead updates
        executor.shutdown();
    }

    @Override
    public void onCreatePlayerRequested() {
        if (sample == null || sample.getAudioFile() == null) {
            logger.warn("No sample loaded, can't create player");
            return;
        }

        try {
            // Get the current session
            SessionManager sessionManager = SessionManager.getInstance();
            if (sessionManager == null || sessionManager.getActiveSession() == null) {
                logger.error("No active session available");
                JOptionPane.showMessageDialog(this,
                        "No active session available. Please create or load a session first.",
                        "Cannot Create Player",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Create a descriptive name for the player based on sample file
            String sampleName = sample.getAudioFile().getName();
            if (sampleName.toLowerCase().endsWith(".wav")) {
                sampleName = sampleName.substring(0, sampleName.length() - 4);
            }
            String playerName = "Sample: " + sampleName;

            // Create a new instrument wrapper for sample playback
            InstrumentWrapper instrument = createSampleInstrument(sample);

            // Create a new Strike player (for percussive samples)
            Strike player = new Strike();
            player.setName(playerName);
            player.setInstrument(instrument);
            player.setEnabled(true);

            // Initialize selection points
            if (sample.getSampleStart() != 0 || sample.getSampleEnd() != sample.getAudioData().length) {
                // If selection exists, store in player properties
                player.getProperties().put("sampleStart", sample.getSampleStart());
                player.getProperties().put("sampleEnd", sample.getSampleEnd());
            }

            // Add loop points if enabled
            if (sample.isLoopEnabled()) {
                player.getProperties().put("loopEnabled", true);
                player.getProperties().put("loopStart", sample.getLoopStart());
                player.getProperties().put("loopEnd", sample.getLoopEnd());
            }

            // Add the player to the session
            sessionManager.getActiveSession().addPlayer(player);

            // Notify the system about the new player
            CommandBus.getInstance().publish(
                    Commands.PLAYER_ADDED,
                    this,
                    player);

            // Show confirmation to user
            JOptionPane.showMessageDialog(this,
                    "Created new player: " + playerName,
                    "Player Created",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            logger.error("Error creating player from sample: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                    "Error creating player: " + e.getMessage(),
                    "Player Creation Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Create an instrument wrapper for the sample
     */
    private InstrumentWrapper createSampleInstrument(Sample sample) {
        // Create a new instrument wrapper specifically for sample playback
        InstrumentWrapper instrument = new InstrumentWrapper();
        instrument.setName("Sample: " + sample.getAudioFile().getName());
        instrument.setInternal(true);

        // Store sample data in the instrument
        instrument.getProperties().put("sampleFile", sample.getAudioFile().getAbsolutePath());
        instrument.getProperties().put("sampleData", sample.getAudioData());
        instrument.getProperties().put("sampleFormat", sample.getAudioFormat());

        // Configure instrument with sample metadata
        instrument.getProperties().put("sampleRate", sample.getSampleRate());
        instrument.getProperties().put("channels", sample.getChannels());
        instrument.getProperties().put("sampleSizeInBits", sample.getSampleSizeInBits());

        // Mark as a sample instrument type
        instrument.getProperties().put("type", "sample");

        return instrument;
    }

    /**
     * Get the waveform controls panel
     * @return The WaveformControlsPanel instance
     */
    public WaveformControlsPanel getControlsPanel() {
        return controlsPanel;
    }
}
