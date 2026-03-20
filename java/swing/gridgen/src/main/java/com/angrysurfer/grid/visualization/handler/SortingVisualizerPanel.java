package com.angrysurfer.grid.visualization.handler;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Random;

public class SortingVisualizerPanel extends JPanel {
    private int[] array;
    private int currentIndex;
    private int compareIndex;
    private final int BAR_WIDTH = 5;
    private final int DEFAULT_HEIGHT = 400; // Add default height
    private boolean isSorting = false;
    private Timer timer;
    private JComboBox<String> algorithmSelect;
    private JButton startButton;
    private JButton resetButton;
    private JSlider speedSlider;

    public SortingVisualizerPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, DEFAULT_HEIGHT)); // Set preferred size
        setupControls();
        
        // Delay initialization until component is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                initializeArray();
            }
        });
    }

    private void setupControls() {
        JPanel controlPanel = new JPanel();
        algorithmSelect = new JComboBox<>(new String[]{"Bubble Sort", "Selection Sort", "Insertion Sort", "Quick Sort"});
        startButton = new JButton("Start");
        resetButton = new JButton("Reset");
        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 50);
        
        startButton.addActionListener(e -> startSorting());
        resetButton.addActionListener(e -> resetArray());

        controlPanel.add(new JLabel("Algorithm:"));
        controlPanel.add(algorithmSelect);
        controlPanel.add(startButton);
        controlPanel.add(resetButton);
        controlPanel.add(new JLabel("Speed:"));
        controlPanel.add(speedSlider);

        add(controlPanel, BorderLayout.NORTH);
    }

    private void initializeArray() {
        array = new int[100];
        Random rand = new Random();
        int maxHeight = Math.max(getHeight() - 50, DEFAULT_HEIGHT - 50); // Use default height if actual height is too small
        for (int i = 0; i < array.length; i++) {
            array[i] = rand.nextInt(maxHeight) + 10;
        }
        currentIndex = 0;
        compareIndex = 0;
        repaint();
    }

    private void startSorting() {
        if (isSorting) return;
        isSorting = true;
        startButton.setEnabled(false);
        algorithmSelect.setEnabled(false);

        String selectedAlgorithm = (String) algorithmSelect.getSelectedItem();
        switch (selectedAlgorithm) {
            case "Bubble Sort" -> bubbleSort();
            case "Selection Sort" -> selectionSort();
            case "Insertion Sort" -> insertionSort();
            case "Quick Sort" -> quickSort();
        }
    }

    private void resetArray() {
        if (timer != null) timer.stop();
        isSorting = false;
        startButton.setEnabled(true);
        algorithmSelect.setEnabled(true);
        initializeArray();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = 10;
        for (int i = 0; i < array.length; i++) {
            if (i == currentIndex)
                g2d.setColor(Color.RED);
            else if (i == compareIndex)
                g2d.setColor(Color.BLUE);
            else
                g2d.setColor(Color.BLACK);

            g2d.fillRect(x, getHeight() - array[i], BAR_WIDTH, array[i]);
            x += BAR_WIDTH + 1;
        }
    }

    private void bubbleSort() {
        timer = new Timer(101 - speedSlider.getValue(), e -> {
            if (currentIndex >= array.length - 1) {
                ((Timer)e.getSource()).stop();
                isSorting = false;
                startButton.setEnabled(true);
                algorithmSelect.setEnabled(true);
                return;
            }

            for (int i = 0; i < array.length - 1; i++) {
                if (array[i] > array[i + 1]) {
                    int temp = array[i];
                    array[i] = array[i + 1];
                    array[i + 1] = temp;
                    compareIndex = i + 1;
                }
            }
            currentIndex++;
            repaint();
        });
        timer.start();
    }

    private void selectionSort() {
        currentIndex = 0;
        timer = new Timer(101 - speedSlider.getValue(), e -> {
            if (currentIndex >= array.length - 1) {
                ((Timer)e.getSource()).stop();
                isSorting = false;
                startButton.setEnabled(true);
                algorithmSelect.setEnabled(true);
                return;
            }

            int minIdx = currentIndex;
            for (int i = currentIndex + 1; i < array.length; i++) {
                compareIndex = i;
                if (array[i] < array[minIdx]) {
                    minIdx = i;
                }
            }

            int temp = array[currentIndex];
            array[currentIndex] = array[minIdx];
            array[minIdx] = temp;
            currentIndex++;
            repaint();
        });
        timer.start();
    }

    private void insertionSort() {
        currentIndex = 1;
        timer = new Timer(101 - speedSlider.getValue(), e -> {
            if (currentIndex >= array.length) {
                ((Timer)e.getSource()).stop();
                isSorting = false;
                startButton.setEnabled(true);
                algorithmSelect.setEnabled(true);
                return;
            }

            int key = array[currentIndex];
            compareIndex = currentIndex - 1;

            while (compareIndex >= 0 && array[compareIndex] > key) {
                array[compareIndex + 1] = array[compareIndex];
                compareIndex--;
            }
            array[compareIndex + 1] = key;
            currentIndex++;
            repaint();
        });
        timer.start();
    }

    private void quickSort() {
        // Implement quicksort visualization
        // This is a simplified version that just sorts instantly
        Arrays.sort(array);
        repaint();
        isSorting = false;
        startButton.setEnabled(true);
        algorithmSelect.setEnabled(true);
    }
}
