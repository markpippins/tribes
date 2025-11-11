package com.angrysurfer.beats.panel.sample;

import com.angrysurfer.core.model.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class WaveformPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WaveformPanel.class);
    private static final int MARKER_NONE = 0;
    private static final int MARKER_SAMPLE_START = 1;
    private static final int MARKER_SAMPLE_END = 2;
    private static final int MARKER_LOOP_START = 3;
    private static final int MARKER_LOOP_END = 4;
    // For marker hit detection
    private static final int MARKER_TOLERANCE = 5; // pixels
    // Colors
    private static final Color SELECTION_COLOR = new Color(200, 200, 255, 80); // Light blue, semi-transparent
    private static final Color LOOP_COLOR = new Color(200, 255, 200, 80); // Light green, semi-transparent
    private static final Color PLAYHEAD_COLOR = Color.RED;
    private final int scrollPosition = 0;
    private double zoomFactor = 1.0;
    private Sample sample;
    // For marker dragging operations
    private boolean isDragging = false;
    private int currentMarker = MARKER_NONE;
    private int currentPlayPosition = -1;
    private Timer playheadTimer;
    private WaveformChangeListener listener;

    public WaveformPanel() {
        setBackground(Color.WHITE);

        // Add mouse listeners for marker manipulation
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (sample == null) return;

                // Determine if a marker was clicked
                int x = e.getX();
                currentMarker = getMarkerAtPosition(x);

                if (currentMarker != MARKER_NONE) {
                    isDragging = true;
                } else {
                    // If no marker clicked, determine if we're creating a new selection
                    int frame = screenToFrame(x);

                    // Alt key for loop points, normal for sample points
                    if (e.isAltDown()) {
                        sample.setLoopStart(frame);
                        sample.setLoopEnd(frame);
                        currentMarker = MARKER_LOOP_END; // Will drag the end point
                    } else {
                        sample.setSampleStart(frame);
                        sample.setSampleEnd(frame);
                        currentMarker = MARKER_SAMPLE_END; // Will drag the end point
                    }

                    isDragging = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    isDragging = false;

                    // Ensure proper order of markers (start < end)
                    if (sample != null) {
                        if (sample.getSampleStart() > sample.getSampleEnd()) {
                            int temp = sample.getSampleStart();
                            sample.setSampleStart(sample.getSampleEnd());
                            sample.setSampleEnd(temp);
                        }

                        if (sample.getLoopStart() > sample.getLoopEnd()) {
                            int temp = sample.getLoopStart();
                            sample.setLoopStart(sample.getLoopEnd());
                            sample.setLoopEnd(temp);
                        }
                    }

                    // Notify listeners of changes
                    if (listener != null && sample != null) {
                        listener.onMarkersChanged(sample);
                    }
                }
                currentMarker = MARKER_NONE;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isDragging || sample == null) return;

                int frame = screenToFrame(e.getX());

                // Update the appropriate marker position
                switch (currentMarker) {
                    case MARKER_SAMPLE_START:
                        sample.setSampleStart(frame);
                        break;
                    case MARKER_SAMPLE_END:
                        sample.setSampleEnd(frame);
                        break;
                    case MARKER_LOOP_START:
                        sample.setLoopStart(frame);
                        break;
                    case MARKER_LOOP_END:
                        sample.setLoopEnd(frame);
                        break;
                }

                // Redraw
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Change cursor when over a marker
                if (sample != null) {
                    int x = e.getX();
                    int marker = getMarkerAtPosition(x);

                    if (marker != MARKER_NONE) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }
        });
    }

    public void setSample(Sample sample) {
        this.sample = sample;
        repaint();
    }

    public void setWaveformChangeListener(WaveformChangeListener listener) {
        this.listener = listener;
    }

    public void setZoomFactor(double factor) {
        this.zoomFactor = factor;
        repaint();
    }

    public void resetWaveform() {
        this.sample = null;
        this.repaint();
    }

    /**
     * Set the current play position and update display
     */
    public void setCurrentPlayPosition(int framePosition) {
        this.currentPlayPosition = framePosition;
        repaint();
    }

    /**
     * Reset the playhead
     */
    public void resetPlayhead() {
        this.currentPlayPosition = -1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (sample == null || sample.getWaveformData() == null) {
            drawEmptyState(g2d);
            return;
        }

        // Fill background
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw selected region background first
        drawSelectedRegion(g2d);

        // Draw loop region background
        drawLoopRegion(g2d);

        // Draw waveform
        drawWaveform(g2d, sample.getWaveformData(), zoomFactor, scrollPosition);

        // Draw sample markers
        drawMarker(g2d, sample.getSampleStart(), Color.RED, "S");
        drawMarker(g2d, sample.getSampleEnd(), Color.RED, "E");

        // Draw loop markers if loop is enabled
        if (sample.isLoopEnabled()) {
            drawMarker(g2d, sample.getLoopStart(), Color.BLUE, "L1");
            drawMarker(g2d, sample.getLoopEnd(), Color.BLUE, "L2");
        }

        // Draw playhead after everything else
        drawPlayhead(g2d);

        g2d.dispose();
    }

    /**
     * Draw empty state when no waveform is available
     */
    private void drawEmptyState(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();

        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.GRAY);
        g2d.drawString("No audio loaded", width / 2 - 40, height / 2);
    }

    /**
     * Draw highlighted region for selection
     */
    private void drawSelectedRegion(Graphics2D g2d) {
        if (sample == null) return;

        int startX = frameToScreen(sample.getSampleStart());
        int endX = frameToScreen(sample.getSampleEnd());

        // Draw selection region
        g2d.setColor(SELECTION_COLOR);
        g2d.fillRect(startX, 0, endX - startX, getHeight());
    }

    /**
     * Draw highlighted region for loop
     */
    private void drawLoopRegion(Graphics2D g2d) {
        if (sample == null || !sample.isLoopEnabled()) return;

        int startX = frameToScreen(sample.getLoopStart());
        int endX = frameToScreen(sample.getLoopEnd());

        // Draw loop region
        g2d.setColor(LOOP_COLOR);
        g2d.fillRect(startX, 0, endX - startX, getHeight());
    }

    /**
     * Draw waveform with current zoom and scroll settings
     */
    private void drawWaveform(Graphics2D g2d, int[] waveformData, double zoom, int scroll) {
        if (waveformData == null || waveformData.length == 0) return;

        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        // Calculate visible range based on zoom
        int zoomedLength = (int) (waveformData.length / zoom);
        int startIdx = Math.min(scroll, waveformData.length - zoomedLength);
        if (startIdx < 0) startIdx = 0;

        // Draw horizontal center line
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawLine(0, centerY, width, centerY);

        // Calculate samples per pixel
        double samplesPerPixel = (double) zoomedLength / width;

        // Draw waveform
        g2d.setColor(new Color(30, 144, 255)); // Blue

        for (int x = 0; x < width; x++) {
            int startSample = startIdx + (int) (x * samplesPerPixel);
            int endSample = startIdx + (int) ((x + 1) * samplesPerPixel);

            // Find max amplitude in this pixel range
            int maxAmplitude = 0;
            for (int i = startSample; i < endSample && i < waveformData.length; i++) {
                maxAmplitude = Math.max(maxAmplitude, waveformData[i]);
            }

            // Scale to panel height (leaving 10% margin)
            int barHeight = (int) (maxAmplitude * height * 0.8 / 100);
            if (barHeight < 1) barHeight = 1;

            // Draw symmetric waveform
            g2d.drawLine(x, centerY - barHeight / 2, x, centerY + barHeight / 2);
        }
    }

    /**
     * Draw marker at a specific frame position
     */
    private void drawMarker(Graphics2D g2d, int framePosition, Color color, String label) {
        if (sample == null) return;

        int x = frameToScreen(framePosition);
        int height = getHeight();

        // Draw marker line
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(x, 0, x, height);

        // Draw marker handle
        int handleSize = 8;
        g2d.fillRect(x - handleSize / 2, 0, handleSize, handleSize);

        // Draw marker label
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.drawString(label, x - 4, handleSize + 12);
    }

    /**
     * Draw playhead at current position
     */
    private void drawPlayhead(Graphics2D g2d) {
        if (currentPlayPosition < 0 || sample == null) return;

        int x = frameToScreen(currentPlayPosition);
        int height = getHeight();

        // Draw playhead line with a distinct appearance
        g2d.setColor(PLAYHEAD_COLOR);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{5, 3}, 0));
        g2d.drawLine(x, 0, x, height);

        // Draw position indicator at top
        int indicatorSize = 8;
        g2d.setStroke(new BasicStroke(2));
        g2d.fillPolygon(
                new int[]{x - indicatorSize / 2, x, x + indicatorSize / 2},
                new int[]{indicatorSize, 0, indicatorSize},
                3
        );
    }

    /**
     * Convert frame position to screen X coordinate
     */
    private int frameToScreen(int frame) {
        if (sample == null || sample.getWaveformData() == null) return 0;

        int width = getWidth();
        int totalFrames = sample.getWaveformData().length;

        // Calculate frame to screen position based on zoom
        double pixelsPerFrame = width * zoomFactor / totalFrames;
        return (int) (frame * pixelsPerFrame) - scrollPosition;
    }

    /**
     * Convert screen X coordinate to frame position
     */
    private int screenToFrame(int x) {
        if (sample == null || sample.getWaveformData() == null) return 0;

        int width = getWidth();
        int totalFrames = sample.getWaveformData().length;

        // Calculate screen to frame position based on zoom
        double framesPerPixel = totalFrames / (width * zoomFactor);
        int frame = (int) ((x + scrollPosition) * framesPerPixel);

        // Ensure frame is within valid range
        frame = Math.max(0, frame);
        frame = Math.min(totalFrames - 1, frame);

        return frame;
    }

    /**
     * Determine which marker (if any) is at the given screen position
     */
    private int getMarkerAtPosition(int x) {
        if (sample == null) return MARKER_NONE;

        // Check each marker to see if it's within tolerance
        int sampleStartX = frameToScreen(sample.getSampleStart());
        if (Math.abs(x - sampleStartX) <= MARKER_TOLERANCE) {
            return MARKER_SAMPLE_START;
        }

        int sampleEndX = frameToScreen(sample.getSampleEnd());
        if (Math.abs(x - sampleEndX) <= MARKER_TOLERANCE) {
            return MARKER_SAMPLE_END;
        }

        // Only check loop markers if looping is enabled
        if (sample.isLoopEnabled()) {
            int loopStartX = frameToScreen(sample.getLoopStart());
            if (Math.abs(x - loopStartX) <= MARKER_TOLERANCE) {
                return MARKER_LOOP_START;
            }

            int loopEndX = frameToScreen(sample.getLoopEnd());
            if (Math.abs(x - loopEndX) <= MARKER_TOLERANCE) {
                return MARKER_LOOP_END;
            }
        }

        return MARKER_NONE;
    }

    // Listener interface
    public interface WaveformChangeListener {
        void onMarkersChanged(Sample sample);
    }
}
