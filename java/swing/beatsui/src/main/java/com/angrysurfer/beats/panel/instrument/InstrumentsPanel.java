package com.angrysurfer.beats.panel.instrument;

import com.angrysurfer.beats.Dialog;
import com.angrysurfer.beats.panel.ContextMenuHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.ControlCode;
import com.angrysurfer.core.model.ControlCodeCaption;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.*;

@Getter
@Setter
public class InstrumentsPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentsPanel.class.getName());
    private JTable instrumentsTable;
    private JTable controlCodesTable;
    private JTable captionsTable;
    private InstrumentWrapper selectedInstrument;
    private ControlCode selectedControlCode;
    private JButton addCaptionButton;
    private JButton editCaptionButton;
    private JButton deleteCaptionButton;
    private JButton addInstrumentButton;
    private JButton editInstrumentButton;
    private JButton deleteInstrumentButton;
    private JButton enableInstrumentButton;
    private ContextMenuHelper instrumentsContextMenu;
    private ContextMenuHelper controlCodesContextMenu;
    private ContextMenuHelper captionsContextMenu;

    public InstrumentsPanel() {
        super(new BorderLayout());
        setup();
        registerCommandListener();
    }

    private void setup() {
        setLayout(new BorderLayout());

        // Create tables first
        instrumentsTable = createInstrumentsTable();
        setupInstrumentsTableSelectionListener();

        controlCodesTable = createControlCodesTable();
        setupControlCodesTableSelectionListener();

        captionsTable = createCaptionsTable();
        setupCaptionsTableSelectionListener();

        // Setup context menus after tables exist
        setupContextMenus();

        // Setup key bindings
        setupKeyBindings();

        // Add main content to the CENTER
        add(createOptionsPanel(), BorderLayout.CENTER);

        // Add MIDI test panel to the SOUTH position (bottom) of the main panel
        add(createMidiTestControls(), BorderLayout.SOUTH);
    }

    private void registerCommandListener() {
        CommandBus.getInstance().register(command -> {
            if (Commands.LOAD_CONFIG.equals(command.getCommand())) {
                // SwingUtilities.invokeLater(() -> showConfigFileChooserDialog());
            }
        }, new String[] { Commands.LOAD_CONFIG });
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create horizontal split pane for instruments and control codes
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Right side - Control Codes and Captions
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Create panels using already created tables
        JPanel controlCodesPanel = new JPanel(new BorderLayout());
        JPanel controlCodesToolbar = setupControlCodeToolbar();
        controlCodesPanel.add(controlCodesToolbar, BorderLayout.NORTH);
        controlCodesPanel.add(new JScrollPane(controlCodesTable), BorderLayout.CENTER);
        controlCodesPanel.setPreferredSize(new Dimension(250, controlCodesPanel.getPreferredSize().height));
        controlCodesPanel.setMinimumSize(new Dimension(250, 0));

        // Create captions panel with toolbar and fixed width
        JPanel captionsPanel = new JPanel(new BorderLayout());
        JPanel captionsToolbar = setupCaptionsToolbar();
        captionsPanel.add(captionsToolbar, BorderLayout.NORTH);
        captionsPanel.add(new JScrollPane(captionsTable), BorderLayout.CENTER);
        captionsPanel.setPreferredSize(new Dimension(250, captionsPanel.getPreferredSize().height));
        captionsPanel.setMinimumSize(new Dimension(250, 0));

        rightSplitPane.setLeftComponent(controlCodesPanel);
        rightSplitPane.setRightComponent(captionsPanel);
        rightSplitPane.setResizeWeight(0.5);

        // Left side - Instruments panel gets all extra space
        JPanel instrumentsPanel = new JPanel(new BorderLayout());
        JPanel instrumentsToolbar = setupInstrumentToolbar();
        instrumentsPanel.add(instrumentsToolbar, BorderLayout.NORTH);
        instrumentsPanel.add(new JScrollPane(instrumentsTable), BorderLayout.CENTER);

        mainSplitPane.setLeftComponent(instrumentsPanel);
        mainSplitPane.setRightComponent(rightSplitPane);
        mainSplitPane.setResizeWeight(0.6); // Give more weight to instruments panel

        panel.add(mainSplitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel setupInstrumentToolbar() {
        JPanel toolBar = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        addInstrumentButton = new JButton("Add");
        editInstrumentButton = new JButton("Edit");
        deleteInstrumentButton = new JButton("Delete");
        enableInstrumentButton = new JButton("Enable");

        addInstrumentButton.addActionListener(e -> showInstrumentDialog(null));
        editInstrumentButton.addActionListener(e -> editSelectedInstrument());
        deleteInstrumentButton.addActionListener(e -> deleteSelectedInstrument());
        enableInstrumentButton.addActionListener(e -> enableSelectedInstrument());

        // Initially disabled until an instrument is selected
        editInstrumentButton.setEnabled(false);
        deleteInstrumentButton.setEnabled(false);
        enableInstrumentButton.setEnabled(false);

        buttonPanel.add(addInstrumentButton);
        buttonPanel.add(editInstrumentButton);
        buttonPanel.add(deleteInstrumentButton);
        buttonPanel.add(enableInstrumentButton);

        toolBar.add(buttonPanel, BorderLayout.CENTER);

        return toolBar;
    }

    /**
     * Creates MIDI testing controls for the toolbar
     */
    private JPanel createMidiTestControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBorder(BorderFactory.createTitledBorder("MIDI Test"));

        // --- Note testing section ---

        // Channel selector for notes (use channels defined in the instrument)
        JPanel channelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        channelPanel.add(new JLabel("Ch:"));
        JSpinner channelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 16, 1));

        // Update channel spinner based on selected instrument
        if (selectedInstrument != null && selectedInstrument.getChannel() != null) {
            // Convert from 0-based to 1-based for display
            channelSpinner.setValue(selectedInstrument.getChannel() + 1);
        }

        channelSpinner.setPreferredSize(new Dimension(50, 25));
        channelSpinner.setToolTipText("MIDI Channel (1-16)");
        channelPanel.add(channelSpinner);
        panel.add(channelPanel);

        // Note selector
        JPanel notePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        notePanel.add(new JLabel("Note:"));
        JSpinner noteSpinner = new JSpinner(new SpinnerNumberModel(60, 0, 127, 1));
        noteSpinner.setPreferredSize(new Dimension(55, 25));
        noteSpinner.setToolTipText("MIDI Note (0-127)");
        notePanel.add(noteSpinner);
        panel.add(notePanel);

        // Velocity selector
        JPanel velocityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        velocityPanel.add(new JLabel("Vel:"));
        JSpinner velocitySpinner = new JSpinner(new SpinnerNumberModel(100, 1, 127, 1));
        velocitySpinner.setPreferredSize(new Dimension(50, 25));
        velocitySpinner.setToolTipText("Note Velocity (1-127)");
        velocityPanel.add(velocitySpinner);
        panel.add(velocityPanel);

        // Send note button
        JButton sendNoteButton = new JButton("Send Note");
        sendNoteButton.setMargin(new Insets(2, 8, 2, 8));
        sendNoteButton.addActionListener(e -> {
            sendMidiNote(
                    (Integer) channelSpinner.getValue() - 1, // Convert 1-16 to 0-15
                    (Integer) noteSpinner.getValue(),
                    (Integer) velocitySpinner.getValue());
        });
        panel.add(sendNoteButton);

        // Add separator
        panel.add(Box.createHorizontalStrut(15));

        // --- Control Change section ---

        // CC number selector
        JPanel ccPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        ccPanel.add(new JLabel("CC:"));
        JSpinner ccSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 127, 1));
        ccSpinner.setPreferredSize(new Dimension(50, 25));
        ccSpinner.setToolTipText("Control Change Number (0-127)");
        ccPanel.add(ccSpinner);
        panel.add(ccPanel);

        // CC value selector
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        valuePanel.add(new JLabel("Val:"));
        JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(64, 0, 127, 1));
        valueSpinner.setPreferredSize(new Dimension(50, 25));
        valueSpinner.setToolTipText("Control Change Value (0-127)");
        valuePanel.add(valueSpinner);
        panel.add(valuePanel);

        // Send CC button
        JButton sendCCButton = new JButton("Send CC");
        sendCCButton.setMargin(new Insets(2, 8, 2, 8));
        sendCCButton.addActionListener(e -> {
            sendMidiControlChange(
                    (Integer) channelSpinner.getValue() - 1, // Convert 1-16 to 0-15
                    (Integer) ccSpinner.getValue(),
                    (Integer) valueSpinner.getValue());
        });
        panel.add(sendCCButton);

        // Add separator
        panel.add(Box.createHorizontalStrut(15));

        // --- Program Change section ---

        // Program number selector
        JPanel programPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        programPanel.add(new JLabel("Program:"));
        JSpinner programSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 127, 1));
        programSpinner.setPreferredSize(new Dimension(50, 25));
        programSpinner.setToolTipText("Program Number (0-127)");
        programPanel.add(programSpinner);
        panel.add(programPanel);

        // Send Program Change button
        JButton sendPCButton = new JButton("Send PC");
        sendPCButton.setMargin(new Insets(2, 8, 2, 8));
        sendPCButton.addActionListener(e -> {
            sendMidiProgramChange(
                    (Integer) channelSpinner.getValue() - 1, // Convert 1-16 to 0-15
                    (Integer) programSpinner.getValue());
        });
        panel.add(sendPCButton);

        return panel;
    }

    private void sendMidiNote(int channel, int noteNumber, int velocity) {
        if (selectedInstrument == null) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Error", "No instrument selected"));
            return;
        }

        try {
            // Get selected output device from device selection
            MidiDevice device = getSelectedInstrument().getDevice();
            if (device == null) {
                device = MidiService.getInstance().getDevice(getSelectedInstrument().getDeviceName());
            }
            if (device == null) {
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("MIDI Test", "Error", "No MIDI output device selected"));
                return;
            }

            // Open device if not already open
            if (!device.isOpen()) {
                device.open();
            }

            // Get receiver
            Receiver receiver = device.getReceiver();

            // Create note on message
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(ShortMessage.NOTE_ON, channel, noteNumber, velocity);
            receiver.send(noteOn, -1);

            // Create note off message (to be sent after a delay)
            ShortMessage noteOff = new ShortMessage();
            noteOff.setMessage(ShortMessage.NOTE_OFF, channel, noteNumber, 0);

            // Schedule note off message after 500ms
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    receiver.send(noteOff, -1);
                }
            }, 500);

            // Log and update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Info",
                            String.format("Sent note: %d on channel: %d with velocity: %d",
                                    noteNumber, channel + 1, velocity)));

        } catch (Exception e) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Error", "Failed to send MIDI note: " + e.getMessage()));
            logger.error("Error sending MIDI note", e);
        }
    }

    /**
     * Sends a MIDI control change message to the selected instrument
     */
    private void sendMidiControlChange(int channel, int controlNumber, int value) {
        if (selectedInstrument == null) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Error", "No instrument selected"));
            return;
        }

        try {
            // Check if the instrument is available
            if (!selectedInstrument.getAvailable()) {
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("InstrumentsPanel", "Error",
                                "Instrument is not available: " + selectedInstrument.getName()));
                return;
            }

            // Send control change message
            selectedInstrument.controlChange(controlNumber, value);

            // Log and update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Info",
                            String.format("Sent CC: %d on channel: %d with value: %d",
                                    controlNumber, channel + 1, value)));

        } catch (Exception e) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Error", "Failed to send MIDI CC: " + e.getMessage()));
            logger.error("Error sending MIDI control change", e);
        }
    }

    /**
     * Sends a MIDI program change message to the selected instrument
     */
    private void sendMidiProgramChange(int channel, int programNumber) {
        if (selectedInstrument == null) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Error", "No instrument selected"));
            return;
        }

        try {
            // Check if the instrument is available
            if (!selectedInstrument.getAvailable()) {
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("InstrumentsPanel", "Error",
                                "Instrument is not available: " + selectedInstrument.getName()));
                return;
            }

            // Send program change message
            selectedInstrument.programChange(programNumber, 0);

            // Log and update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Info",
                            String.format("Sent Program Change: %d on channel: %d",
                                    programNumber, channel + 1)));

        } catch (Exception e) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Error",
                            "Failed to send MIDI Program Change: " + e.getMessage()));
            logger.error("Error sending MIDI program change", e);
        }
    }

    private JPanel setupControlCodeToolbar() {
        JPanel toolBar = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        addButton.addActionListener(e -> showControlCodeDialog(null));
        editButton.addActionListener(e -> editSelectedControlCode());
        deleteButton.addActionListener(e -> deleteSelectedControlCode());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        toolBar.add(buttonPanel, BorderLayout.CENTER);
        return toolBar;
    }

    private JPanel setupCaptionsToolbar() {
        JPanel toolBar = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        addCaptionButton = new JButton("Add");
        editCaptionButton = new JButton("Edit");
        deleteCaptionButton = new JButton("Delete");

        addCaptionButton.addActionListener(e -> showCaptionDialog(null));
        editCaptionButton.addActionListener(e -> editSelectedCaption());
        deleteCaptionButton.addActionListener(e -> deleteSelectedCaption());

        buttonPanel.add(addCaptionButton);
        buttonPanel.add(editCaptionButton);
        buttonPanel.add(deleteCaptionButton);

        toolBar.add(buttonPanel, BorderLayout.CENTER);
        return toolBar;
    }

    private JTable createInstrumentsTable() {
        // Update the columns array to include Owner at the end
        String[] columns = {"ID", "Name", "Device Name", "Channel", "Available", "Low", "High", "Initialized",
                "Owner"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells read-only for now
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: // ID - numeric
                        return Long.class;
                    case 3: // Channel - numeric
                        return Integer.class;
                    case 4: // Available
                    case 7: // Initialized
                        return Boolean.class;
                    case 5: // Lowest Note
                    case 6: // Highest Note
                        return Integer.class;
                    default:
                        return String.class;
                }
            }
        };

        // Load data from Redis
        List<InstrumentWrapper> instruments = RedisService.getInstance().findAllInstruments();
        for (InstrumentWrapper instrument : instruments) {
            model.addRow(new Object[]{
                    instrument.getId(),
                    instrument.getName(),
                    instrument.getDeviceName(),
                    instrument.getChannel() != null ? instrument.getChannel() + 1 : null, // Convert to 1-based
                    instrument.getAvailable(),
                    instrument.getLowestNote(),
                    instrument.getHighestNote(),
                    instrument.isInitialized(),
                    "" // Add owner column
            });
        }

        JTable table = new JTable(model);

        // Enable auto-sorting
        table.setAutoCreateRowSorter(true);

        // Add default sorting by name (column 1)
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        // Add double-click handler for instrument editing
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedInstrument();
                }
            }
        });

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Create a left-aligned renderer for the ID column
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        // Apply left alignment to ID column (column 0)
        table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);

        // Apply center alignment to other numeric columns
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Channel
        for (int i = 5; i <= 6; i++) { // Center-align note columns
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setMaxWidth(70); // ID
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setMaxWidth(60); // Channel
        table.getColumnModel().getColumn(3).setPreferredWidth(60);

        return table;
    }

    private JTable createControlCodesTable() {
        String[] columns = {"Name", "Code", "Min", "Max"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column > 0 ? Integer.class : String.class;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true); // Enable sorting

        // Add default sorting by name (column 0)
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        // Center-align all numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Code, Lower Bound, Upper Bound
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Set fixed column widths for control codes table
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        table.getColumnModel().getColumn(0).setMinWidth(100);

        // Fixed widths for numeric columns
        table.getColumnModel().getColumn(1).setMaxWidth(50); // Code
        table.getColumnModel().getColumn(1).setMinWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(45); // Lower Bound
        table.getColumnModel().getColumn(2).setMinWidth(45);
        table.getColumnModel().getColumn(3).setMaxWidth(45); // Upper Bound
        table.getColumnModel().getColumn(3).setMinWidth(45);

        // Modify selection listener to handle caption buttons
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                boolean hasSelection = row >= 0;
                if (hasSelection && selectedInstrument != null) {
                    String name = (String) table.getValueAt(row, 0);
                    selectedControlCode = findControlCodeByName(name);
                    updateCaptionsTable();
                    addCaptionButton.setEnabled(true);
                } else {
                    selectedControlCode = null;
                    updateCaptionsTable();
                    addCaptionButton.setEnabled(false);
                    editCaptionButton.setEnabled(false);
                    deleteCaptionButton.setEnabled(false);
                }
            }
        });

        // Add double-click handler
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedControlCode();
                }
            }
        });

        return table;
    }

    private JTable createCaptionsTable() {
        String[] columns = {"Code", "Description"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Long.class : String.class;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true); // Enable sorting

        // Add default sorting by description (column 1)
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        // Center-align the numeric code column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        // Set fixed column widths for captions table
        table.getColumnModel().getColumn(0).setMaxWidth(50); // Code
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(190); // Description
        table.getColumnModel().getColumn(1).setMinWidth(190);

        // Modify selection listener to only enable edit/delete when caption is selected
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0 && selectedControlCode != null;
                editCaptionButton.setEnabled(hasSelection);
                deleteCaptionButton.setEnabled(hasSelection);
            }
        });

        // Add double-click handler
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedCaption();
                }
            }
        });

        return table;
    }

    private void updateControlCodesTable() {
        DefaultTableModel model = (DefaultTableModel) controlCodesTable.getModel();
        model.setRowCount(0);

        if (selectedInstrument != null && selectedInstrument.getControlCodes() != null) {
            logger.info("Updating control codes table for instrument: " + selectedInstrument.getName() + " with "
                    + selectedInstrument.getControlCodes().size() + " codes");

            for (ControlCode cc : selectedInstrument.getControlCodes()) {
                model.addRow(new Object[]{cc.getName(), cc.getCode(), cc.getLowerBound(), cc.getUpperBound()});
                logger.info("Added control code to table: " + cc.getName());
            }
        } else {
            logger.error("No control codes to display - instrument: "
                    + (selectedInstrument == null ? "null" : selectedInstrument.getName()));
        }
    }

    private void updateCaptionsTable() {
        DefaultTableModel model = (DefaultTableModel) captionsTable.getModel();
        model.setRowCount(0);

        if (selectedControlCode != null && selectedControlCode.getCaptions() != null) {
            logger.info("Updating captions table for control code: " + selectedControlCode.getName() + " with "
                    + selectedControlCode.getCaptions().size() + " captions");

            for (ControlCodeCaption caption : selectedControlCode.getCaptions()) {
                model.addRow(new Object[]{caption.getCode(), caption.getDescription()});
                logger.info("Added caption to table: " + caption.getDescription());
            }
        } else {
            logger.error("No captions to display - control code: "
                    + (selectedControlCode == null ? "null" : selectedControlCode.getName()));
        }
    }

    private InstrumentWrapper findInstrumentByName(String name) {
        if (name == null)
            return null;

        // Get instruments from Redis
        for (InstrumentWrapper instrument : RedisService.getInstance().getInstrumentHelper().findAllInstruments()) {
            if (instrument != null && name.equals(instrument.getName())) {
                return instrument;
            }
        }

        // Fallback to Redis search
        return RedisService.getInstance().findAllInstruments().stream()
                .filter(i -> i != null && name.equals(i.getName()))
                .findFirst()
                .orElse(null);
    }

    private ControlCode findControlCodeByName(String name) {
        if (selectedInstrument != null && selectedInstrument.getControlCodes() != null) {
            return selectedInstrument.getControlCodes().stream().filter(cc -> cc.getName().equals(name)).findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void showControlCodeDialog(ControlCode controlCode) {
        boolean isNew = (controlCode == null);
        if (isNew) {
            controlCode = new ControlCode();
        }

        ControlCodeEditPanel editorPanel = new ControlCodeEditPanel(controlCode);
        Dialog<ControlCode> dialog = new Dialog<>(controlCode, editorPanel);
        dialog.setTitle(isNew ? "Add Control Code" : "Edit Control Code");

        if (dialog.showDialog()) {
            ControlCode updatedControlCode = editorPanel.getUpdatedControlCode();
            if (isNew) {
                selectedInstrument.getControlCodes().add(updatedControlCode);
            }
            RedisService.getInstance().saveInstrument(selectedInstrument);
            updateControlCodesTable();
        }
    }

    private void editSelectedControlCode() {
        int row = controlCodesTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) controlCodesTable.getValueAt(row, 0);
            ControlCode controlCode = findControlCodeByName(name);
            if (controlCode != null) {
                showControlCodeDialog(controlCode);
            }
        }
    }

    private void deleteSelectedControlCode() {
        int row = controlCodesTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) controlCodesTable.getValueAt(row, 0);
            ControlCode controlCode = findControlCodeByName(name);
            if (controlCode != null) {
                selectedInstrument.getControlCodes().remove(controlCode);
                RedisService.getInstance().saveInstrument(selectedInstrument);
                updateControlCodesTable();
            }
        }
    }

    private void showCaptionDialog(ControlCodeCaption caption) {
        if (selectedControlCode == null) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("No control code selected"));
            return;
        }

        boolean isNew = (caption == null);
        if (isNew) {
            caption = new ControlCodeCaption();
            caption.setCode((long) selectedControlCode.getCode());
        }

        CaptionEditPanel editorPanel = new CaptionEditPanel(caption);
        Dialog<ControlCodeCaption> dialog = new Dialog<>(caption, editorPanel);
        dialog.setTitle(isNew ? "Add Caption" : "Edit Caption");

        if (dialog.showDialog()) {
            ControlCodeCaption updatedCaption = editorPanel.getUpdatedCaption();
            if (isNew) {
                if (selectedControlCode.getCaptions() == null) {
                    selectedControlCode.setCaptions(new HashSet<>());
                }
                selectedControlCode.getCaptions().add(updatedCaption);
            }
            RedisService.getInstance().saveInstrument(selectedInstrument);
            updateCaptionsTable();
        }
    }

    private void editSelectedCaption() {
        int selectedRow = captionsTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Get caption from selected row, not just first caption
            ControlCodeCaption caption = getCaptionFromRow(selectedRow);
            if (caption != null) {
                logger.info("Editing caption: " + caption.getDescription() + " for control code: "
                        + selectedControlCode.getName());
                showCaptionDialog(caption);
            }
        }
    }

    private void deleteSelectedCaption() {
        int row = captionsTable.getSelectedRow();
        if (row >= 0) {
            ControlCodeCaption caption = getCaptionFromRow(row);
            selectedControlCode.getCaptions().remove(caption);
            RedisService.getInstance().saveInstrument(selectedInstrument);
            updateCaptionsTable();
        }
    }

    private ControlCodeCaption getCaptionFromRow(int row) {
        if (selectedControlCode != null && selectedControlCode.getCaptions() != null) {
            // Convert view index to model index if table is sorted
            int modelRow = captionsTable.convertRowIndexToModel(row);

            // Get values from table model
            Long code = (Long) captionsTable.getModel().getValueAt(modelRow, 0);
            String description = (String) captionsTable.getModel().getValueAt(modelRow, 1);

            // Find matching caption in control code's captions
            return selectedControlCode.getCaptions().stream()
                    .filter(c -> c.getCode().equals(code) && c.getDescription().equals(description)).findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void showInstrumentDialog(InstrumentWrapper instrument) {
        try {
            boolean isNew = (instrument == null);
            logger.info("Showing instrument dialog: {}", isNew ? "new instrument" : instrument.getName());

            // Create a deep copy to avoid modifying original until save is confirmed
            InstrumentWrapper instrumentCopy;
            if (isNew) {
                instrumentCopy = new InstrumentWrapper();
                // Initialize required fields for new instruments
                instrumentCopy.setInternal(Boolean.FALSE);
                instrumentCopy.setAvailable(Boolean.FALSE);
            } else {
                // Create a deep copy to preserve original until save is confirmed
                instrumentCopy = new InstrumentWrapper();
                instrumentCopy.setId(instrument.getId());
                instrumentCopy.setName(instrument.getName());
                instrumentCopy.setDeviceName(instrument.getDeviceName());
                instrumentCopy.setChannel(instrument.getChannel());
                instrumentCopy.setLowestNote(instrument.getLowestNote());
                instrumentCopy.setHighestNote(instrument.getHighestNote());
                instrumentCopy.setInternal(instrument.getInternal() != null ? instrument.getInternal() : Boolean.FALSE);
                instrumentCopy.setAvailable(instrument.getAvailable() != null ? instrument.getAvailable() : Boolean.FALSE);
                instrumentCopy.setControlCodes(instrument.getControlCodes()); // Shallow copy of control codes is OK
                instrumentCopy.setDescription(instrument.getDescription());
                instrumentCopy.setReceivedChannels(instrument.getReceivedChannels());
                // Copy any other fields you need to preserve
            }

            // Create and configure dialog
            InstrumentEditPanel editorPanel = new InstrumentEditPanel(instrumentCopy);
            Dialog<InstrumentWrapper> dialog = new Dialog<>(instrumentCopy, editorPanel);
            dialog.setTitle(isNew ? "Add Instrument" : "Edit Instrument: " + instrumentCopy.getName());
            dialog.setLocationRelativeTo(this);

            logger.info("Showing dialog for instrument...");
            boolean result = dialog.showDialog();
            logger.info("Dialog result: {}", result);

            if (result) {
                // Get the updated instrument from the editor panel
                InstrumentWrapper updatedInstrument = editorPanel.getUpdatedInstrument();
                logger.info("Saving updated instrument: {} (ID: {})", updatedInstrument.getName(), updatedInstrument.getId());

                // First save to Redis
                RedisService.getInstance().saveInstrument(updatedInstrument);

                // Then explicitly update in UserConfigManager to ensure persistence
                updateInstrumentInUserConfig(updatedInstrument);

                // Save the instrument
                PlaybackService.getInstance().saveInstrument(updatedInstrument);

                // Always publish the update event
                CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, updatedInstrument);

                // Refresh the UI
                refreshInstrumentsTable();

                // If this was the selected instrument, update selected instrument reference
                if (selectedInstrument != null &&
                        updatedInstrument.getId() != null &&
                        updatedInstrument.getId().equals(selectedInstrument.getId())) {
                    selectedInstrument = updatedInstrument;
                    updateControlCodesTable();
                    updateCaptionsTable();
                }

                // Show status message
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("InstrumentsPanel", "Success",
                                "Saved instrument: " + updatedInstrument.getName()));
            } else {
                logger.info("Dialog cancelled, no changes made");
            }
        } catch (Exception e) {
            logger.error("Error showing/processing instrument dialog: {}", e.getMessage(), e);
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Error",
                            "Error editing instrument: " + e.getMessage()));
        }
    }


    /**
     * Delete selected instruments with validation checks
     */
    private void deleteSelectedInstrument() {
        int[] selectedRows = instrumentsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }

        // Collect all instruments to delete
        List<InstrumentWrapper> instrumentsToDelete = new ArrayList<>();
        List<InstrumentWrapper> inUseInstruments = new ArrayList<>();

        // Check each selected instrument
        for (int viewRow : selectedRows) {
            int modelRow = instrumentsTable.convertRowIndexToModel(viewRow);

            // Get ID from column 0 and name from column 1
            Long id = (Long) instrumentsTable.getModel().getValueAt(modelRow, 0);
            String name = (String) instrumentsTable.getModel().getValueAt(modelRow, 1);

            // Get owner info from the last column (our newly added Owner column)
            String ownerInfo = (String) instrumentsTable.getModel().getValueAt(modelRow,
                    instrumentsTable.getModel().getColumnCount() - 1);

            InstrumentWrapper instrument = PlaybackService.getInstance().getInstrument(id);
            if (instrument != null) {
                // Check if the instrument has no owner or owner is "None"
                if (ownerInfo == null || ownerInfo.isEmpty() || "None".equals(ownerInfo)) {
                    instrumentsToDelete.add(instrument);
                    logger.info("Instrument {} will be deleted (no owner)", instrument.getName());
                } else {
                    inUseInstruments.add(instrument);
                    logger.info("Instrument {} cannot be deleted (owned by: {})",
                            instrument.getName(), ownerInfo);
                }
            }
        }

        // Handle warnings and confirmations
        if (!inUseInstruments.isEmpty()) {
            StringBuilder warningMsg = new StringBuilder();
            warningMsg.append("The following instruments are in use and cannot be deleted:\n\n");

            for (InstrumentWrapper instrument : inUseInstruments) {
                warningMsg.append("• ").append(instrument.getName()).append("\n");
            }

            if (!instrumentsToDelete.isEmpty()) {
                warningMsg.append("\nProceed with deleting the other ").append(instrumentsToDelete.size())
                        .append(" instrument(s)?");

                int choice = JOptionPane.showConfirmDialog(this, warningMsg.toString(),
                        "Instruments In Use", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(this, warningMsg.toString(),
                        "Cannot Delete In-Use Instruments", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else if (!instrumentsToDelete.isEmpty()) {
            // Confirm deletion when all instruments can be deleted
            String message = instrumentsToDelete.size() == 1
                    ? "Delete the selected instrument?"
                    : "Delete " + instrumentsToDelete.size() + " instruments?";

            int choice = JOptionPane.showConfirmDialog(this, message, "Delete Instrument",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Process deletions
        int deletedCount = 0;
        StringBuilder statusMsg = new StringBuilder();

        for (InstrumentWrapper instrument : instrumentsToDelete) {
            try {
                // Delete from Redis
                RedisService.getInstance().deleteInstrument(instrument);

                // Also remove from InstrumentManager's cache
                PlaybackService.getInstance().removeInstrument(instrument.getId());

                deletedCount++;

                // Add to status message (limit to first 3 for brevity)
                if (deletedCount <= 3) {
                    if (statusMsg.length() > 0) {
                        statusMsg.append(", ");
                    }
                    statusMsg.append(instrument.getName());
                }

                logger.info("Deleted instrument: {}", instrument.getName());
            } catch (Exception e) {
                logger.error("Error deleting instrument {}: {}", instrument.getName(), e.getMessage(), e);
            }
        }

        // Publish status updates
        if (deletedCount > 0) {
            // Add ellipsis if more than 3 instruments were deleted
            if (deletedCount > 3) {
                statusMsg.append(", and ").append(deletedCount - 3).append(" more");
            }

            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("InstrumentsPanel", "Success",
                            "Deleted: " + statusMsg));

            // Refresh the table after deletions
            refreshInstrumentsTable();

            // Let any listeners know instruments have been deleted
            CommandBus.getInstance().publish(Commands.INSTRUMENTS_REFRESHED, this, null);

            // Clear selection state
            selectedInstrument = null;
            updateControlCodesTable();
            updateCaptionsTable();

            // Update button states
            editInstrumentButton.setEnabled(false);
            deleteInstrumentButton.setEnabled(false);
        }
    }

    /**
     * Check if an instrument is in use by any player in the session or sequencers
     */
    private boolean isInstrumentInUse(InstrumentWrapper instrument) {
        if (instrument == null || instrument.getId() == null) {
            return false;
        }

        try {
            // Check session players
            Set<Player> sessionPlayers = SessionManager.getInstance().getActiveSession().getPlayers();
            if (sessionPlayers != null) {
                for (Player player : sessionPlayers) {
                    if (player != null &&
                            player.getInstrumentId() != null &&
                            player.getInstrumentId().equals(instrument.getId())) {
                        return true;
                    }
                }
            }

            // Check melodic sequencers
            for (MelodicSequencer sequencer : SequencerService.getInstance().getAllMelodicSequencers()) {
                if (sequencer != null && sequencer.getPlayer() != null &&
                        sequencer.getPlayer().getInstrumentId() != null &&
                        sequencer.getPlayer().getInstrumentId().equals(instrument.getId())) {
                    return true;
                }
            }

            // Check drum sequencers (which have multiple players)
            for (DrumSequencer sequencer : SequencerService.getInstance().getAllSequencers()) {
                if (sequencer != null && sequencer.getPlayers() != null) {
                    for (Player player : sequencer.getPlayers()) {
                        if (player != null &&
                                player.getInstrumentId() != null &&
                                player.getInstrumentId().equals(instrument.getId())) {
                            return true;
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            logger.error("Error checking if instrument is in use: {}", e.getMessage(), e);
            return true; // Assume in use if there's an error (safety first)
        }
    }

    /**
     * Fixed version of refreshInstrumentsTable that properly updates the model
     */
    private void refreshInstrumentsTable() {
        DefaultTableModel model = (DefaultTableModel) instrumentsTable.getModel();
        model.setRowCount(0);

        // Get fresh data DIRECTLY from Redis instead of using the potentially stale cache
        List<InstrumentWrapper> instruments = RedisService.getInstance().findAllInstruments();
        logger.info("Refreshing instruments table with " + instruments.size() + " instruments from Redis");

        // Also refresh the InstrumentManager's cache to keep it in sync
        PlaybackService.getInstance().refreshCache(instruments);

        // Add each instrument to the table
        for (InstrumentWrapper instrument : instruments) {
            if (instrument != null) {
                model.addRow(new Object[]{
                        instrument.getId(),
                        instrument.getName(),
                        instrument.getDeviceName(),
                        instrument.getChannel() != null ? instrument.getChannel() + 1 : null, // Convert to 1-based
                        instrument.getAvailable() != null ? instrument.getAvailable() : false,
                        instrument.getLowestNote(),
                        instrument.getHighestNote(),
                        instrument.isInitialized(),
                        "" // Add owner column
                });
            }
        }

        // Force a repaint to ensure UI is updated
        instrumentsTable.repaint();
    }

    private void setupContextMenus() {
        // Create context menus
        instrumentsContextMenu = new ContextMenuHelper("ADD_INSTRUMENT", "EDIT_INSTRUMENT", "DELETE_INSTRUMENT");

        controlCodesContextMenu = new ContextMenuHelper("ADD_CONTROL_CODE", "EDIT_CONTROL_CODE", "DELETE_CONTROL_CODE");

        captionsContextMenu = new ContextMenuHelper("ADD_CAPTION", "EDIT_CAPTION", "DELETE_CAPTION");

        // Now safe to install on existing tables
        instrumentsContextMenu.install(instrumentsTable);
        controlCodesContextMenu.install(controlCodesTable);
        captionsContextMenu.install(captionsTable);

        // Setup action listeners
        setupContextMenuListeners();
    }

    private void setupContextMenuListeners() {
        instrumentsContextMenu.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case "ADD_INSTRUMENT" -> showInstrumentDialog(null);
                case "EDIT_INSTRUMENT" -> editSelectedInstrument();
                case "DELETE_INSTRUMENT" -> deleteSelectedInstrument();
            }
        });

        controlCodesContextMenu.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case "ADD_CONTROL_CODE" -> showControlCodeDialog(null);
                case "EDIT_CONTROL_CODE" -> editSelectedControlCode();
                case "DELETE_CONTROL_CODE" -> deleteSelectedControlCode();
            }
        });

        captionsContextMenu.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case "ADD_CAPTION" -> showCaptionDialog(null);
                case "EDIT_CAPTION" -> editSelectedCaption();
                case "DELETE_CAPTION" -> deleteSelectedCaption();
            }
        });
    }

    private void setupInstrumentsTableSelectionListener() {
        instrumentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = instrumentsTable.getSelectedRow() != -1;

                // Update button states
                editInstrumentButton.setEnabled(hasSelection);
                deleteInstrumentButton.setEnabled(hasSelection);
                enableInstrumentButton.setEnabled(hasSelection);

                // Update context menu states
                instrumentsContextMenu.setEditEnabled(hasSelection);
                instrumentsContextMenu.setDeleteEnabled(hasSelection);

                if (hasSelection) {
                    int viewRow = instrumentsTable.getSelectedRow();
                    int modelRow = instrumentsTable.convertRowIndexToModel(viewRow);

                    // Fix: Get ID as Long, not String - ID is in first column (index 0)
                    Long instrumentId = (Long) instrumentsTable.getModel().getValueAt(modelRow, 0);
                    String instrumentName = (String) instrumentsTable.getModel().getValueAt(modelRow, 1);

                    // Find the selected instrument
                    selectedInstrument = PlaybackService.getInstance().getInstrument(instrumentId);

                    if (selectedInstrument != null) {
                        logger.info("Selected instrument: {} (ID: {})",
                                selectedInstrument.getName(), selectedInstrument.getId());

                        // Update UI components showing instrument details
                        updateControlCodesTable();
                        updateCaptionsTable();
                    } else {
                        logger.warn("Selected instrument not found: {}", instrumentName);
                    }
                } else {
                    selectedInstrument = null;
                    updateControlCodesTable(); // Clear tables when no selection
                    updateCaptionsTable();
                }
            }
        });
    }

    private void setupControlCodesTableSelectionListener() {
        controlCodesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = controlCodesTable.getSelectedRow() >= 0;
                controlCodesContextMenu.setEditEnabled(hasSelection);
                controlCodesContextMenu.setDeleteEnabled(hasSelection);
                addCaptionButton.setEnabled(hasSelection);
                captionsContextMenu.setAddEnabled(hasSelection);
            }
        });
    }

    private void setupCaptionsTableSelectionListener() {
        captionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = captionsTable.getSelectedRow() >= 0 && selectedControlCode != null;
                editCaptionButton.setEnabled(hasSelection);
                deleteCaptionButton.setEnabled(hasSelection);
                captionsContextMenu.setEditEnabled(hasSelection);
                captionsContextMenu.setDeleteEnabled(hasSelection);
            }
        });
    }

    private void editSelectedInstrument() {
        logger.info("Edit instrument button clicked");
        int row = instrumentsTable.getSelectedRow();
        logger.info("Selected row: {}", row);

        if (row >= 0) {
            try {
                // Convert view row to model row if table is sorted
                int modelRow = instrumentsTable.convertRowIndexToModel(row);
                logger.info("Model row: {}", modelRow);

                // Fix: Get ID as Long from column 0, and name from column 1
                Object idObj = instrumentsTable.getModel().getValueAt(modelRow, 0);
                logger.info("ID object type: {}, value: {}", idObj != null ? idObj.getClass().getName() : "null",
                        idObj);

                Long id = (Long) idObj;
                String name = (String) instrumentsTable.getModel().getValueAt(modelRow, 1);

                logger.info("Attempting to edit instrument: {} (ID: {})", name, id);

                // Find the instrument by ID instead of name
                InstrumentWrapper instrument = PlaybackService.getInstance().getInstrument(id);
                logger.info("Retrieved instrument: {}", instrument != null ? instrument.getName() : "null");

                final InstrumentWrapper finalInstrument = instrument;
                if (finalInstrument != null) {
                    logger.info("Found instrument, showing dialog...");
                    // Use invokeLater to avoid potential thread issues
                    SwingUtilities.invokeLater(() -> showInstrumentDialog(finalInstrument));
                } else {
                    logger.error("Failed to find instrument: {} (ID: {})", name, id);
                    CommandBus.getInstance().publish(
                            Commands.STATUS_UPDATE,
                            this,
                            new StatusUpdate("InstrumentsPanel", "Error",
                                    "Failed to find instrument: " + name + " (ID: " + id + ")"));

                    // Fallback: try finding by name if ID failed

                    if (instrument != null) {
                        logger.info("Found instrument by name instead, showing dialog...");
                        SwingUtilities.invokeLater(() -> showInstrumentDialog(instrument));
                    }
                }
            } catch (Exception e) {
                logger.error("Error editing instrument: {}", e.getMessage(), e);
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("InstrumentsPanel", "Error",
                                "Error editing instrument: " + e.getMessage()));
            }
        } else {
            logger.warn("Edit button clicked but no row selected");
        }
    }

    private void setupKeyBindings() {
        // For instruments table
        instrumentsTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteInstrument");
        instrumentsTable.getActionMap().put("deleteInstrument", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (instrumentsTable.getSelectedRow() >= 0) {
                    deleteSelectedInstrument();
                }
            }
        });

        // For control codes table
        controlCodesTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteControlCode");
        controlCodesTable.getActionMap().put("deleteControlCode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (controlCodesTable.getSelectedRow() >= 0) {
                    deleteSelectedControlCode();
                }
            }
        });

        // For captions table
        captionsTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteCaption");
        captionsTable.getActionMap().put("deleteCaption", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (captionsTable.getSelectedRow() >= 0) {
                    deleteSelectedCaption();
                }
            }
        });
    }

    private void saveInstrument(InstrumentWrapper instrument) {
        try {
            // Save to Redis
            RedisService.getInstance().saveInstrument(instrument);

            // Also update in UserConfigManager
            updateInstrumentInUserConfig(instrument);

            // Publish event for all listeners
            CommandBus.getInstance().publish(Commands.INSTRUMENT_UPDATED, this, instrument);

            // Refresh the table
            refreshInstrumentsTable();

            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("Save instrument: " + instrument.getName()));

        } catch (Exception e) {
            logger.error("Error saving instrument: " + e.getMessage());
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("Error saving instrument: " + e.getMessage()));
        }
    }

    private void updateInstrumentInUserConfig(InstrumentWrapper instrument) {
        UserConfig config = UserConfigManager.getInstance().getCurrentConfig();

        // Update existing or add new
        boolean found = false;
        if (config.getInstruments() != null) {
            for (int i = 0; i < config.getInstruments().size(); i++) {
                if (config.getInstruments().get(i).getId().equals(instrument.getId())) {
                    config.getInstruments().set(i, instrument);
                    found = true;
                    break;
                }
            }
        }

        if (!found && config.getInstruments() != null) {
            config.getInstruments().add(instrument);
        }

        // Save updated config
        UserConfigManager.getInstance().saveConfiguration(config);
    }

    private void enableSelectedInstrument() {
        int row = instrumentsTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        // Convert view index to model index if table is sorted
        int modelRow = instrumentsTable.convertRowIndexToModel(row);
        String name = (String) instrumentsTable.getModel().getValueAt(modelRow, 0);

        InstrumentWrapper instrument = findInstrumentByName(name);
        if (instrument == null) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("Failed to find instrument: " + name));
            logger.error("Failed to find instrument: " + name);
            return;
        }

        try {
            // Try to reconnect the device
            String deviceName = instrument.getDeviceName();
            if (deviceName != null && !deviceName.isEmpty()) {
                // First check if device is available
                List<String> availableDevices = MidiService.getInstance().getOutputDeviceNames();
                if (!availableDevices.contains(deviceName)) {

                    CommandBus.getInstance().publish(
                            Commands.STATUS_UPDATE,
                            this,
                            new StatusUpdate("Device not available: " + deviceName));

                    logger.error("Device not available: " + deviceName);

                    return;
                }

                // Try to reinitialize the device connection
                MidiDevice device = MidiService.getInstance().getDevice(deviceName);
                if (device != null && !device.isOpen()) {
                    device.open();
                }

                boolean connected = device != null && device.isOpen();

                if (connected) {
                    instrument.setDevice(device);
                    instrument.setAvailable(true);
                    // Update in cache/config
                    PlaybackService.getInstance().updateInstrument(instrument);

                    // Update UI and show success message
                    refreshInstrumentsTable();
                    CommandBus.getInstance().publish(
                            Commands.STATUS_UPDATE,
                            this,
                            new StatusUpdate("Instrument " + name + " connected to device " + deviceName));
                } else {
                    CommandBus.getInstance().publish(
                            Commands.STATUS_UPDATE,
                            this,
                            new StatusUpdate("Failed to connect instrument " + name + " to device " + deviceName));
                }
            } else {
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("No device specified for instrument: " + name));
            }
        } catch (Exception e) {
            logger.error("Error enabling instrument: " + e.getMessage());
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("Error enabling instrument: " + e.getMessage()));
        }
    }
}
