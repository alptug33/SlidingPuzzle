package com.alp2.slidingpuzzle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NPuzzleSolver {
    private int[][] grid;
    private boolean[][] fixed;
    private Map<Integer, Position> numbers;
    private List<Move> solution;
    private final int size;

    public static class Position {
        int x, y;
        
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        Position offset(String direction) {
            switch (direction) {
                case "u": return new Position(x, y - 1);
                case "d": return new Position(x, y + 1);
                case "l": return new Position(x - 1, y);
                case "r": return new Position(x + 1, y);
                case "ul": return new Position(x - 1, y - 1);
                case "ur": return new Position(x + 1, y - 1);
                case "dl": return new Position(x - 1, y + 1);
                case "dr": return new Position(x + 1, y + 1);
                default: return new Position(x, y);
            }
        }
    }

    public static class Move {
        public final int number;
        public final Position from;
        public final Position to;

        Move(int number, Position from, Position to) {
            this.number = number;
            this.from = from;
            this.to = to;
        }
    }

    public NPuzzleSolver(List<Integer> puzzle, int size) {
        this.size = size;
        this.grid = new int[size][size];
        this.fixed = new boolean[size][size];
        this.numbers = new HashMap<>();
        this.solution = new ArrayList<>();

        // Convert 1D list to 2D grid
        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int num = puzzle.get(index++);
                grid[i][j] = num;
                numbers.put(num, new Position(j, i));
                fixed[i][j] = false;
            }
        }
    }

    public List<Move> solve() {
        solution.clear();
        try {
            solveGrid(size);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return solution;
    }

    private void solveGrid(int size) {
        if (size > 2) {
            solveRow(size);    // solve the upper row first
            solveColumn(size); // solve the left column next
            solveGrid(size - 1); // solve the sub (n-1)x(n-1) puzzle
        } else if (size == 2) {
            solveRow(size);
            Position empty = numbers.get(0);
            if (grid[grid.length - 1][grid.length - size] == 0) {
                swapE(new Position(grid.length - 1, grid.length - 1));
            }
        }
    }

    private void solveRow(int size) {
        int rowNumber = grid.length - size;
        // Solve all but last two numbers in row
        for (int i = rowNumber; i < grid.length - 2; i++) {
            int number = rowNumber * grid.length + (i + 1);
            moveNumberTowards(number, new Position(i, rowNumber));
            fixed[rowNumber][i] = true;
        }

        int secondToLast = rowNumber * grid.length + grid.length - 1;
        int last = secondToLast + 1;

        moveNumberTowards(secondToLast, new Position(grid.length - 1, rowNumber));
        moveNumberTowards(last, new Position(grid.length - 1, rowNumber + 1));

        Position secondToLastPos = numbers.get(secondToLast);
        Position lastPos = numbers.get(last);

        if (secondToLastPos.x != grid.length - 1 || secondToLastPos.y != rowNumber ||
            lastPos.x != grid.length - 1 || lastPos.y != rowNumber + 1) {
            
            moveNumberTowards(secondToLast, new Position(grid.length - 1, rowNumber));
            moveNumberTowards(last, new Position(grid.length - 2, rowNumber));
            moveEmptyTo(new Position(grid.length - 2, rowNumber + 1));

            Position pos = new Position(grid.length - 1, rowNumber + 1);
            String[] moves = {"ul", "u", "", "l", "dl", "d", "", "l", "ul", "u", "", "l", "ul", "u", "", "d"};
            applyRelativeMoveList(pos, moves);
        }

        specialTopRightRotation(rowNumber);
    }

    private void solveColumn(int size) {
        int colNumber = grid.length - size;
        for (int i = colNumber; i < grid.length - 2; i++) {
            int number = i * grid.length + 1 + colNumber;
            moveNumberTowards(number, new Position(colNumber, i));
            fixed[i][colNumber] = true;
        }

        int secondToLast = (grid.length - 2) * grid.length + 1 + colNumber;
        int last = secondToLast + grid.length;

        moveNumberTowards(secondToLast, new Position(colNumber, grid.length - 1));
        moveNumberTowards(last, new Position(colNumber + 1, grid.length - 1));

        Position secondToLastPos = numbers.get(secondToLast);
        Position lastPos = numbers.get(last);

        if (secondToLastPos.x != colNumber || secondToLastPos.y != grid.length - 1 ||
            lastPos.x != colNumber + 1 || lastPos.y != grid.length - 1) {

            moveNumberTowards(secondToLast, new Position(colNumber, grid.length - 1));
            moveNumberTowards(last, new Position(colNumber, grid.length - 2));
            moveEmptyTo(new Position(colNumber + 1, grid.length - 2));

            Position pos = new Position(colNumber + 1, grid.length - 1);
            String[] moves = {"ul", "l", "", "u", "ur", "r", "", "u", "ul", "l", "", "u", "ul", "l", "", "r"};
            applyRelativeMoveList(pos, moves);
        }

        specialLeftBottomRotation(colNumber);
    }

    private void moveNumberTowards(int number, Position dest) {
        Position current = numbers.get(number);
        if (current.x == dest.x && current.y == dest.y) return;

        makeEmptyNeighborTo(number);
        while (numbers.get(number).x != dest.x || numbers.get(number).y != dest.y) {
            String direction = getDirectionToProceed(number, dest);
            if (!areNeighbors(number, 0)) {
                throw new RuntimeException("Cannot rotate without empty");
            }
            if (direction.equals("u") || direction.equals("d")) {
                rotateVertical(number, direction.equals("u"));
            } else {
                rotateHorizontal(number, direction.equals("l"));
            }
        }
    }

    private void rotateHorizontal(int number, boolean leftDirection) {
        String side = leftDirection ? "l" : "r";
        String other = leftDirection ? "r" : "l";
        Position empty = numbers.get(0);
        Position pos = numbers.get(number);

        if (empty.y != pos.y) {
            String location = (empty.y < pos.y) ? "u" : "d";
            Position offsetPos = pos.offset(location + side);
            if (!moveable(offsetPos) || !moveable(pos.offset(location))) {
                swapE(pos.offset(location + other));
                swapE(pos.offset(other));
                proper3By2RotationHorizontal(pos, leftDirection);
            } else {
                swapE(pos.offset(location + side));
                swapE(pos.offset(side));
            }
        } else if ((empty.x < pos.x && !leftDirection) || (empty.x > pos.x && leftDirection)) {
            proper3By2RotationHorizontal(pos, leftDirection);
        }
        swapE(pos);
    }

    private void proper3By2RotationHorizontal(Position pos, boolean leftDirection) {
        String side = leftDirection ? "l" : "r";
        String other = leftDirection ? "r" : "l";
        String location = "u";

        if (moveable(pos.offset("d" + side)) && moveable(pos.offset("d")) && moveable(pos.offset("d" + other))) {
            location = "d";
        } else if (!moveable(pos.offset("u" + side)) || !moveable(pos.offset("u")) || !moveable(pos.offset("u" + other))) {
            throw new RuntimeException("Unable to move up all spots fixed");
        }

        swapE(pos.offset(location + other));
        swapE(pos.offset(location));
        swapE(pos.offset(location + side));
        swapE(pos.offset(side));
    }

    private void rotateVertical(int number, boolean upDirection) {
        String toward = upDirection ? "u" : "d";
        String away = upDirection ? "d" : "u";
        Position empty = numbers.get(0);
        Position pos = numbers.get(number);

        if (empty.x != pos.x) {
            String side = (empty.x < pos.x) ? "l" : "r";
            if (!moveable(pos.offset(toward + side)) || !moveable(pos.offset(side))) {
                swapE(pos.offset(away + side));
                swapE(pos.offset(away));
                proper2By3RotationVertical(pos, upDirection);
            } else {
                swapE(pos.offset(toward + side));
                swapE(pos.offset(toward));
            }
        } else if ((empty.y < pos.y && !upDirection) || (empty.y > pos.y && upDirection)) {
            proper2By3RotationVertical(pos, upDirection);
        }
        swapE(pos);
    }

    private void proper2By3RotationVertical(Position pos, boolean upDirection) {
        String toward = upDirection ? "u" : "d";
        String away = upDirection ? "d" : "u";
        String side = "r";

        if (moveable(pos.offset(toward + "l")) && moveable(pos.offset("l")) && moveable(pos.offset(away + "l"))) {
            side = "l";
        } else if (!moveable(pos.offset(toward + "r")) || !moveable(pos.offset("r")) || !moveable(pos.offset(away + "r"))) {
            throw new RuntimeException("Unable to perform move, the puzzle is quite possibly unsolveable");
        }

        swapE(pos.offset(away + side));
        swapE(pos.offset(side));
        swapE(pos.offset(toward + side));
        swapE(pos.offset(toward));
    }

    private void specialTopRightRotation(int top) {
        fixed[top][grid.length - 1] = true;
        fixed[top + 1][grid.length - 1] = true;

        Position topRight = new Position(grid.length - 1, top);
        moveEmptyTo(topRight.offset("l"));
        swapE(topRight);
        swapE(topRight.offset("d"));

        fixed[top + 1][grid.length - 1] = false;
        fixed[topRight.y][topRight.x - 1] = true;
    }

    private void specialLeftBottomRotation(int left) {
        fixed[grid.length - 1][left] = true;
        fixed[grid.length - 1][left + 1] = true;

        Position leftBottom = new Position(left, grid.length - 1);
        moveEmptyTo(leftBottom.offset("u"));
        swapE(leftBottom);
        swapE(leftBottom.offset("r"));

        fixed[grid.length - 1][left + 1] = false;
        fixed[leftBottom.y - 1][leftBottom.x] = true;
    }

    private String getDirectionToProceed(int number, Position dest) {
        Position cur = numbers.get(number);
        int diffx = dest.x - cur.x;
        int diffy = dest.y - cur.y;

        if (diffx < 0 && moveable(new Position(cur.x - 1, cur.y))) return "l";
        if (diffx > 0 && moveable(new Position(cur.x + 1, cur.y))) return "r";
        if (diffy < 0 && moveable(new Position(cur.x, cur.y - 1))) return "u";
        if (diffy > 0 && moveable(new Position(cur.x, cur.y + 1))) return "d";

        throw new RuntimeException("There is no valid move, the puzzle was incorrectly shuffled");
    }

    private void makeEmptyNeighborTo(int number) {
        Position target = numbers.get(number);
        Position empty = numbers.get(0);
        int counter = 0;

        while ((empty.x != target.x || empty.y != target.y) && !areNeighbors(0, number)) {
            movingEmptyLoop(target);
            if (++counter > 100) {
                throw new RuntimeException("Infinite loop hit while solving the puzzle");
            }
        }
    }

    private void moveEmptyTo(Position pos) {
        if (fixed[pos.y][pos.x]) {
            throw new RuntimeException("Cannot move empty to a fixed position");
        }

        Position empty = numbers.get(0);
        int counter = 0;
        while (empty.x != pos.x || empty.y != pos.y) {
            movingEmptyLoop(pos);
            if (++counter > 100) break;
            empty = numbers.get(0);
        }
    }

    private void movingEmptyLoop(Position target) {
        Position empty = numbers.get(0);
        int diffx = empty.x - target.x;
        int diffy = empty.y - target.y;

        if (diffx < 0 && canSwap(empty, empty.offset("r"))) {
            swap(empty, empty.offset("r"));
        } else if (diffx > 0 && canSwap(empty, empty.offset("l"))) {
            swap(empty, empty.offset("l"));
        } else if (diffy < 0 && canSwap(empty, empty.offset("d"))) {
            swap(empty, empty.offset("d"));
        } else if (diffy > 0 && canSwap(empty, empty.offset("u"))) {
            swap(empty, empty.offset("u"));
        }
    }

    private void applyRelativeMoveList(Position pos, String[] moves) {
        for (String move : moves) {
            if (move.isEmpty()) {
                swapE(pos);
            } else {
                swapE(pos.offset(move));
            }
        }
    }

    private boolean areNeighbors(int first, int second) {
        Position num1 = numbers.get(first);
        Position num2 = numbers.get(second);
        return (Math.abs(num1.x - num2.x) == 1 && num1.y == num2.y) ||
               (Math.abs(num1.y - num2.y) == 1 && num1.x == num2.x);
    }

    private boolean moveable(Position pos) {
        return validPos(pos) && !fixed[pos.y][pos.x];
    }

    private boolean validPos(Position pos) {
        return !(pos.x < 0 || pos.x >= grid.length || pos.y < 0 || pos.y >= grid.length);
    }

    private boolean canSwap(Position pos1, Position pos2) {
        if (!validPos(pos1) || !validPos(pos2)) return false;
        
        int num1 = grid[pos1.y][pos1.x];
        int num2 = grid[pos2.y][pos2.x];
        
        if (!areNeighbors(num1, num2)) return false;
        return !fixed[pos1.y][pos1.x] && !fixed[pos2.y][pos2.x];
    }

    private void swapE(Position pos) {
        swap(numbers.get(0), pos);
    }

    private void swap(Position pos1, Position pos2) {
        int num1 = grid[pos1.y][pos1.x];
        int num2 = grid[pos2.y][pos2.x];

        if (!areNeighbors(num1, num2)) {
            throw new RuntimeException("These numbers are not neighbors and cannot be swapped");
        }
        if (num1 != 0 && num2 != 0) {
            throw new RuntimeException("You must swap with an empty space");
        }

        Position oldPos1 = numbers.get(num1);
        numbers.put(num1, numbers.get(num2));
        numbers.put(num2, oldPos1);
        
        grid[pos1.y][pos1.x] = num2;
        grid[pos2.y][pos2.x] = num1;

        solution.add(new Move(num1 == 0 ? num2 : num1,
                            num1 == 0 ? pos2 : pos1,
                            num1 == 0 ? pos1 : pos2));
    }
} 