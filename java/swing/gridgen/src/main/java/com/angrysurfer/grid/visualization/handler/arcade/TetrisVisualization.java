package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.awt.Point;
import java.util.Arrays;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class TetrisVisualization implements IVisualizationHandler {
    private boolean[][] board;
    private int[][] currentPiece;
    private Point piecePos;
    private int updateCount = 0;
    private final Random random = new Random();
    private final Color[] colors = {
        new Color(0,240,240), // Cyan
        new Color(240,240,0), // Yellow
        new Color(160,0,240), // Purple
        new Color(0,240,0),   // Green
        new Color(240,0,0),   // Red
        new Color(0,0,240),   // Blue
        new Color(240,160,0)  // Orange
    };
    private int currentColor = 0;

    private final int[][][] PIECES = {
        {{1,1,1,1}},         // I
        {{1,1},{1,1}},       // O
        {{0,1,0},{1,1,1}},   // T
        {{1,1,0},{0,1,1}},   // S
        {{0,1,1},{1,1,0}},   // Z
        {{1,0,0},{1,1,1}},   // L
        {{0,0,1},{1,1,1}}    // J
    };

    @Override
    public void update(JButton[][] buttons) {
        if (board == null) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        updateCount++;
        if (updateCount % 5 == 0) { // Move piece down every 5 updates
            movePiece(0, 1);
        }

        // AI control - move piece towards best position
        if (updateCount % 2 == 0) {
            int bestMove = findBestMove();
            movePiece(bestMove, 0);
        }

        // Draw the fixed blocks
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                if (board[y][x]) {
                    buttons[y][x].setBackground(colors[y % colors.length]);
                }
            }
        }

        // Draw current piece
        if (currentPiece != null) {
            for (int y = 0; y < currentPiece.length; y++) {
                for (int x = 0; x < currentPiece[y].length; x++) {
                    if (currentPiece[y][x] == 1) {
                        int drawY = piecePos.y + y;
                        int drawX = piecePos.x + x;
                        if (drawY >= 0 && drawY < buttons.length &&
                            drawX >= 0 && drawX < buttons[0].length) {
                            buttons[drawY][drawX].setBackground(colors[currentColor]);
                        }
                    }
                }
            }
        }
    }

    private void movePiece(int dx, int dy) {
        if (canMove(dx, dy)) {
            piecePos.x += dx;
            piecePos.y += dy;
        } else if (dy > 0) { // Can't move down
            fixPiece();
            clearLines();
            spawnPiece();
        }
    }

    private boolean canMove(int dx, int dy) {
        for (int y = 0; y < currentPiece.length; y++) {
            for (int x = 0; x < currentPiece[y].length; x++) {
                if (currentPiece[y][x] == 1) {
                    int newX = piecePos.x + x + dx;
                    int newY = piecePos.y + y + dy;
                    if (newX < 0 || newX >= board[0].length ||
                        newY >= board.length ||
                        (newY >= 0 && board[newY][newX])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void fixPiece() {
        for (int y = 0; y < currentPiece.length; y++) {
            for (int x = 0; x < currentPiece[y].length; x++) {
                if (currentPiece[y][x] == 1) {
                    int boardY = piecePos.y + y;
                    if (boardY >= 0) {
                        board[boardY][piecePos.x + x] = true;
                    }
                }
            }
        }
    }

    private void clearLines() {
        for (int y = board.length - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < board[0].length; x++) {
                if (!board[y][x]) {
                    full = false;
                    break;
                }
            }
            if (full) {
                for (int moveY = y; moveY > 0; moveY--) {
                    System.arraycopy(board[moveY-1], 0, board[moveY], 0, board[0].length);
                }
                Arrays.fill(board[0], false);
            }
        }
    }

    private int findBestMove() {
        int bestMove = 0;
        int minHeight = Integer.MAX_VALUE;

        // Try each possible horizontal position
        for (int testX = -2; testX <= 2; testX++) {
            if (canMove(testX, 0)) {
                int height = getHighestPoint();
                if (height < minHeight) {
                    minHeight = height;
                    bestMove = testX;
                }
            }
        }
        return bestMove;
    }

    private int getHighestPoint() {
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                if (board[y][x]) return y;
            }
        }
        return board.length;
    }

    private void spawnPiece() {
        int pieceIdx = random.nextInt(PIECES.length);
        currentPiece = PIECES[pieceIdx];
        currentColor = pieceIdx;
        piecePos = new Point(board[0].length/2 - currentPiece[0].length/2, -1);
    }

    private void initializeGame(JButton[][] buttons) {
        board = new boolean[buttons.length][buttons[0].length];
        spawnPiece();
    }

    @Override
    public String getName() {
        return "Tetris";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
