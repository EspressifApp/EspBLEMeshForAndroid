package com.espressif.espblemesh.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.task.NetworkAddTask;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import libs.espressif.utils.DataUtil;
import libs.espressif.utils.TextUtils;

public class NetworkAddActivity extends BaseActivity {
    private List<Network> mNetworkList;
    private Set<String> mNetworkKeySet;

    private EditText mNetworkKeyET;
    private TextView mNetworkKeyHintTV;
    private Button mOKBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.network_add_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mNetworkList = MeshUser.Instance.getNetworkList();
        mNetworkKeySet = new HashSet<>();
        for (Network network : mNetworkList) {
            mNetworkKeySet.add(DataUtil.bigEndianBytesToHexString(network.getNetKey()));
        }

        mNetworkKeyET = findViewById(R.id.network_key_edit);
        mNetworkKeyHintTV = findViewById(R.id.network_key_hint_text);
        mOKBtn = findViewById(R.id.ok_btn);

        mNetworkKeyET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    mNetworkKeyHintTV.setText("");
                    mOKBtn.setEnabled(false);
                    return;
                }

                if (s.length() != 32) {
                    mNetworkKeyHintTV.setText(R.string.main_new_network_key_length_error);
                    mOKBtn.setEnabled(false);
                    return;
                }

                String keyStr = s.toString();
                if (mNetworkKeySet.contains(keyStr.toLowerCase())) {
                    mNetworkKeyHintTV.setText(R.string.main_new_network_key_duplicate_error);
                    mOKBtn.setEnabled(false);
                    return;
                }

                try {
                    new BigInteger(keyStr, 16);
                    mNetworkKeyHintTV.setText("");
                    mOKBtn.setEnabled(true);
                } catch (Exception e) {
                    mNetworkKeyHintTV.setText(R.string.main_new_network_key_format_error);
                    mOKBtn.setEnabled(false);
                }
            }
        });

        mOKBtn.setOnClickListener(v -> {
            String netKey = mNetworkKeyET.getText().toString();

            EditText nameET = findViewById(R.id.network_name_edit);
            String name = nameET.getText().toString();

            new NetworkAddTask(netKey, name).run();

            setResult(RESULT_OK);
            finish();
        });
    }
}
