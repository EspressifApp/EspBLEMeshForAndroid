package com.espressif.espblemesh.ui.network.node;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Node;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.app.MeshApp;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class OperationActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.operation_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        Intent intent = getIntent();
        MeshApp app = MeshApp.getInstance();
        Model model = (Model) app.takeCacheForIntentKey(intent, Constants.KEY_MODEL);
        Node node = (Node) app.takeCacheForIntentKey(intent, Constants.KEY_NODE);

        OperationFragment fragment;
        switch (model.getId()) {
            case MeshConstants.MODEL_ID_HSL:
                fragment = new OperationLightHSLFragment();
                break;
            case MeshConstants.MODEL_ID_CTL:
                fragment = new OperationLightCTLFragment();
                break;
            default:
                throw new IllegalArgumentException("Unsupported model");
        }

        fragment.setArgs(model, node);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_form, fragment).commit();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattConnectionEvent(GattConnectionEvent event) {
        if (!event.isConnected()) {
            finish();
        }
    }
}
