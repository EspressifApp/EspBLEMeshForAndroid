package libs.espressif.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class LinearColorPicker extends ColorPicker {
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;

    private int mOrientation = HORIZONTAL;

    private int[] mColors;
    private RectF mRectF;
    private Shader mShader;
    private Paint mPaint;

    private Paint mTriaWhitePaint;
    private Paint mTriaBlackPaint;

    private boolean mTouched;

    private Context mContext;

    private float mTriaAxisPercent = -1f;
    private float mTriaAxis;
    private float mTriaLength;
    private float mTriaHeight;
    private float mTriaOffset;

    private float[] mPositions;

    public LinearColorPicker(Context context) {
        super(context);
        init(context);
    }

    public LinearColorPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LinearColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mColors = new int[0];
        mRectF = new RectF();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTriaWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTriaWhitePaint.setColor(Color.WHITE);
        mTriaBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTriaBlackPaint.setColor(Color.BLACK);
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        mTriaLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, dm);
        mTriaHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, dm);
        mTriaOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
        mTriaAxis = mTriaLength / 2;

        mPositions = null;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        switch (mOrientation) {
            case HORIZONTAL:
            case VERTICAL:
                break;
            default:
                throw new IllegalArgumentException("Unknow orientation value");
        }
    }

    public int[] getColors() {
        return mColors;
    }

    public void setColors(int[] colors) {
        if (mColors != colors) {
            mColors = colors;
            if (mShader != null) {
                mShader = generateShader();
                mPaint.setShader(mShader);
                invalidate();
            }
        }
    }

    public void setPositions(float[] positions) {
        mPositions = positions;
    }

    public void updateColor(int position, int newColor) {
        mColors[position] = newColor;
        if (mShader != null) {
            mShader = generateShader();
            mPaint.setShader(mShader);
            invalidate();
        }
    }

    public void setTriaAxis(float axis) {
        mTriaAxis = axis;
        invalidate();
    }

    public void setTriaAxisPercent(float percent) {
        mTriaAxisPercent = percent;
        invalidate();
    }

    public float getTriaAxisPercent() {
        return mTriaAxisPercent;
    }

    private Shader generateShader() {
        switch (mOrientation) {
            case HORIZONTAL:
                return new LinearGradient(mRectF.left, 0, mRectF.right, 0, mColors, mPositions, Shader.TileMode.CLAMP);
            case VERTICAL:
                return new LinearGradient(0, mRectF.top, 0, mRectF.bottom, mColors, mPositions, Shader.TileMode.CLAMP);
            default:
                return null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = getWidth();
        int height = getHeight();

        if (mShader == null) {
            switch (mOrientation) {
                case HORIZONTAL:
                    mRectF.left = 0 + mTriaLength / 2;
                    mRectF.top = mTriaOffset;
                    mRectF.right = width - mTriaLength / 2;
                    mRectF.bottom = height - mTriaOffset;
                    break;
                case VERTICAL:
                    mRectF.left = mTriaOffset;
                    mRectF.top = 0 + mTriaLength / 2;
                    mRectF.right = width - mTriaOffset;
                    mRectF.bottom = height - mTriaLength / 2;
                    break;
            }

            mShader = generateShader();
            mPaint.setShader(mShader);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTriaAxisPercent >= 0f) {
            float start = 0f;
            float length = 0f;
            switch (mOrientation) {
                case HORIZONTAL:
                    start = mRectF.left;
                    length = mRectF.width();
                    break;
                case VERTICAL:
                    start = mRectF.top;
                    length = mRectF.height();
                    break;
            }

            mTriaAxis = start + length * mTriaAxisPercent;
        }


        switch (mOrientation) {
            case HORIZONTAL:
                drawHorizontal(canvas);
                break;
            case VERTICAL:
                drawVertical(canvas);
                break;
        }
    }

    private void drawHorizontal(Canvas canvas) {
        if (mTriaAxis < mRectF.left) {
            mTriaAxis = mRectF.left;
        } else if (mTriaAxis > mRectF.right) {
            mTriaAxis = mRectF.right;
        }

        canvas.drawRect(mRectF, mPaint);

        canvas.drawPath(getTopTriaPath(), mTriaWhitePaint);
        canvas.drawPath(getBottomTriaPath(), mTriaBlackPaint);
    }

    private void drawVertical(Canvas canvas) {
        if (mTriaAxis < mRectF.top) {
            mTriaAxis = mRectF.top;
        } else if (mTriaAxis > mRectF.bottom) {
            mTriaAxis = mRectF.bottom;
        }

        canvas.drawRect(mRectF, mPaint);

        canvas.drawPath(getLeftTriaPath(), mTriaWhitePaint);
        canvas.drawPath(getRightTriaPath(), mTriaBlackPaint);
    }

    private Path getTopTriaPath() {
        Path path = new Path();
        path.moveTo(mTriaAxis, mRectF.top + (mTriaHeight - mTriaOffset));
        path.lineTo(mTriaAxis + mTriaLength / 2, mRectF.top - mTriaOffset);
        path.lineTo(mTriaAxis - mTriaLength / 2, mRectF.top - mTriaOffset);
        path.close();

        return path;
    }

    private Path getBottomTriaPath() {
        Path path = new Path();
        path.moveTo(mTriaAxis, mRectF.bottom - (mTriaHeight - mTriaOffset));
        path.lineTo(mTriaAxis + mTriaLength / 2, mRectF.bottom + mTriaOffset);
        path.lineTo(mTriaAxis - mTriaLength / 2, mRectF.bottom + mTriaOffset);
        path.close();

        return path;
    }

    private Path getLeftTriaPath() {
        Path path = new Path();
        path.moveTo(mRectF.left + (mTriaHeight - mTriaOffset), mTriaAxis);
        path.lineTo(mRectF.left - mTriaOffset, mTriaAxis - mTriaLength / 2);
        path.lineTo(mRectF.left - mTriaOffset, mTriaAxis + mTriaLength / 2);
        path.close();

        return path;
    }

    private Path getRightTriaPath() {
        Path path = new Path();
        path.moveTo(mRectF.right - (mTriaHeight - mTriaOffset), mTriaAxis);
        path.lineTo(mRectF.right + mTriaOffset, mTriaAxis - mTriaLength / 2);
        path.lineTo(mRectF.right + mTriaOffset, mTriaAxis + mTriaLength / 2);
        path.close();

        return path;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (mOrientation) {
            case HORIZONTAL:
                mTriaAxis = event.getX();
                if (mTriaAxis < mRectF.left) {
                    mTriaAxis = mRectF.left;
                } else if (mTriaAxis > mRectF.right) {
                    mTriaAxis = mRectF.right;
                }
                mTriaAxisPercent = (mTriaAxis - mRectF.left) / mRectF.width();
                break;
            case VERTICAL:
                mTriaAxis = event.getY();
                if (mTriaAxis < mRectF.top) {
                    mTriaAxis = mRectF.top;
                } else if (mTriaAxis > mRectF.bottom) {
                    mTriaAxis = mRectF.bottom;
                }
                mTriaAxisPercent = (mTriaAxis - mRectF.top) / mRectF.height();
                break;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouched = true;
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChangeStart(this, getTouchPointColor(mTriaAxis));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChanged(this, getTouchPointColor(mTriaAxis));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouched = false;
                if (getOnColorChangedListener() != null) {
                    getOnColorChangedListener().onColorChangeEnd(this, getTouchPointColor(mTriaAxis));
                }
                break;
        }

        invalidate();

        return true;
    }

    private int getTouchPointColor(float axis) {
        switch (mOrientation) {
            case HORIZONTAL:
                return interceptColor(mColors, axis, mRectF.left, mRectF.right);
            case VERTICAL:
                return interceptColor(mColors, axis, mRectF.top, mRectF.bottom);
            default:
                return 0;
        }
    }
}
