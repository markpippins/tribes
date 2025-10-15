package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class LaunchPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(LaunchPanel.class);

    private static final int[] LAUNCH_PAD_LABELS = {
            13, 14, 15, 16, // inputs 1-4 map to 13,14,15,16
            9, 10, 11, 12, // inputs 5-8 map to 9,10,11,12
            5, 6, 7, 8, // inputs 9-12 map to 5,6,7,8
            1, 2, 3, 4 // inputs 13-16 map to 1,2,3,4
    };

    JPanel gridPanel;

    public LaunchPanel() {
        super(new BorderLayout());
        UIHelper.setPanelBorder(this);
        CommandBus.getInstance().register(this, new String[]{Commands.CHANGE_THEME});
        setup();
    }

    @Override
    public void onAction(Command action) {
        if (Commands.CHANGE_THEME.equals(action.getCommand())) {
            SwingUtilities.invokeLater(() -> {
                // Remove existing grid panel
                remove(gridPanel);

                // Create new grid panel with updated theme colors
                gridPanel = createGridPanel();
                add(gridPanel, BorderLayout.CENTER);

                // Ensure proper layout
                revalidate();
                repaint();

                // Add debug message
                logger.debug("LaunchPanel: Grid panel recreated after theme change");
            });
        }
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createGridPanel(), BorderLayout.CENTER);
    }

    private JPanel createGridPanel() {
        gridPanel = new JPanel(new GridLayout(8, 8, 2, 2));
        // gridPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        Color[] quadrantColors = {
                UIHelper.mutedRed, // Top-left
                UIHelper.mutedOlive, // Top-right
                UIHelper.warmMustard, // Bottom-left
                UIHelper.fadedOrange // Bottom-right
        };

        int[] count = {1, 1, 1, 1};
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int quadrant = (row / 4) * 2 + (col / 4);

                JButton padButton = createDrumPadButton(row * 8 + col, quadrantColors[quadrant]);
                padButton.setText(Integer.toString(getLabelText(count[quadrant]++)));

                gridPanel.add(padButton);
            }
        }

        return gridPanel;
    }

    public int getLabelText(int input) {
        return (input >= 1 && input <= 16) ? LAUNCH_PAD_LABELS[input - 1] : input;
    }

    private JButton createDrumPadButton(int index, Color baseColor) {
        JButton button = new JButton();

        Color flashColor = new Color(
                Math.min(baseColor.getRed() + 100, 255),
                Math.min(baseColor.getGreen() + 100, 255),
                Math.min(baseColor.getBlue() + 100, 255));

        final boolean[] isFlashing = {false};

        button.addActionListener(e -> {
            isFlashing[0] = true;
            button.repaint();

            // Update status when pad is pressed
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("Launch Panel", "Playing", "Pad " + button.getText()));

            Timer timer = new Timer(100, evt -> {
                isFlashing[0] = false;
                button.repaint();
                ((Timer) evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                if (isFlashing[0]) {
                    g2d.setColor(flashColor);
                    g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                } else {
                    g2d.setColor(baseColor);
                    g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                }

                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                String text = ((JButton) c).getText();
                if (text != null && !text.isEmpty()) {
                    g2d.setFont(new Font("Arial", Font.BOLD, 16));
                    g2d.setColor(Color.WHITE);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text,
                            (w - textWidth) / 2,
                            ((h + textHeight) / 2) - fm.getDescent());
                }

                g2d.dispose();
            }
        });

        button.setPreferredSize(new Dimension(40, 40));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText("Pad " + index);

        return button;
    }

}
