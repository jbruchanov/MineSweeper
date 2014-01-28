package com.scurab.minesweeper;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * User: jbruchanov
 * Date: 25/11/13
 * Time: 22:54
 */
public class PlayArea extends LinearLayout implements MineSweeper.MineSweeperDelegate {

    public interface OnFinishGameListener {
        void onFinishGame(boolean success);
    }

    private int mAreaSize;

    private int mMines;

    private MineSweeper mMineSweeper;

    private OnFinishGameListener mOnFinishGameListener;

    public PlayArea(Context context) {
        super(context);
        init(null);
    }

    public PlayArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setOrientation(LinearLayout.VERTICAL);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.PlayArea);
            mAreaSize = array.getInt(R.styleable.PlayArea_areaSize, 8);
            mMines = array.getInt(R.styleable.PlayArea_mines, 10);
        }
        mMineSweeper = new MineSweeper(mAreaSize, mMines, this);
        buildPlayArea(mAreaSize);
    }

    //region builders
    void buildPlayArea(int size) {
        for (int i = 0; i < size; i++) {
            addView(buildRow(size, i));
        }
    }

    /**
     * Build particular row for play area
     *
     * @param items
     * @param rowIndex
     * @return
     */
    ViewGroup buildRow(int items, int rowIndex) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < items; i++) {
            row.addView(buildButton(rowIndex, i));
        }
        return row;
    }

    /**
     * Build and init button into row
     *
     * @param row
     * @param column
     * @return
     */
    PlayButton buildButton(int row, int column) {
        PlayButton pb = (PlayButton) View.inflate(getContext(), R.layout.play_button, null);
        pb.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        pb.setPosition(row, column);
        pb.setOnPlayButtonClickListener(mClickListener);
        pb.setOnPlayButtonLongClickListener(mLongClickListener);
        return pb;
    }
    //endregion builders

    private PlayButton.OnPlayButtonClickListener mClickListener = new PlayButton.OnPlayButtonClickListener() {
        @Override
        public void onClick(PlayButton source, int rowIndex, int columnIndex) {
            onButtonClick(source, rowIndex, columnIndex);
        }
    };

    private PlayButton.OnPlayButtonClickListener mLongClickListener = new PlayButton.OnPlayButtonClickListener() {
        @Override
        public void onClick(PlayButton source, int rowIndex, int columnIndex) {
            onLongButtonClick(source, rowIndex, columnIndex);
        }
    };

    public PlayButton getPlayButton(int row, int column) {
        return (PlayButton) ((ViewGroup) getChildAt(row)).getChildAt(column);
    }

    public void onLongButtonClick(PlayButton source, int rowIndex, int columnIndex) {
        mMineSweeper.onFlag(rowIndex, columnIndex);
    }

    public void onButtonClick(PlayButton button, int rowIndex, int columnIndex) {
        mMineSweeper.onStep(rowIndex, columnIndex);
    }

    void vibrate() {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

    @Override
    public void onSaveStep(int row, int column, int adjacents) {
        getPlayButton(row, column).setAdjacents(adjacents);
    }

    @Override
    public void onMineStep(int row, int column) {
        getPlayButton(row, column).setImageResource(R.drawable.mine);
        setEnabled(false);
        vibrate();
        finishGame();
        Toast.makeText(getContext(), R.string.finish_unsuccess, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onShowFlag(int row, int column) {
        PlayButton pb = getPlayButton(row, column);
        pb.setText("");
        pb.setEnabled(true);
        pb.setImageResource(R.drawable.flag);
        pb.setIsFlagged();
    }

    @Override
    public void onShowHelp(int row, int column, int data) {
        PlayButton pb = getPlayButton(row, column);
        pb.setEnabled(false);
        if (MineSweeper.DATA_MINE == data) {
            pb.setImageResource(R.drawable.mine);
            pb.setHasMine();
        } else {
            pb.setImageDrawable(null);
            pb.setAdjacents(data);
        }
    }

    @Override
    public void onReset(int row, int column) {
        getPlayButton(row, column).reset();
    }

    /**
     * Start new game
     */
    public void startNewGame() {
        mMineSweeper = new MineSweeper(mAreaSize, mMines, this);
        for (int row = 0; row < mAreaSize; row++) {
            for (int col = 0; col < mAreaSize; col++) {
                onReset(row, col);
            }
        }
    }

    /**
     * Show not opened fields for cheating
     *
     * @param show
     */
    public void showCheat(boolean show) {
        mMineSweeper.showCheat(show);
    }

    /**
     * Finish and validate games
     *
     * @return true if game is in victory state
     */
    public boolean finishGame() {
        boolean result = mMineSweeper.finishGame();
        if (mOnFinishGameListener != null) {
            mOnFinishGameListener.onFinishGame(result);
        }
        return result;
    }

    public void setOnFinishGameListener(OnFinishGameListener onFinishGameListener) {
        mOnFinishGameListener = onFinishGameListener;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        int[] saved = mMineSweeper.saveInstance();
        ss.dataSize = saved.length;
        ss.data = saved;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mMineSweeper.restoreInstance(ss.data);
    }

    //region state
    static class SavedState extends BaseSavedState {
        int dataSize;
        int[] data;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            dataSize = in.readInt();
            data = new int[dataSize];
            in.readIntArray(data);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(dataSize);
            out.writeIntArray(data);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
    //region state
}
