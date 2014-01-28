package com.scurab.minesweeper;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * User: jbruchanov
 * Date: 25/11/13
 * Time: 22:56
 */
public class PlayButton extends ImageButton {

    public interface OnPlayButtonClickListener {
        /**
         * @param source
         * @param rowIndex
         * @param columnIndex
         */
        void onClick(PlayButton source, int rowIndex, int columnIndex);
    }

    /* Help offsets for centering text */
    private float mTextOffsetY;
    private float mTextOffsetX;

    /* current data value for button */
    private int mRowIndex;
    private int mColumnIndex;

    private String mText;
    /* text paint */
    private Paint mPaint;
    /* different colors for adjacent values */
    private int[] mColors = new int[4];

    //region listeners
    private OnPlayButtonClickListener mClickListener;
    private OnPlayButtonClickListener mLongClickListener;
    //endregion

    public PlayButton(Context context) {
        super(context);
        init(null);
    }

    public PlayButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PlayButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        Resources r = getResources();
        initColors(r);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        float textSize = 0f;

        int textColor = Color.BLACK;
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.PlayButton);
            textSize = array.getDimension(R.styleable.PlayButton_textSize, 0);
            if (textSize == 0) {
                textSize = 14 * r.getDisplayMetrics().scaledDensity;
            }
            textColor = array.getColor(R.styleable.PlayButton_textColor, Color.BLACK);
            array.recycle();
        }

        mPaint.setTextSize(textSize);
        mPaint.setColor(textColor);
    }

    private void initColors(Resources res) {
        mColors[0] = res.getColor(R.color.adj_0);
        mColors[1] = res.getColor(R.color.adj_1);
        mColors[2] = res.getColor(R.color.adj_2);
        mColors[3] = res.getColor(R.color.adj_3);
    }

    @Override
    public boolean performClick() {
        if (mClickListener != null) {
            mClickListener.onClick(this, mRowIndex, mColumnIndex);
        }
        return super.performClick();
    }

    @Override
    public boolean performLongClick() {
        if (mLongClickListener != null) {
            mLongClickListener.onClick(this, mRowIndex, mColumnIndex);
            return true;
        }
        return super.performLongClick();
    }

    //region get/set
    public int getRowIndex() {
        return mRowIndex;
    }

    public void setPosition(int rowIndex, int columnIndex) {
        mRowIndex = rowIndex;
        mColumnIndex = columnIndex;
        setContentDescription(getContext().getString(R.string.a11y_button_open, mRowIndex + 1, mColumnIndex + 1));
    }

    public int getColumnIndex() {
        return mColumnIndex;
    }

    //endregion get/set


    public CharSequence getText() {
        return mText;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            setText(mText);//reset text for proper center position
        }
    }

    public void setText(String text) {
        if (!TextUtils.equals(text, mText)) {
            mText = text;
            Rect r = new Rect();
            mPaint.getTextBounds(mText, 0, mText.length(), r);
            mTextOffsetY = r.bottom - ((r.bottom - r.top) >> 1);
            mTextOffsetX = -mPaint.measureText(mText) / 2;
            invalidate();
        }
    }

    public void setAdjacents(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Invalid value:" + value);
        }
        mPaint.setColor(mColors[Math.min(value, mColors.length - 1)]);
        setText(String.valueOf(value));
        setEnabled(false);
        setContentDescription(getContext().getString(R.string.a11y_button_closed, getRowIndex() + 1, getColumnIndex() + 1, value));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mText != null) {
            canvas.drawText(mText, (getWidth() >> 1) + mTextOffsetX, (getHeight() >> 1) - mTextOffsetY, mPaint);
        }
    }

    public void setOnPlayButtonClickListener(OnPlayButtonClickListener clickListener) {
        mClickListener = clickListener;
    }

    public void setOnPlayButtonLongClickListener(OnPlayButtonClickListener longClickListener) {
        mLongClickListener = longClickListener;
        setLongClickable(longClickListener != null);
    }

    /**
     * Reset button to default state => hide icon and remove text
     */
    public void reset() {
        setImageDrawable(null);
        mText = "";
        setEnabled(true);
        resetContentDescription();
        invalidate();
    }

    //region a11y

    /**
     * A11y Helper only
     * Call this if button is "flagged" by user
     */
    public void setIsFlagged() {
        setContentDescription(getContext().getString(R.string.a11y_button_flagged, getRowIndex() + 1, getColumnIndex() + 1));
    }

    /**
     * A11y Helper only
     * Call this when button has mine
     */
    public void setHasMine() {
        setContentDescription(getContext().getString(R.string.a11y_button_mine, getRowIndex() + 1, getColumnIndex() + 1));
    }

    /**
     * A11y Helper only
     * Reset content description to default value
     */
    public void resetContentDescription() {
        setContentDescription(getContext().getString(R.string.a11y_button_open, getRowIndex() + 1, getColumnIndex() + 1));
    }
    //region a11y
}
