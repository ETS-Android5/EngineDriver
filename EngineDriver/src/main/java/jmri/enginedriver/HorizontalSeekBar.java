package jmri.enginedriver;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.SeekBar;

@SuppressLint("AppCompatCustomView")
public class HorizontalSeekBar extends SeekBar {

    private SharedPreferences prefs;
    protected int prefDisplaySpeedUnits = 100;
    protected boolean prefTickMarksOnSliders = true;
    public boolean tickMarksChecked = false;

    Paint tickPaint;

    protected int steps;
    protected int height;
    protected int width;
    protected int paddingTop;
    protected int paddingBottom;
    protected float gridMiddle;
    protected float tickSpacing;
    protected float sizeIncrease;
    protected float d;
    protected float l;
    protected float r;


    // A change listener registrating start and stop of tracking. Need an own listener because the listener in SeekBar
    // is private.
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public boolean touchFromUser = false;
    public boolean realTouch = true;

    public threaded_application mainapp;  // hold pointer to mainapp

    public HorizontalSeekBar(final Context context) {
        super(context);
        mainapp = (threaded_application) context.getApplicationContext();
    }

    public HorizontalSeekBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        mainapp = (threaded_application) context.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        tickMarksChecked = false;

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
    }

    public HorizontalSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mainapp = (threaded_application) context.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        tickMarksChecked = false;

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(context.getResources().getColor(R.color.seekBarTickColor));
    }


//    @Override
//    protected final void onSizeChanged(final int width, final int height, final int oldWidth, final int oldHeight) {
//        super.onSizeChanged(height, width, oldHeight, oldWidth);
//    }
//
//    @Override
//    protected final synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
//        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
//        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
//    }

    @Override
    protected final void onDraw(final Canvas c) {

        if (!tickMarksChecked) {
            tickMarksChecked = true;
            prefTickMarksOnSliders = prefs.getBoolean("prefTickMarksOnSliders", getResources().getBoolean(R.bool.prefTickMarksOnSlidersDefaultValue));
            prefDisplaySpeedUnits = preferences.getIntPrefValue(prefs, "DisplaySpeedUnits", getResources().getString(R.string.prefDisplaySpeedUnitsDefaultValue));

            steps = prefDisplaySpeedUnits;
            if (steps >= 100) {
                steps = steps / 3;
            } else {
                if (steps < 28) {
                    steps = steps * 3;
                }
            }
        }
        if (prefTickMarksOnSliders) {
            height = getHeight();
            width = getWidth();

            int additionalPadding = 30;
            int startSize = 10;
            if (height < 100 ) {
                startSize = 2;
                additionalPadding = 15;
            }

            gridMiddle = height / 2;

            tickSpacing = (width - (getPaddingLeft()*2) ) / (steps - 1);
            sizeIncrease = (gridMiddle - getPaddingTop() - additionalPadding) / (steps * steps);

            for (int i = -1; i < steps; i++) {
                d = getPaddingLeft() + i * tickSpacing;
                l = gridMiddle - startSize - sizeIncrease * i * i;
                r = gridMiddle + startSize + sizeIncrease * i * i;
                c.drawLine(d, l, d, r, tickPaint);   // x, y, end_x, end_y
            }
        }

        super.onDraw(c);
    }

}