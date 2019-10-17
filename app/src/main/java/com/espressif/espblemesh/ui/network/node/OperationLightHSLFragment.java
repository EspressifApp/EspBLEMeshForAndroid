package com.espressif.espblemesh.ui.network.node;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.espressif.espblemesh.R;
import com.espressif.espblemesh.eventbus.blemesh.LightHSLEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import libs.espressif.utils.EspColorUtil;
import libs.espressif.widget.CircleColorPicker;
import libs.espressif.widget.ColorPicker;
import libs.espressif.widget.LinearColorPicker;

public class OperationLightHSLFragment extends OperationFragment {

    private View mColorDisplay;
    private CircleColorPicker mCircleCP;
    private LinearColorPicker mLinearCP;
    private int[] mLinearColors;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.operation_light_hsl_content, container, false);

        mColorDisplay = view.findViewById(R.id.color_display);
        mCircleCP = view.findViewById(R.id.color_picker1);
        mLinearCP = view.findViewById(R.id.color_picker2);
        mLinearColors = new int[]{Color.BLACK, 0xff808080, Color.WHITE};
        mLinearCP.setColors(mLinearColors);
        mLinearCP.setPositions(new float[]{0f, 0.5f, 1f});
        mLinearCP.setTriaAxisPercent(0.5f);
        mCircleCP.setOnColorChangedListener(mColorChangedListener);
        mLinearCP.setOnColorChangedListener(mColorChangedListener);

        mMeshConnection.getLightHSL(mNode, mDstAddress);

        EventBus.getDefault().register(this);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle("Light HSL");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);
    }

    private ColorPicker.OnColorChangedListener mColorChangedListener = new ColorPicker.OnColorChangedListener() {
        @Override
        public void onColorChangeStart(View v, int color) {
            mColorDisplay.setBackgroundColor(color);
            if (v == mCircleCP) {
                mLinearCP.setColors(new int[]{Color.BLACK, color, Color.WHITE});
            }
        }

        @Override
        public void onColorChanged(View v, int color) {
            mColorDisplay.setBackgroundColor(color);
            if (v == mCircleCP) {
                mLinearColors[1] = color;
                mLinearCP.setColors(new int[]{Color.BLACK, color, Color.WHITE});
            }
        }

        @Override
        public void onColorChangeEnd(View v, int color) {
            mColorDisplay.setBackgroundColor(color);
            if (v == mCircleCP) {
                mLinearColors[1] = color;
                mLinearCP.setColors(new int[]{Color.BLACK, color, Color.WHITE});
            }

            mMeshConnection.setLightHSL(color, mNode, mDstAddress);
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLightHSLEvent(LightHSLEvent event) {
        int[] rgb = EspColorUtil.HSLToRGB(event.getHue() / 360.0, event.getSaturation(), event.getLightness());
        int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
        mColorDisplay.setBackgroundColor(color);
    }
}
