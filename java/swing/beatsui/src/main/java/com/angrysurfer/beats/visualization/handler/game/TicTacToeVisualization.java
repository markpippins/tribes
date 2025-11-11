package com.angrysurfer.beats.visualization.handler.game;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.core.sequencer.SequencerConstants;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class TicTacToeVisualization implements IVisualizationHandler {
    private static final int BOARD_SIZE = 3;
    private static final Color X_COLOR = Color.RED;
    private static final Color O_COLOR = Color.BLUE;
    private static final Color GRID_COLOR = Color.GRAY;
    private static final int MOVE_DELAY = 20; // frames between moves
    private final Random random;
    private char[][] board;
    private boolean isXTurn;
    private int moveCounter;
    private int frameCounter;

    public TicTacToeVisualization() {
        random = new Random();
        resetGame();
    }

    private void resetGame() {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = ' ';
            }
        }
        isXTurn = true;
        moveCounter = 0;
        frameCounter = 0;
    }

    @Override
    public void update(JButton[][] buttons) {
        int cellSize = Math.min(buttons.length, buttons[0].length) / BOARD_SIZE;
        int offsetY = (buttons.length - cellSize * BOARD_SIZE) / 2;
        int offsetX = (buttons[0].length - cellSize * BOARD_SIZE) / 2;

        // Clear display
        clearDisplay(buttons);

        // Draw grid
        drawGrid(buttons, cellSize, offsetX, offsetY);

        // Draw pieces
        drawPieces(buttons, cellSize, offsetX, offsetY);

        // Make move if needed
        frameCounter++;
        if (frameCounter >= MOVE_DELAY && !isGameOver() && moveCounter < 9) {
            makeMove();
            frameCounter = 0;
        }

        // Reset game if finished
        if (isGameOver() || moveCounter >= SequencerConstants.MIDI_DRUM_CHANNEL) {
            if (frameCounter >= MOVE_DELAY * 2) {
                resetGame();
            }
        }
    }

    private void clearDisplay(JButton[][] buttons) {
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(Color.BLACK);
            }
        }
    }

    private void drawGrid(JButton[][] buttons, int cellSize, int offsetX, int offsetY) {
        // Draw vertical lines
        for (int x = 1; x < BOARD_SIZE; x++) {
            int gridX = offsetX + x * cellSize;
            for (int y = offsetY; y < offsetY + cellSize * BOARD_SIZE; y++) {
                buttons[y][gridX].setBackground(GRID_COLOR);
            }
        }

        // Draw horizontal lines
        for (int y = 1; y < BOARD_SIZE; y++) {
            int gridY = offsetY + y * cellSize;
            for (int x = offsetX; x < offsetX + cellSize * BOARD_SIZE; x++) {
                buttons[gridY][x].setBackground(GRID_COLOR);
            }
        }
    }

    private void drawPieces(JButton[][] buttons, int cellSize, int offsetX, int offsetY) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != ' ') {
                    drawSymbol(buttons, i, j, board[i][j], cellSize, offsetX, offsetY);
                }
            }
        }
    }

    private void drawSymbol(JButton[][] buttons, int boardY, int boardX, char symbol,
                            int cellSize, int offsetX, int offsetY) {
        Color color = symbol == 'X' ? X_COLOR : O_COLOR;
        int startY = offsetY + boardY * cellSize;
        int startX = offsetX + boardX * cellSize;

        if (symbol == 'X') {
            // Draw X
            for (int i = 1; i < cellSize - 1; i++) {
                buttons[startY + i][startX + i].setBackground(color);
                buttons[startY + i][startX + cellSize - i - 1].setBackground(color);
            }
        } else {
            // Draw O
            for (int i = 1; i < cellSize - 1; i++) {
                for (int j = 1; j < cellSize - 1; j++) {
                    double distance = Math.sqrt(Math.pow(i - cellSize / 2.0, 2) +
                            Math.pow(j - cellSize / 2.0, 2));
                    if (Math.abs(distance - cellSize / 3.0) < 1.0) {
                        buttons[startY + i][startX + j].setBackground(color);
                    }
                }
            }
        }
    }

    private void makeMove() {
        // Find empty spaces
        int[] emptySpaces = new int[9 - moveCounter];
        int index = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == ' ') {
                    emptySpaces[index++] = i * BOARD_SIZE + j;
                }
            }
        }

        // Make random move
        int move = emptySpaces[random.nextInt(emptySpaces.length)];
        int row = move / BOARD_SIZE;
        int col = move % BOARD_SIZE;
        board[row][col] = isXTurn ? 'X' : 'O';

        isXTurn = !isXTurn;
        moveCounter++;
    }

    private boolean isGameOver() {
        // Check rows, columns and diagonals
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (board[i][0] != ' ' && board[i][0] == board[i][1] && board[i][1] == board[i][2]) return true;
            if (board[0][i] != ' ' && board[0][i] == board[1][i] && board[1][i] == board[2][i]) return true;
        }
        if (board[0][0] != ' ' && board[0][0] == board[1][1] && board[1][1] == board[2][2]) return true;
        return board[0][2] != ' ' && board[0][2] == board[1][1] && board[1][1] == board[2][0];
    }

    @Override
    public String getName() {
        return "Tic-Tac-Toe";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GAME;
    }
}
