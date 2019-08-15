package com.espressif.espblemesh.ui.network.ota;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.blemesh.task.OTAListBinsTask;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.app.MeshApp;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.ota.OTAClient;
import com.espressif.espblemesh.ota.OTAUtils;
import com.espressif.espblemesh.ui.network.SimpleMessageCallback;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import libs.espressif.log.EspLog;

public class NodeOTAActivity extends BaseActivity {
    private final EspLog mLog = new EspLog(getClass());

    private View mBinForm;

    private EditText mSoftApSsidET;
    private EditText mSoftApPasswordET;

    private View mSoftConnForm;
    private TextView mSoftConnMsgTV;
    private Button mSoftApConntinueBtn;

    private RecyclerView mBinRecyclerView;
    private List<File> mBinList;
    private BinAdapter mBinAdapter;

    private OTAClient mOTAClient;
    private OTAPackage mOTAPackage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.node_ota_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mBinForm = findViewById(R.id.bin_form);

        mSoftApSsidET = findViewById(R.id.bin_ssid_text);
        mSoftApPasswordET = findViewById(R.id.bin_password_text);

        mSoftConnForm = findViewById(R.id.softap_connect_form);
        mSoftConnMsgTV = findViewById(R.id.softap_message_text);
        mSoftApConntinueBtn = findViewById(R.id.softap_continue_btn);
        mSoftApConntinueBtn.setOnClickListener(v -> {
            if (mOTAClient != null) {
                mSoftConnForm.setVisibility(View.GONE);
                mOTAClient.notifyWifiConnected();
            } else {
                mLog.w("OTAClient is null");
            }
        });

        mBinRecyclerView = findViewById(R.id.bin_recycler_view);
        mBinList = new OTAListBinsTask(MeshApp.getAppDirPath() + "ota/").run();
        mBinAdapter = new BinAdapter();
        mBinRecyclerView.setAdapter(mBinAdapter);

        String pkgKey = getIntent().getStringExtra(Constants.KEY_OTA_PACKAGE);
        mOTAPackage = (OTAPackage) MeshApp.getInstance().takeCache(pkgKey);
        mOTAPackage.gattCB = new GattCB();
        mOTAPackage.messageCB = new MessageCB();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mOTAClient != null) {
            mOTAClient.close();
            mOTAClient = null;
        }
        mOTAPackage.messager = null;
        mOTAPackage.gattCB = null;
        mOTAPackage.messageCB = null;
    }

    private void startOTA(File bin) {
        if (mOTAClient != null) {
            mOTAClient.close();
        }

        mSoftApSsidET.setError(null);
        mSoftApPasswordET.setError(null);


        String ssid = mSoftApSsidET.getText().toString();
        if (ssid.length() != 2) {
            mSoftApSsidET.setError(getString(R.string.node_ota_ssid_length_error));
            return;
        }
        String password = mSoftApPasswordET.getText().toString();
        if (password.length() != 2) {
            mSoftApPasswordET.setError(getString(R.string.node_ota_passowrd_length_error));
            return;
        }

        long nodeAddr = mOTAPackage.node.getUnicastAddress();
        String softApSSID = OTAUtils.getSoftApSSID(ssid, nodeAddr);
        String softApPassword = OTAUtils.getSoftApPassword(password, ssid, nodeAddr);

        mBinForm.setVisibility(View.GONE);
        mOTAClient = new OTAClient(this, mOTAPackage.node, mOTAPackage.messager, bin, ssid.getBytes(),
                password.getBytes());
        mOTAClient.setSoftAPCallback(new OTAClient.SoftAPCallback() {
            @Override
            public void onConnectedResult(boolean connected) {
                if (!connected) {
                    // TODO
                    mSoftConnForm.setVisibility(View.VISIBLE);
                    mSoftConnMsgTV.setText(getString(R.string.node_ota_softap_connect_message,
                            softApSSID, softApPassword));
                } else {
                    //
                    mSoftConnForm.setVisibility(View.GONE);
                }
            }
        });
        Observable.just(mOTAClient)
                .subscribeOn(Schedulers.io())
                .doOnNext(OTAClient::ota)
                .subscribe();
    }

    private class BinHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        File bin;
        TextView name;

        BinHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(android.R.id.text1);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            new AlertDialog.Builder(NodeOTAActivity.this)
                    .setTitle(bin.getName())
                    .setMessage(R.string.node_ota_bin_dialog)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        //
                        startOTA(bin);
                    })
                    .show();
        }
    }

    private class BinAdapter extends RecyclerView.Adapter<BinHolder> {

        @NonNull
        @Override
        public BinHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new BinHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull BinHolder holder, int position) {
            File bin = mBinList.get(position);
            holder.bin = bin;
            holder.name.setText(bin.getName());
        }

        @Override
        public int getItemCount() {
            return mBinList.size();
        }
    }

    private class GattCB extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }
    }

    private class MessageCB extends SimpleMessageCallback {
        @Override
        public void onNeedOTAUpdateNotification(byte[] manufacturerId, byte[] binId, byte[] version) {
            mLog.d("onNeedOTAUpdateNotification");
            if (mOTAClient != null) {
                mOTAClient.onReceiveNeedOTAUpdateNotification();
            }
        }

        @Override
        public void onOTAStartResponse() {
            mLog.d("onOTAStartResponse");
            if (mOTAClient != null) {
                mOTAClient.onReceiveOTAStartResponse();
            }
        }

        @Override
        public void onGattClosed() {
            // TODO
        }
    }
}
