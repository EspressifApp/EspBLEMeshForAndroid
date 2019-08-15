package libs.espressif.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

public class CircleColorPicker extends ColorPicker {
    private static final int[] COLORS_SWEEP = {
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
    };
    private static final int[] COLORS_RADIAL = {0xffffffff, 0x00ffffff};

    private RectF mRectF;
    private float mRadius;

    private int[] mSweepColors;
    private Shader mSweepShader;
    private Paint mSweepPaint;

    private int[] mRadialColors;
    private Shader mRadialShader;
    private Paint mRadialPaint;

    private float[] mPositions;

    public CircleColorPicker(Context context) {
        super(context);
        init(context);
    }

    public CircleColorPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircleColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mRectF = new RectF();
        mSweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRadialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setSweepColors(COLORS_SWEEP);
        setRadialColors(COLORS_RADIAL);

        mPositions = null;
    }

    private void setSweepColors(int[] sweepColors) {
        mSweepColors = sweepColors;
    }

    private void setRadialColors(int[] radialColors) {
        mRadialColors = radialColors;
    }

    private void setPositions(float[] positions) {
        mPositions = positions;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = getWidth();
        int height = getHeight();
        mRectF.left = 0;
        mRectF.right = width;
        mRectF.top = 0;
        mRectF.bottom = height;
        mRadius = Math.min(mRectF.width(), mRectF.height()) / 2;
        if (mSweepShader == null && mSweepColors != null) {
            mSweepShader = new SweepGradient(mRectF.centerX(), mRectF.centerY(), mSweepColors, mPositions);
            mSweepPaint.setShader(mSweepShader);
        }
        if (mRadialShader == null && mRadialColors != null) {
            mRadialShader = new RadialGradient(mRectF.centerX(), mRectF.centerY(), mRadius,
                    mRadialColors, null, Shader.TileMode.CLAMP);
            mRadialPaint.setShader(mRadialShader);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mSweepShader != null) {
            canvas.drawCircle(mRectF.centerX(), mRectF.centerY(), mRadius, mSweepPaint);
        }
        if (mRadialColors != null) {
            canvas.drawCircle(mRectF.centerX(), mRectF.centerY(), mRadius, mRadialPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float circleX = x - mRectF.centerX();
        float circleY = y - mRectF.centerY();

        int color = Color.TRANSPARENT;
        if (mSweepColors != null) {
            float angle = (float) Math.atan2(circleY, circleX);
            float unit = (float) (angle / (2 * Math.PI));
            if (unit < 0) {
                unit += 1;
            }
            color = interpCircleColor(mSweepColors, unit);
        }

        float s = 1f;
        if (mRadialColors != null) {
            float distanceCenter = (float) Math.sqrt(circleX * circleX + circleY * circleY);
            if (distanceCenter > mRadius) {
                distanceCenter = mRadius;
            }
            s = distanceCenter / mRadius;
        }

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = s;

        color = Color.HSVToColor(hsv);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChangeStart(this, color);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChanged(this, color);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChangeEnd(this, color);
                }
                break;
        }

        return true;
    }

    private int interpCircleColor(int colors[], float unit) {
        if (unit <= 0) {
            return colors[0];
        }
        if (unit >= 1) {
            return colors[colors.length - 1];
        }

        float p = unit * (colors.length - 1);
        int i = (int) p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i + 1];

        return getGradientColor(c0, c1, p);
    }
}
