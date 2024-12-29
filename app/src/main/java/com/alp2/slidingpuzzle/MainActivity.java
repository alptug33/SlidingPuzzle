package com.alp2.slidingpuzzle;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.HashSet;
import java.util.Set;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity {
    private GridLayout puzzleGrid;
    private Button btnShuffle;
    private Button btnSolve;
    private TextView moveCounterText;
    private int gridSize = 3;
    private List<Button> tiles;
    private int emptyPosition;
    private int moveCount = 0;
    private boolean isSolving = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        puzzleGrid = findViewById(R.id.puzzleGrid);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnSolve = findViewById(R.id.btnSolve);
        moveCounterText = findViewById(R.id.moveCounterText);

        findViewById(R.id.btn2x2).setOnClickListener(v -> setGridSize(2));
        findViewById(R.id.btn3x3).setOnClickListener(v -> setGridSize(3));
        findViewById(R.id.btn4x4).setOnClickListener(v -> setGridSize(4));
        findViewById(R.id.btn5x5).setOnClickListener(v -> setGridSize(5));
        btnShuffle.setOnClickListener(v -> shuffleTiles());
        btnSolve.setOnClickListener(v -> solvePuzzle());

        initializeGame();
    }

    private void setGridSize(int size) {
        gridSize = size;
        initializeGame();
    }

    private void initializeGame() {
        moveCount = 0;
        updateMoveCounter();
        puzzleGrid.removeAllViews();
        puzzleGrid.setColumnCount(gridSize);
        puzzleGrid.setRowCount(gridSize);
        
        tiles = new ArrayList<>();
        int totalTiles = gridSize * gridSize;

        // Create tiles
        for (int i = 0; i < totalTiles - 1; i++) {
            Button tile = createTile(i + 1);
            tiles.add(tile);
            puzzleGrid.addView(tile);
        }

        // Add empty tile
        Button emptyTile = createTile(0);
        emptyTile.setBackgroundColor(Color.TRANSPARENT);
        emptyTile.setText("");
        tiles.add(emptyTile);
        puzzleGrid.addView(emptyTile);
        emptyPosition = totalTiles - 1;

        shuffleTiles();
    }

    private void updateMoveCounter() {
        moveCounterText.setText("Hamle: " + moveCount);
        moveCounterText.setTextColor(Color.parseColor("#5C4033"));
    }

    private Button createTile(final int number) {
        Button tile = new Button(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        int tileSize = (int) (getResources().getDisplayMetrics().widthPixels * 0.8 / gridSize);
        params.width = tileSize;
        params.height = tileSize;
        params.setMargins(4, 4, 4, 4);
        tile.setLayoutParams(params);

        // Create tile background with pastel colors
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(Color.parseColor("#C4B280")); // Pastel sand color
        shape.setStroke(2, Color.parseColor("#B4A26C")); // Darker sand color for border
        shape.setCornerRadius(12); // Slightly more rounded corners
        tile.setBackground(shape);

        tile.setText(number > 0 ? String.valueOf(number) : "");
        tile.setTextColor(Color.parseColor("#5C4033")); // Dark brown text color
        tile.setTextSize(20); // Larger text size
        tile.setOnClickListener(v -> {
            if (!isSolving) {
                moveTile(tiles.indexOf(tile));
            }
        });

        return tile;
    }

    private void moveTile(int position) {
        // Check if the clicked tile is adjacent to the empty tile
        if (isAdjacent(position, emptyPosition)) {
            // Swap the tiles
            Collections.swap(tiles, position, emptyPosition);
            updateTilePositions();
            emptyPosition = position;
            moveCount++;
            updateMoveCounter();

            // Check if puzzle is solved
            if (isPuzzleSolved()) {
                Toast.makeText(this, "Tebrikler! Bulmacayı " + moveCount + " hamlede çözdünüz!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void solvePuzzle() {
        if (isSolving) return;
        isSolving = true;
        btnSolve.setEnabled(false);
        btnShuffle.setEnabled(false);

        new Thread(() -> {
            List<Integer> solution = findSolution();
            if (solution != null) {
                for (int i = 0; i < solution.size() && isSolving; i++) {
                    final int movePosition = solution.get(i);
                    final int currentMove = i;
                    try {
                        Thread.sleep(300);
                        handler.post(() -> {
                            if (!isSolving) return;
                            moveTile(movePosition);
                            if (currentMove == solution.size() - 1) {
                                isSolving = false;
                                btnSolve.setEnabled(true);
                                btnShuffle.setEnabled(true);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, 
                        "Bu durum için çözüm bulunamadı! Tekrar karıştırıp deneyin.", Toast.LENGTH_LONG).show();
                    isSolving = false;
                    btnSolve.setEnabled(true);
                    btnShuffle.setEnabled(true);
                });
            }
        }).start();
    }

    private List<Integer> findSolution() {
        List<Integer> currentState = new ArrayList<>();
        for (Button tile : tiles) {
            String text = tile.getText().toString();
            currentState.add(text.isEmpty() ? 0 : Integer.parseInt(text));
        }

        NPuzzleSolver solver = new NPuzzleSolver(currentState, gridSize);
        List<NPuzzleSolver.Move> solution = solver.solve();
        
        if (solution == null) return null;
        
        List<Integer> moves = new ArrayList<>();
        for (NPuzzleSolver.Move move : solution) {
            // Convert position to index
            moves.add(move.from.y * gridSize + move.from.x);
        }
        return moves;
    }

    private boolean isAdjacent(int pos1, int pos2) {
        int row1 = pos1 / gridSize;
        int col1 = pos1 % gridSize;
        int row2 = pos2 / gridSize;
        int col2 = pos2 % gridSize;

        return (Math.abs(row1 - row2) == 1 && col1 == col2) ||
               (Math.abs(col1 - col2) == 1 && row1 == row2);
    }

    private void updateTilePositions() {
        puzzleGrid.removeAllViews();
        for (Button tile : tiles) {
            puzzleGrid.addView(tile);
        }
    }

    private void shuffleTiles() {
        moveCount = 0;
        updateMoveCounter();
        // Shuffle tiles randomly
        do {
            Collections.shuffle(tiles.subList(0, tiles.size() - 1));
        } while (!isSolvable());

        updateTilePositions();
        emptyPosition = tiles.indexOf(tiles.get(tiles.size() - 1));
    }

    private boolean isSolvable() {
        int inversions = 0;
        List<Integer> numbers = new ArrayList<>();
        
        for (Button tile : tiles) {
            String text = tile.getText().toString();
            if (!text.isEmpty()) {
                numbers.add(Integer.parseInt(text));
            }
        }

        for (int i = 0; i < numbers.size() - 1; i++) {
            for (int j = i + 1; j < numbers.size(); j++) {
                if (numbers.get(i) > numbers.get(j)) {
                    inversions++;
                }
            }
        }

        // For odd grid sizes, number of inversions must be even for the puzzle to be solvable
        if (gridSize % 2 == 1) {
            return inversions % 2 == 0;
        }
        // For even grid sizes, puzzle is solvable if:
        // (blank on even row from bottom + odd inversions) or (blank on odd row from bottom + even inversions)
        else {
            int blankRowFromBottom = gridSize - (emptyPosition / gridSize);
            return (blankRowFromBottom % 2 == 0 && inversions % 2 == 1) ||
                   (blankRowFromBottom % 2 == 1 && inversions % 2 == 0);
        }
    }

    private boolean isPuzzleSolved() {
        for (int i = 0; i < tiles.size() - 1; i++) {
            String text = tiles.get(i).getText().toString();
            if (text.isEmpty() || Integer.parseInt(text) != i + 1) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSolving = false; // Stop solver when activity is destroyed
    }
}