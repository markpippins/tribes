package com.angrysurfer.grid.widget;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

@Getter
@Setter
public class GridButton extends JButton {

    public static final int BUTTON_SIZE = 15;
    private final Random rand = new Random();
    Color[] colors = {
            new Color(255, 200, 200), // Light red
            new Color(200, 255, 200), // Light green
            new Color(200, 200, 255), // Light blue
            new Color(255, 255, 200), // Light yellow
            new Color(255, 200, 255), // Light purple
            new Color(200, 255, 255) // Light cyan
    };
    private int col;

    public GridButton(int row, int col) {
        this.col = col;
        setup();
    }

    public void clear() {
        setText("");
        setToolTipText("");
        setBackground(getParent().getBackground());
    }

    public void reset() {
        setText("");
        setToolTipText("");
        setBackground(getParent().getBackground());
    }

    public void randomize() {
        setText("");
        setToolTipText("");
        setBackground(colors[rand.nextInt(colors.length)]);
    }

    public void setOn(boolean on) {
        if (!on) {
            setBackground(Color.RED);
        }
        repaint();
    }

    private void setup() {
        setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setBackground(colors[rand.nextInt(colors.length)]);
        setOpaque(true);
        setBorderPainted(true);
    }
}
