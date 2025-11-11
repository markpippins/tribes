package com.angrysurfer.beats.panel.sample;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Panel for browsing and editing audio samples
 */
public class SampleBrowserPanel extends JPanel implements IBusListener {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SampleBrowserPanel.class);

    private final FileBrowserPanel fileBrowserPanel;
    private final SampleViewerPanel sampleViewerPanel;
    private File currentSampleFile;

    public SampleBrowserPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create file browser and sample viewer panels
        fileBrowserPanel = new FileBrowserPanel(this::onFileSelected);
        sampleViewerPanel = new SampleViewerPanel();

        // Make sure to disable the create player button initially
        sampleViewerPanel.getControlsPanel().setCreatePlayerEnabled(false);

        // Create split pane with file browser on left and sample viewer on right
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                fileBrowserPanel,
                sampleViewerPanel);

        // Configure split pane
        splitPane.setDividerLocation(300);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);

        // Add split pane to the main panel
        add(splitPane, BorderLayout.CENTER);

        // Register for command bus events
        CommandBus.getInstance().register(this, new String[]{Commands.THEME_CHANGED});

        // Set initial directory to user's home directory
        fileBrowserPanel.navigateToDirectory(getUserSamplesDirectory());
    }

    /**
     * Get the user's samples directory or fallback to home directory
     */
    private Path getUserSamplesDirectory() {
        // Try common sample locations first
        String[] possiblePaths = {
                System.getProperty("user.home") + "/Samples",
                System.getProperty("user.home") + "/Music/Samples",
                System.getProperty("user.home") + "/Documents/Samples"
        };

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                return dir.toPath();
            }
        }

        // Fall back to user home directory
        return Paths.get(System.getProperty("user.home"));
    }

    /**
     * Handle sample file selection
     */
    private void onFileSelected(File file) {
        if (file == null || !file.isFile()) {
            return;
        }

        String filename = file.getName().toLowerCase();
        if (filename.endsWith(".wav")) {
            currentSampleFile = file;
            logger.info("Loading sample: {}", file.getAbsolutePath());

            // Load and display the audio file
            try {
                sampleViewerPanel.loadAudioFile(file);
            } catch (Exception e) {
                logger.error("Error loading audio file: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;

        switch (action.getCommand()) {
            case Commands.THEME_CHANGED -> {
                SwingUtilities.invokeLater(() -> {
                    fileBrowserPanel.refreshUI();
                    sampleViewerPanel.refreshUI();
                });
            }
        }
    }
}
