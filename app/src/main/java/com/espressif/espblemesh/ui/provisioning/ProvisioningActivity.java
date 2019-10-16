package com.espressif.espblemesh.ui.provisioning;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.blemesh.client.IMeshMessager;
import com.espressif.blemesh.client.IMeshProvisioner;
import com.espressif.blemesh.client.MeshGattClient;
import com.espressif.blemesh.client.callback.MeshGattCallback;
import com.espressif.blemesh.client.callback.MessageCallback;
import com.espressif.blemesh.client.callback.ProvisioningCallback;
import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.custom.FastProvInfoSetMessage;
import com.espressif.blemesh.model.message.standard.AppKeyAddMessage;
import com.espressif.blemesh.model.message.standard.CompositionDataGetMessage;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.blemesh.utils.MeshUtils;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.ui.settings.SettingsActivity;
import com.espressif.espblemesh.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import libs.espressif.log.EspLog;
import libs.espressif.utils.TextUtils;

public class ProvisioningActivity extends BaseActivity {
    private final EspLog mLog = new EspLog(getClass());

    private MeshUser mUser;

    private ScanResult mScanResult;

    private MeshGattClient mMeshGattClient;
    private IMeshProvisioner mProvisioner;
    private IMeshMessager mMessager;
    private Node mNode;

    private App mApp;

    private View mProgressView;
    private Button mCancelBtn;

    private RecyclerView mRecyclerView;
    private MsgAdapter mMsgAdapter;
    private List<String> mMsgList;

    private View mConfigForm;
    private EditText mDeviceNameET;
    private CheckBox mFastProvCB;
    private View mFastProvForm;
    private EditText mFastProvCountET;
    private Spinner mNetworkSp;
    private List<Network> mNetworkList;
    private List<String> mNetNameList;
    private Button mOKBtn;

    private volatile boolean mProvResult = false;
    private volatile boolean mWillProv = false;

    private Network mNetwork;

    private TextView mHintTV;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provisioning_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mUser = MeshUser.Instance;
        long usedAppKeyIndex = SettingsActivity.getUsedAppKeyIndex(this);
        mApp = mUser.getAppForKeyIndex(usedAppKeyIndex);
        mScanResult = getIntent().getParcelableExtra(Constants.KEY_SCAN_RESULT);

        mProgressView = findViewById(R.id.progress);
        mCancelBtn = findViewById(R.id.cancel_btn);
        mCancelBtn.setOnClickListener(v -> {
            mWillProv = false;
            closeGatt();
            showProgress(false);
        });

        mRecyclerView = findViewById(R.id.recycler_view);
        mMsgList = new ArrayList<>();
        mMsgAdapter = new MsgAdapter();
        mRecyclerView.setAdapter(mMsgAdapter);

        mConfigForm = findViewById(R.id.config_form);

        mDeviceNameET = findViewById(R.id.config_device_name);
        mDeviceNameET.setText(Utils.getBLEDeviceName(mScanResult));

        mFastProvCB = findViewById(R.id.config_fast_prov_check);
        mFastProvForm = findViewById(R.id.config_fast_prov_form);
        mFastProvCountET = findViewById(R.id.config_fast_prov_count);
        mFastProvCB.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mFastProvForm.setVisibility(View.VISIBLE);
            } else {
                mFastProvForm.setVisibility(View.GONE);
            }
        });
        mFastProvCB.setChecked(false);

        mNetworkSp = findViewById(R.id.config_netwok);
        mNetworkList = mUser.getNetworkList();
        Collections.sort(mNetworkList, (o1, o2) -> {
            Long index1 = o1.getKeyIndex();
            Long index2 = o2.getKeyIndex();
            return index1.compareTo(index2);
        });
        mNetNameList = new ArrayList<>();
        for (Network network : mNetworkList) {
            mNetNameList.add(network.getName());
        }
        ArrayAdapter<String> netAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                mNetNameList);
        netAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNetworkSp.setAdapter(netAdapter);

        mOKBtn = findViewById(R.id.config_ok_btn);
        mOKBtn.setOnClickListener(v -> {
            mWillProv = true;
            connectGatt();
        });

        mHintTV = findViewById(R.id.hint_text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mWillProv = false;
        closeGatt();
    }

    private void updateMsg(String msg) {
        runOnUiThread(() -> {
            mMsgList.add(msg);
            mMsgAdapter.notifyItemInserted(mMsgList.size() - 1);
            mRecyclerView.scrollToPosition(mMsgList.size() - 1);
        });
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
            mCancelBtn.setVisibility(View.VISIBLE);

            mConfigForm.setVisibility(View.GONE);
        } else {
            mProgressView.setVisibility(View.GONE);
            mCancelBtn.setVisibility(View.GONE);

            mConfigForm.setVisibility(View.VISIBLE);
        }
    }

    private void connectGatt() {
        closeGatt();

        mHintTV.setText("");
        showProgress(true);

        mNetwork = mNetworkList.get(mNetworkSp.getSelectedItemPosition());

        mMeshGattClient = new MeshGattClient(mScanResult.getDevice());
        mMeshGattClient.setAppAddr(Constants.APP_ADDRESS_DEFAULT);
        mMeshGattClient.setDeviceUUID(MeshUtils.getProvisioningUUID(mScanResult.getScanRecord().getBytes()));
        mMeshGattClient.setGattCallback(new GattCallback());
        mMeshGattClient.setMeshCallback(new MeshCB());
        mMeshGattClient.connect(getApplicationContext());
    }

    private void closeGatt() {
        if (mMeshGattClient != null) {
            mMeshGattClient.close();
            mMeshGattClient = null;
        }
    }

    private class MsgHolder extends RecyclerView.ViewHolder {
        TextView text1;

        MsgHolder(View itemView) {
            super(itemView);

            text1 = itemView.findViewById(android.R.id.text1);
        }
    }

    private class MsgAdapter extends RecyclerView.Adapter<MsgHolder> {

        @NonNull
        @Override
        public MsgHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MsgHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MsgHolder holder, int position) {
            String msg = mMsgList.get(position);
            holder.text1.setText(msg);
        }

        @Override
        public int getItemCount() {
            return mMsgList.size();
        }
    }

    private class MeshCB extends MeshGattCallback {
        @Override
        public void onDiscoverDeviceServiceResult(int code, IMeshProvisioner provisioner) {
            super.onDiscoverDeviceServiceResult(code, provisioner);

            if (code == CODE_SUCCESS) {
                mProvisioner = provisioner;
                String nodeName = mDeviceNameET.getText().toString();
                Network network = mNetworkList.get(mNetworkSp.getSelectedItemPosition());
                mProvisioner.setProvisioningCallback(new ProvisioningCB());
                mProvisioner.provisioning(nodeName, network);
            } else {
                if (!mProvResult) {
                    updateMsg("Discover device service failed");
                }
            }
        }

        @Override
        public void onDiscoverNodeServiceResult(int code, IMeshMessager messager) {
            super.onDiscoverNodeServiceResult(code, messager);

            mLog.d("onDiscoverNodeServiceResult " + code);
            if (code == CODE_SUCCESS) {
                mMessager = messager;
                mMessager.setNetwork(mUser.getNetworkForKeyIndex(mNetwork.getKeyIndex()));
                mMessager.setMessageCallback(new MessageCB());

                mLog.d("Request to add AppKey");
                updateMsg("Request to add AppKey");
                AppKeyAddMessage messageAppKeyAdd = new AppKeyAddMessage(mNode, mApp.getAppKey(), mApp.getKeyIndex(),
                        mMessager.getNetwork().getKeyIndex());
                messageAppKeyAdd.setPostCount(1);
                mMessager.appKeyAdd(messageAppKeyAdd);
//                updateMsg("Request to ge CompositionData");
//                MessageCompositionDataGet message = new MessageCompositionDataGet(mNode, 0);
//                message.setPostCount(1);
//                mMessager.compositionDataGet(message);
            } else {
                if (mProvResult) {
                    // Provisioning already complete, but discover node service failed
                    mLog.w("Discover node service failed");
                    Intent intent = new Intent();
                    intent.putExtra(Constants.KEY_NODE_MAC, mScanResult.getDevice().getAddress());
                    intent.putExtra(Constants.KEY_FAST_FROV, false);
                    intent.putExtra(Constants.KEY_NETWORK_INDEX, mMessager.getNetwork().getKeyIndex());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }
    }

    private class MessageCB extends MessageCallback {
        @Override
        public void onAppKeyStatus(int status, long netKeyIndex, long appKeyIndex) {
            mLog.d("onAppKeyStatus " + status);
            updateMsg("Add App key status is " + status);

            mLog.d("Request to ge CompositionData");
            updateMsg("Request to ge CompositionData");
            CompositionDataGetMessage message = new CompositionDataGetMessage(mNode, 0);
            message.setPostCount(1);
            mMessager.compositionDataGet(message);
        }

        @Override
        public void onFastProvStatus() {
            super.onFastProvStatus();

            mLog.d("onFastProvStatus");
            Intent intent = new Intent();
            intent.putExtra(Constants.KEY_NODE_MAC, mScanResult.getDevice().getAddress());
            intent.putExtra(Constants.KEY_FAST_FROV, true);
            intent.putExtra(Constants.KEY_NETWORK_INDEX, mMessager.getNetwork().getKeyIndex());
            setResult(RESULT_OK, intent);
            finish();
        }

        @Override
        public void onCompositionDataStatus(int status, int page) {
            super.onCompositionDataStatus(status, page);

            mLog.d("onCompositionDataStatus " + status);
            updateMsg("Get composition data status is " + status);
            boolean willFastProv = mFastProvCB.isChecked();
            if (willFastProv) {
                mLog.d("Request to FastProv");
                int provCount = TextUtils.isEmpty(mFastProvCountET.getText()) ? 100 :
                        Integer.parseInt(mFastProvCountET.getText().toString());
                byte[] devUUID = mMessager.getDeviceUUID();
                FastProvInfoSetMessage message = new FastProvInfoSetMessage(mNode, mApp);
                message.setProvCount(provCount);
                message.setUnicastAddressMin(0x0400L);
                message.setPrimaryProvisionerAddress(mApp.getUnicastAddr());
                message.setMatchValue(new byte[]{devUUID[0], devUUID[1]});
                message.setGroupAddress(MeshConstants.ADDRESS_GROUP_MIN);
                message.setAction((1 << 7) | 1);
                mMessager.fastProvInfoSet(message);
            } else {
                mLog.d("finish");
                Intent intent = new Intent();
                intent.putExtra(Constants.KEY_NODE_MAC, mScanResult.getDevice().getAddress());
                intent.putExtra(Constants.KEY_FAST_FROV, false);
                intent.putExtra(Constants.KEY_NETWORK_INDEX, mMessager.getNetwork().getKeyIndex());
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    private class ProvisioningCB extends ProvisioningCallback {
        @Override
        public void onProvisioningFailed(int code) {
            super.onProvisioningFailed(code);

            mProvResult = false;
            runOnUiThread(() -> {
                showProgress(false);
                if (code == -10) {
                    mHintTV.setText(R.string.provisioning_bluetooth_hint);
                }
            });

            mLog.w("onProvisioningFailed " + code);
            updateMsg("onProvisioningFailed: " + code);
        }

        @Override
        public void onProvisioningSuccess(int code, Node node) {
            super.onProvisioningSuccess(code, node);

            mProvResult = true;
            Observable.just(node)
                    .subscribeOn(Schedulers.io())
                    .doOnNext(n -> mUser.reload())
                    .doOnNext(n -> Thread.sleep(500))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(n -> {
                        String msg = "Provisioning success";
                        mLog.d(msg);
                        updateMsg(msg);

                        mNode = mUser.getNodeForMac(n.getMac());

                        mProvisioner.release();
                        mProvisioner = null;

                        setResult(RESULT_OK, new Intent());

                        mLog.d("Discover node service");
                        mMeshGattClient.discoverGattServices();
                    })
                    .subscribe();
        }
    }

    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED: {
                        String msg = "Gatt connected";
                        mLog.d(msg);
                        updateMsg(msg);
                        break;
                    }
                    case BluetoothProfile.STATE_CONNECTING: {
                        String msg = "Gatt connecting";
                        mLog.d(msg);
                        updateMsg(msg);
                        break;
                    }
                    case BluetoothProfile.STATE_DISCONNECTING: {
                        String msg = "Gatt disconnecting";
                        mLog.d(msg);
                        updateMsg(msg);
                        break;
                    }
                    case BluetoothProfile.STATE_DISCONNECTED:
                        runOnUiThread(() -> showProgress(false));

                        String msg = "Gatt disconnected";
                        mLog.d(msg);
                        updateMsg(msg);
                        break;
                }
            } else {
                String msg = "Gatt status error " + status;
                mLog.w(msg);
                updateMsg(msg);

                runOnUiThread(() -> {
                    showProgress(false);
                    if (status == 133 && mWillProv) {
                        connectGatt();
                    }
                });
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            String msg = String.format(Locale.ENGLISH, "Set mtu %d %s",
                    mtu, status == BluetoothGatt.GATT_SUCCESS ? "success" : "failed");
            mLog.d(msg);
            updateMsg(msg);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String msg = "Write descriptor success";
                mLog.d(msg);
                updateMsg(msg);
            } else {
                String msg = "Write descriptor failed";
                mLog.w(msg);
                updateMsg(msg);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                String msg = "Write characteristic failed";
                mLog.w(msg);
                updateMsg(msg);
            }
        }
    }
}
