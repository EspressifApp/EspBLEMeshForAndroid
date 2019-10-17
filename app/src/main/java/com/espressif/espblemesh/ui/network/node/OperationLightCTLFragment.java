package com.espressif.espblemesh.ui.network.node;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.espressif.espblemesh.R;

import libs.espressif.utils.TextUtils;

public class OperationLightCTLFragment extends OperationFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.operation_light_ctl_content, container, false);
        EditText lightnessET = view.findViewById(R.id.lightness_edit);
        EditText temperatureET = view.findViewById(R.id.temperature_edit);
        EditText deltaUVET = view.findViewById(R.id.delta_uv_edit);
        Button okBtn = view.findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(v -> {
            String lightnessStr = lightnessET.getText().toString();
            if (TextUtils.isEmpty(lightnessStr)) {
                return;
            }
            String temperatureStr = temperatureET.getText().toString();
            if (TextUtils.isEmpty(temperatureStr)) {
                return;
            }
            String deltaUVStr = deltaUVET.getText().toString();
            if (TextUtils.isEmpty(deltaUVStr)) {
                return;
            }

            v.setEnabled(false);
            int lightness = Integer.parseInt(lightnessStr);
            int temperature = Integer.parseInt(temperatureStr);
            int deltaUV = Integer.parseInt(deltaUVStr);
            mMeshConnection.setLightCTL(lightness, temperature, deltaUV, mNode, mDstAddress);
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle("Light CTL");
    }
}
