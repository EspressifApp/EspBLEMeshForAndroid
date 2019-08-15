package libs.espressif.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public abstract class ColorPicker extends View {
    private OnColorChangedListener mOnColorChangedListener;

    public ColorPicker(Context context) {
        super(context);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OnColorChangedListener getOnColorChangedListener() {
        return mOnColorChangedListener;
    }

    public void setOnColorChangedListener(OnColorChangedListener onColorChangedListener) {
        mOnColorChangedListener = onColorChangedListener;
    }

    /**
     * Get the value of position p from s to d
     *
     * @param s
     * @param d
     * @param p
     * @return
     */
    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }

    /**
     * Get the color value of the axis
     *
     * @param colors
     * @param axis
     * @param minAxis
     * @param maxAxis
     * @return
     */
    protected int interceptColor(int[] colors, float axis, float minAxis, float maxAxis) {
        int result;

        if (axis <= minAxis) {
            result = colors[0];
        } else if (axis >= maxAxis) {
            result = colors[colors.length - 1];
        } else {
            float percent = (axis - minAxis) / maxAxis;
            float position = (colors.length - 1) * percent;
            int start = (int) position;
            int end = start + 1;
            int color0 = colors[start];
            int color1 = colors[end];

            int a = ave(Color.alpha(color0), Color.alpha(color1), position - start);
            int r = ave(Color.red(color0), Color.red(color1), position - start);
            int g = ave(Color.green(color0), Color.green(color1), position - start);
            int b = ave(Color.blue(color0), Color.blue(color1), position - start);

            result = Color.argb(a, r, g, b);
        }

        return result;
    }

    protected int getGradientColor(int sColor, int dColor, float percent) {
        int a = ave(Color.alpha(sColor), Color.alpha(dColor), percent);
        int r = ave(Color.red(sColor), Color.red(dColor), percent);
        int g = ave(Color.green(sColor), Color.green(dColor), percent);
        int b = ave(Color.blue(sColor), Color.blue(dColor), percent);
        return Color.argb(a, r, g, b);
    }

    public interface OnColorChangedListener {
        void onColorChangeStart(View v, int color);

        void onColorChanged(View v, int color);

        void onColorChangeEnd(View v, int color);
    }
}
