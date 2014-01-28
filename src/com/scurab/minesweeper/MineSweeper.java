package com.scurab.minesweeper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * User: jbruchanov
 * Date: 25/11/13
 * Time: 22:20
 */
public class MineSweeper {

    static final int MASK_DATA = 0xF;//0b1111
    static final int MASK_STATE = 0xF << 4; //0b11110000

    public static final int DATA_MINE = 0xF;

    static final int STATE_CLOSED = 0;
    static final int STATE_OPEN = 1 << 5;
    static final int STATE_FLAG = 1 << 6;

    public interface MineSweeperDelegate {
        /**
         * Called when user initiated real step and survived
         *
         * @param row
         * @param column
         * @param adjacents
         */
        void onSaveStep(int row, int column, int adjacents);

        /**
         * User steped on mine => dead
         *
         * @param row
         * @param column
         */
        void onMineStep(int row, int column);

        /**
         * Called when user flagged field as minefield
         *
         * @param row
         * @param column
         */
        void onShowFlag(int row, int column);

        /**
         * Show help to user
         *
         * @param row
         * @param column
         * @param data   {@link #DATA_MINE} for mine present or value of adjacents
         */
        void onShowHelp(int row, int column, int data);

        /**
         * Set this field into default not opened state
         *
         * @param row
         * @param column
         */
        void onReset(int row, int column);
    }

    private Random mRandom = new Random(System.currentTimeMillis());

    /* Mines in game */
    private int mMines = 10;

    /* Dataset for game */
    private final int[] mMineField;

    /* Current size which means sqrt(buttons) */
    private int mSize;

    /* Delegate for events */
    private final MineSweeperDelegate mDelegate;


    public MineSweeper(int size, int mines, MineSweeperDelegate delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate can't be null!");
        }
        if (size < 3) {
            throw new IllegalArgumentException("Size of field must be at least 3 (3x3)!");
        }
        if (mines < 0) {
            throw new IllegalArgumentException("Number of mines can't be negative!");
        }
        int squareSize = size * size;
        if (mines > squareSize) {
            throw new IllegalArgumentException("Number of mines can't be bigger then size!");
        }
        mDelegate = delegate;
        mSize = size;
        mMines = mines;
        mMineField = new int[squareSize];

        initArea(mMines);
        generateAdjacents();
    }

    /**
     * Fill array with random mines
     *
     * @param count
     */
    private void initArea(int count) {
        //gen mines
        for (int index : getRandomIndexesForMines(count)) {
            mMineField[index] = DATA_MINE;
        }
        //init state
        for (int i = 0; i < mMineField.length; i++) {
            mMineField[i] |= STATE_CLOSED;
        }
    }

    /**
     * Little more sophisticated approach how to generate random mine index with sureness of mine amount
     *
     * @param size
     * @return
     */
    private int[] getRandomIndexesForMines(int size) {
        int[] subData = new int[mMineField.length];
        //gen indexes
        for (int i = 0; i < subData.length; i++) {
            subData[i] = i;
        }
        //random order of indexes
        for (int i = 0; i < subData.length && i < size; i++) {
            int v = subData[i];
            int rfIndex = i + mRandom.nextInt(subData.length - i);
            subData[i] = subData[rfIndex];
            subData[rfIndex] = v;
        }
        //just copy result
        int[] result = new int[size];
        System.arraycopy(subData, 0, result, 0, result.length);
        return result;
    }

    void generateAdjacents() {
        for (int i = 0; i < mMineField.length; i++) {
            if ((mMineField[i] & MASK_DATA) != DATA_MINE) {
                mMineField[i] += getMinesAround(i);
            }
        }
    }

    /**
     * Open field by user
     *
     * @param row
     * @param column
     */
    public void onStep(int row, int column) {
        int index = getIndex(row, column);
        if (hasState(row, column, STATE_CLOSED)) {
            int data = mMineField[index] & MASK_DATA;
            if (data == DATA_MINE) {
                mDelegate.onMineStep(row, column);
            } else {
                mDelegate.onSaveStep(row, column, data);
                if (data == 0) {
                    onZeroStep(row, column);
                }
            }
            mMineField[index] = data | STATE_OPEN;
        }
    }

    /**
     * Flag field by "flag icon"
     *
     * @param row
     * @param column
     * @return true is flag has ben turned on
     */
    public void onFlag(int row, int column) {
        int index = getIndex(row, column);
        if (hasState(row, column, STATE_FLAG)) {
            mMineField[index] &= MASK_DATA;
            mDelegate.onReset(row, column);
        } else if (hasState(row, column, STATE_CLOSED)) {
            mMineField[index] = (mMineField[index] & MASK_DATA) | STATE_FLAG;
            mDelegate.onShowFlag(row, column);
        }
    }

    boolean hasState(int rowIndex, int columnIndex, int flag) {
        return (mMineField[getIndex(rowIndex, columnIndex)] & MASK_STATE) == flag;
    }

    boolean hasState(int index, int flag) {
        return (mMineField[index] & MASK_STATE) == flag;
    }

    /**
     * Call this to inform UI what we need to show as mines
     *
     * @param cheating
     */
    public void showCheat(boolean cheating) {
        for (int i = 0, n = mMineField.length; i < n; i++) {
            int row = i / mSize;
            int column = i % mSize;
            if (hasState(i, STATE_CLOSED)) {
                if (cheating) {
                    mDelegate.onShowHelp(row, column, mMineField[i] & MASK_DATA);
                } else {
                    mDelegate.onReset(row, column);
                }
            } else if (hasState(i, STATE_FLAG)) {
                if (cheating) {
                    mDelegate.onShowHelp(row, column, mMineField[i] & MASK_DATA);
                } else {
                    mDelegate.onShowFlag(row, column);
                }
            }
        }
    }

    /**
     * Return index based on row and column
     *
     * @param row
     * @param column
     * @return
     */
    int getIndex(int row, int column) {
        return row * mSize + column;
    }

    /**
     * Count how many mines are around index
     *
     * @param index
     * @return
     */
    int getMinesAround(int index) {
        int result = 0;
        for (int i = 0, n = WAYS.length; i < n; i++) {
            int indexToCheck = getIndex(index, WAYS[i]);
            if (indexToCheck != -1) {
                result += ((mMineField[indexToCheck] & MASK_DATA) == DATA_MINE) ? 1 : 0;
            }
        }
        return result;
    }

    /**
     * Show rest of uncovered fields
     *
     * @return true if game is in success finish
     */
    public boolean finishGame() {
        int notOpened = 0;
        for (int i = 0, n = mMineField.length; i < n; i++) {
            int row = i / mSize;
            int column = i % mSize;
            if (!hasState(i, STATE_OPEN)) {
                int data = mMineField[i] & MASK_DATA;
                mDelegate.onShowHelp(row, column, data);
                notOpened++;
                mMineField[i] = data | STATE_OPEN;
            }
        }
        return notOpened == mMines;
    }

    private static final Way[] WAYS = Way.values();

    /* help enum for navigation */
    enum Way {
        NW, N, NE, W, E, SW, S, SE
    }

    /**
     * Get index of adjacent field<br/>
     * <p/>
     * -1 is returned in case when you are asking for not existing place
     * I.g. TopLeft field doesn't have West or North fields around
     *
     * @param center
     * @param way
     * @return index of field or -1 if it's not valid
     */
    int getIndex(int center, Way way) {
        final int div = (center / mSize);
        final int mod = (center % mSize);

        boolean isLeftEdge = mod == 0;
        boolean isTopEdge = div == 0;
        boolean isRightEdge = mod == (mSize - 1);
        boolean isBottomEdge = div == (mSize - 1);

        int value = -1;
        switch (way) {
            case NW:
                value = (isTopEdge || isLeftEdge) ? -1 : center - mSize - 1;
                break;
            case N:
                value = (isTopEdge) ? -1 : center - mSize;
                break;
            case NE:
                value = (isTopEdge || isRightEdge) ? -1 : center - mSize + 1;
                break;
            case W:
                value = (isLeftEdge) ? -1 : center + -1;
                break;
            case E:
                value = (isRightEdge) ? -1 : center + 1;
                break;
            case SW:
                value = (isLeftEdge || isBottomEdge) ? -1 : center + mSize - 1;
                break;
            case S:
                value = (isBottomEdge) ? -1 : center + mSize;
                break;
            case SE:
                value = (isBottomEdge || isRightEdge) ? -1 : center + mSize + 1;
        }
        return value == mMineField.length ? -1 : value;
    }


    /**
     * Go through adjacent fields with 0 and notify UI
     *
     * @param row
     * @param column
     */
    void onZeroStep(int row, int column) {
        List<Integer> bfs = new ArrayList<Integer>();
        bfs.add(getIndex(row, column));

        while (bfs.size() > 0) {

            Iterator<Integer> iter = bfs.iterator();
            List<Integer> wave = new ArrayList<Integer>();

            while (iter.hasNext()) {
                int index = iter.next();

                //look around for zeros
                for (int i = 0, n = WAYS.length; i < n; i++) {
                    Way w = WAYS[i];
                    int adjIndex = getIndex(index, w);
                    if (adjIndex != -1  //is valid index
                            && hasState(adjIndex, STATE_CLOSED) //not opened yet
                            && ((mMineField[adjIndex] & MASK_DATA) == 0)) { //has 0 adjacents

                        wave.add(adjIndex);
                        mMineField[adjIndex] = STATE_OPEN;
                        //notify UI
                        mDelegate.onSaveStep(adjIndex / mSize, adjIndex % mSize, 0);
                    }
                }
                iter.remove();
            }
            bfs = wave;
        }
    }

    /**
     * Save current state of game
     *
     * @return
     */
    int[] saveInstance() {
        int[] data = new int[mMineField.length];
        System.arraycopy(mMineField, 0, data, 0, data.length);
        return data;
    }

    /**
     * Restore game, data must be valid otherwise you can put game into dangerous state!
     *
     * @param data
     */
    void restoreInstance(int[] data) {
        if (data.length != mMineField.length) {
            throw new IllegalArgumentException("Data size is different than current game!");
        }
        System.arraycopy(data, 0, mMineField, 0, data.length);
        onRestoreUI();
    }

    private void onRestoreUI() {
        for (int i = 0, n = mMineField.length; i < n; i++) {
            int row = i / mSize;
            int column = i % mSize;
            int data = mMineField[i] & MASK_DATA;
            if (hasState(i, STATE_OPEN)) {
                mDelegate.onShowHelp(row, column, data);
            } else if (hasState(i, STATE_FLAG)) {
                mDelegate.onShowFlag(row, column);
            }
        }
    }
}
