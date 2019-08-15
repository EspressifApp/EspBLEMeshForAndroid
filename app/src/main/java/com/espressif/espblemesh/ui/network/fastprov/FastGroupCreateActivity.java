package com.espressif.espblemesh.ui.network.fastprov;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;

import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.db.entity.FastGroupDB;
import com.espressif.blemesh.utils.MeshUtils;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.constants.Constants;

import libs.espressif.utils.TextUtils;

public class FastGroupCreateActivity extends BaseActivity {

    private EditText mGroupNameET;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fast_group_create_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mGroupNameET = findViewById(R.id.group_name);

        Button okBtn = findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(v -> {
            String name = mGroupNameET.getText().toString();
            if (TextUtils.isEmpty(name)) {
                mGroupNameET.setError(getString(R.string.group_name_error));
            } else {
                mGroupNameET.setError(null);

                long groupAddr = MeshUtils.generateGroupAddress();
                FastGroupDB db = new FastGroupDB();
                db.name = name;
                db.addr = groupAddr;
                long id = MeshObjectBox.getInstance().addOrUpdateFastGroup(db);
                Intent data = new Intent();
                data.putExtra(Constants.KEY_ID, id);
                data.putExtra(Constants.KEY_NAME, name);
                data.putExtra(Constants.KEY_GROUP_ADDRESS, groupAddr);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }
}
