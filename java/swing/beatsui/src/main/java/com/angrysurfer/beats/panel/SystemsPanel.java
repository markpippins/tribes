package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.Dialog;
import com.angrysurfer.beats.panel.instrument.InstrumentEditPanel;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.PlaybackService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemsPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(SystemsPanel.class);
    private final JTable devicesTable;
    private final JPopupMenu contextMenu;
    private final JMenuItem createInstrumentMenuItem;

    public SystemsPanel() {
        super(new BorderLayout());
        this.devicesTable = createDevicesTable();
        this.createInstrumentMenuItem = new JMenuItem("Create Instrument"); // Create this FIRST
        this.contextMenu = createContextMenu(); // Then create the menu that uses it
        setupContextMenu();
        setupLayout();
    }

    private static DefaultTableModel createTableModel() {
        String[] columns = {
                "Name", "Description", "Vendor", "Version", "Max Receivers",
                "Max Transmitters", "Receivers", "Transmitters", "Receiver", "Transmitter"
        };

        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 8 || column == 9) {
                    return Boolean.class;
                }
                if (column >= 4 && column <= 7) {
                    return Integer.class;
                }
                return String.class;
            }
        };
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Create toolbar with refresh button only
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh Devices");
        refreshButton.addActionListener(e -> refreshDevicesTable());
        toolBar.add(refreshButton);

        // Add components to panel
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(devicesTable), BorderLayout.CENTER);

        // Add MIDI test controls to the bottom of the panel instead of toolbar
        add(createMidiTestControls(), BorderLayout.SOUTH);
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        createInstrumentMenuItem.addActionListener(e -> createInstrumentFromDevice());
        menu.add(createInstrumentMenuItem);
        return menu;
    }

    private void setupContextMenu() {
        devicesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseEvent(e);
            }

            private void handleMouseEvent(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // Get the row at the click location
                    Point p = e.getPoint();
                    int row = devicesTable.rowAtPoint(p);

                    // If row is valid and not already selected, select it
                    if (row >= 0 && !devicesTable.isRowSelected(row)) {
                        devicesTable.setRowSelectionInterval(row, row);
                    }

                    // Only enable Create Instrument if device is a receiver (output device)
                    boolean isOutputDevice = false;
                    if (row >= 0) {
                        // Check Max Receivers column (column 4, value of 0 means no receiving capability)
                        Object maxReceivers = devicesTable.getValueAt(row, 4);
                        isOutputDevice = maxReceivers != null && !maxReceivers.equals(0);
                    }

                    createInstrumentMenuItem.setEnabled(isOutputDevice);
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private JTable createDevicesTable() {
        DefaultTableModel model = createTableModel();

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 4; i < 8; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);

        // Set fixed widths for numeric columns
        for (int i = 4; i <= 7; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(60);
            table.getColumnModel().getColumn(i).setMaxWidth(60);
        }

        // Populate the table
        loadMidiDevices(model);

        return table;
    }

    private void loadMidiDevices(DefaultTableModel model) {
        model.setRowCount(0); // Clear existing rows

        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                model.addRow(new Object[]{
                        info.getName(),
                        info.getDescription(),
                        info.getVendor(),
                        info.getVersion(),
                        device.getMaxReceivers(),
                        device.getMaxTransmitters(),
                        device.getReceivers().size(),
                        device.getTransmitters().size(),
                        device.getMaxReceivers() != 0,
                        device.getMaxTransmitters() != 0
                });
            }
        } catch (MidiUnavailableException e) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("Systems Panel", "Error", "Error loading MIDI devices: " + e.getMessage()));
            logger.error("Error loading MIDI devices: {}", e.getMessage(), e);
        }
    }

    private void refreshDevicesTable() {
        DefaultTableModel model = (DefaultTableModel) devicesTable.getModel();
        loadMidiDevices(model);
        CommandBus.getInstance().publish(
                Commands.STATUS_UPDATE,
                this,
                new StatusUpdate("Systems Panel", "Info", "MIDI devices refreshed"));
        logger.info("MIDI devices refreshed");
    }

    private void createInstrumentFromDevice() {
        int row = devicesTable.getSelectedRow();
        if (row < 0) return;

        try {
            // Get the device name from the selected row
            String deviceName = (String) devicesTable.getValueAt(row, 0);

            // Create a new InstrumentWrapper pre-configured with the device
            InstrumentWrapper newInstrument = new InstrumentWrapper();
            newInstrument.setDeviceName(deviceName);
            newInstrument.setName(deviceName); // Default name to device name
            newInstrument.setLowestNote(0);    // Default range
            newInstrument.setHighestNote(127); // Default range
            newInstrument.setInitialized(true);

            // Generate a consistent ID for this device
            // Use a hash of the device name to make it consistent
            long deviceId = Math.abs(deviceName.hashCode());

            // For Gervill specifically, use a well-known ID to match MelodicSequencer
            if (SequencerConstants.GERVILL.equals(deviceName) || deviceName.contains(SequencerConstants.GERVILL)) {
                // Base ID for Gervill (matches MelodicSequencer's 9985L base)
                deviceId = 9985L;
            }

            newInstrument.setId(deviceId);

            // Show the edit dialog
            InstrumentEditPanel editorPanel = new InstrumentEditPanel(newInstrument);
            Dialog<InstrumentWrapper> dialog = new Dialog<>(newInstrument, editorPanel);
            dialog.setTitle("Create Instrument from Device");

            if (dialog.showDialog()) {
                InstrumentWrapper updatedInstrument = editorPanel.getUpdatedInstrument();

                // Ensure ID is preserved
                if (updatedInstrument.getId() == null) {
                    updatedInstrument.setId(deviceId);
                }

                // Save the new instrument
                RedisService.getInstance().saveInstrument(updatedInstrument);

                // Update local cache too
                PlaybackService.getInstance().updateInstrument(updatedInstrument);

                // Rest of notification code...
            }
        } catch (Exception e) {
            // Error handling...
        }
    }

    /**
     * Creates MIDI testing controls for the toolbar
     */
    private JPanel createMidiTestControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBorder(BorderFactory.createTitledBorder("MIDI Test"));

        // --- Note testing section ---

        // Channel selector for notes (shared by all sections)
        JPanel channelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        channelPanel.add(new JLabel("Ch:"));
        JSpinner channelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 16, 1));
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
        sendNoteButton.addActionListener(e -> sendMidiNote(
                (Integer) channelSpinner.getValue() - 1, // Convert 1-16 to 0-15
                (Integer) noteSpinner.getValue(),
                (Integer) velocitySpinner.getValue()
        ));
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
        sendCCButton.addActionListener(e -> sendMidiControlChange(
                (Integer) channelSpinner.getValue() - 1, // Convert 1-16 to 0-15
                (Integer) ccSpinner.getValue(),
                (Integer) valueSpinner.getValue()
        ));
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
        sendPCButton.addActionListener(e -> sendMidiProgramChange(
                (Integer) channelSpinner.getValue() - 1, // Convert 1-16 to 0-15
                (Integer) programSpinner.getValue()
        ));
        panel.add(sendPCButton);

        return panel;
    }

    /**
     * Sends a MIDI note message to the selected output device
     */
    private void sendMidiNote(int channel, int noteNumber, int velocity) {
        try {
            // Get selected output device from device selection
            MidiDevice device = getSelectedOutputDevice();
            if (device == null) {
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("MIDI Test", "Error", "No MIDI output device selected")
                );
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
                                    noteNumber, channel + 1, velocity))
            );

        } catch (Exception e) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Error", "Failed to send MIDI note: " + e.getMessage())
            );
            logger.error("Error sending MIDI note", e);
        }
    }

    /**
     * Sends a MIDI control change message to the selected output device
     */
    private void sendMidiControlChange(int channel, int controlNumber, int value) {
        try {
            // Get selected output device from device selection
            MidiDevice device = getSelectedOutputDevice();
            if (device == null) {
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("MIDI Test", "Error", "No MIDI output device selected")
                );
                return;
            }

            // Open device if not already open
            if (!device.isOpen()) {
                device.open();
            }

            // Get receiver
            Receiver receiver = device.getReceiver();

            // Create control change message
            ShortMessage cc = new ShortMessage();
            cc.setMessage(ShortMessage.CONTROL_CHANGE, channel, controlNumber, value);
            receiver.send(cc, -1);

            // Log and update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Info",
                            String.format("Sent CC: %d on channel: %d with value: %d",
                                    controlNumber, channel + 1, value))
            );

        } catch (Exception e) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Error", "Failed to send MIDI CC: " + e.getMessage())
            );
            logger.error("Error sending MIDI control change", e);
        }
    }

    /**
     * Sends a MIDI program change message to the selected output device
     */
    private void sendMidiProgramChange(int channel, int programNumber) {
        try {
            // Get selected output device from device selection
            MidiDevice device = getSelectedOutputDevice();
            if (device == null) {
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("MIDI Test", "Error", "No MIDI output device selected")
                );
                return;
            }

            // Open device if not already open
            if (!device.isOpen()) {
                device.open();
            }

            // Get receiver
            Receiver receiver = device.getReceiver();

            // Create program change message
            ShortMessage pc = new ShortMessage();
            pc.setMessage(ShortMessage.PROGRAM_CHANGE, channel, programNumber, 0);
            receiver.send(pc, -1);

            // Log and update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Info",
                            String.format("Sent Program Change: %d on channel: %d",
                                    programNumber, channel + 1))
            );

        } catch (Exception e) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Error", "Failed to send Program Change: " + e.getMessage())
            );
            logger.error("Error sending MIDI program change", e);
        }
    }

    /**
     * Gets the currently selected MIDI output device
     */
    private MidiDevice getSelectedOutputDevice() {
        // This needs to be implemented based on how devices are selected in your UI
        // For SystemsPanel, you'd likely use the selected row in the device table
        int selectedRow = devicesTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }

        // Get the device name from selected row
        String deviceName = (String) devicesTable.getValueAt(selectedRow, 0);

        // Find the matching device
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                if (info.getName().equals(deviceName)) {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // Make sure it's an output device
                    if (device.getMaxReceivers() != 0) {
                        return device;
                    }
                }
            }
        } catch (MidiUnavailableException e) {
            logger.error("Error getting MIDI device", e);
        }

        return null;
    }
}
