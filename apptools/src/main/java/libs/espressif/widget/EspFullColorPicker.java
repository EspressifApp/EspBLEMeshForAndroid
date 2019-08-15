package libs.espressif.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;

public class EspFullColorPicker extends ColorPicker {
    /**
     * Vertical color
     */
    private static final int[] mColorsH = new int[]{
            0xFFFF0000,
            0xFFFFFF00,
            0xFF00FF00,
            0xFF00FFFF,
            0xFF0000FF,
            0xFFFF00FF,
            0xFFFF0000,
    };
    /**
     * Horizontal color
     */
    private static final int[] mColorsV = new int[]{
            0xFFFFFFFF,
            0x00FFFFFF,
            0x00000000,
            0xFF000000,
    };
    /**
     * Position of horizontal color
     */
    private static final float[] mPositionsV = new float[]{
            0f,
            0.5f,
            0.5f,
            1f,
    };

    private static final float MIN_HEIGHT = 20; // Unit is dip;

    private Context mContext;

    private RectF mRectF;
    private Paint mPaintH;
    private Shader mShaderH;
    private Paint mPaintV;
    private Shader mShaderV;

    private boolean mClicked;
    private float mLineVX;
    private float mLineHY;
    private Paint mLinePaint;
    private float mLineWidth;

    public EspFullColorPicker(Context context) {
        super(context);
        init(context);
    }

    public EspFullColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EspFullColorPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        float minHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_HEIGHT, dm);
        setMinimumHeight((int) minHeight);

        mRectF = new RectF();
        mPaintV = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintH = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(0x80000000);
        mLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = getWidth();
        int height = getHeight();

        mRectF.left = 0;
        mRectF.top = 0;
        mRectF.right = width;
        mRectF.bottom = height;

        if (mShaderH == null) {
            mShaderH = new LinearGradient(mRectF.left, 0, mRectF.right, 0, mColorsH, null, TileMode.MIRROR);
            mPaintH.setShader(mShaderH);
        }
        if (mShaderV == null) {
            mShaderV = new LinearGradient(0, mRectF.top, 0, mRectF.bottom, mColorsV, mPositionsV, TileMode.MIRROR);
            mPaintV.setShader(mShaderV);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(mRectF, mPaintH);
        canvas.drawRect(mRectF, mPaintV);

        if (mClicked) {
            float lineWidthOffset = mLineWidth / 2;
            canvas.drawRect(mLineVX - lineWidthOffset, 0, mLineVX + lineWidthOffset, getHeight(), mLinePaint);
            canvas.drawRect(0, mLineHY - lineWidthOffset, getWidth(), mLineHY + lineWidthOffset, mLinePaint);
        }

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = mLineVX = event.getX();
        float y = mLineHY = event.getY();

        if (mLineVX < mRectF.left) {
            mLineVX = mRectF.left;
        } else if (mLineVX > mRectF.right) {
            mLineVX = mRectF.right;
        }

        if (mLineHY < mRectF.top) {
            mLineHY = mRectF.top;
        } else if (mLineHY > mRectF.bottom) {
            mLineHY = mRectF.bottom;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mClicked = true;
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChangeStart(this, getTouchPointColor(x, y));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChanged(this, getTouchPointColor(x, y));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mClicked = false;
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChangeEnd(this, getTouchPointColor(x, y));
                }
                break;
        }

        invalidate();

        return true;
    }

    private int getTouchPointColor(float x, float y) {
        // get Horizontal color
        int colorH = interceptColor(mColorsH, x, mRectF.left, mRectF.right);
        // get touch point color
        int[] colorsV = {Color.WHITE, colorH, Color.BLACK};
        int color = interceptColor(colorsV, y, mRectF.top, mRectF.bottom);

        return color;
    }

    /**
     * Set the width of the touch point axis line
     *
     * @param value the unit is PX
     */
    public void setLineWidth(float value) {
        setLineWidth(TypedValue.COMPLEX_UNIT_PX, value);
    }

    /**
     * Set the width of the touch point axis line
     *
     * @param unit  @see TypedValue
     * @param value
     */
    public void setLineWidth(int unit, float value) {
        mLineWidth = TypedValue.applyDimension(unit, value, mContext.getResources().getDisplayMetrics());
    }
}
