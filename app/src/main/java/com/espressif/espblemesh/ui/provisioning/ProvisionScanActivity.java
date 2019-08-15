package com.espressif.espblemesh.ui.provisioning;

import android.Manifest;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.utils.MeshUtils;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.blemesh.task.NodeDeleteTask;
import com.espressif.espblemesh.ui.ServiceActivity;
import com.espressif.espblemesh.utils.Utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import libs.espressif.app.PermissionHelper;
import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;
import libs.espressif.utils.TextUtils;

public class ProvisionScanActivity extends ServiceActivity {
    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_CONFIGURE = 2;

    private static final int MENU_FILTER = 0x10;

    private final EspLog mLog = new EspLog(getClass());

    private List<ScanResult> mBleList;
    private BleAdapter mBleAdapter;

    private SwipeRefreshLayout mRefreshLayout;

    private UpdateThread mUpdateThread;

    private int mFilterRssiMin = Integer.MIN_VALUE;
    private int mFilterRssiMax = Integer.MAX_VALUE;
    private String mFilterName;
    private String mFilterUUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provision_scan_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mBleList = new LinkedList<>();
        mBleAdapter = new BleAdapter();
        RecyclerView bleRecyclerView = findViewById(R.id.recycler_view);
        bleRecyclerView.setAdapter(mBleAdapter);

        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mRefreshLayout.setOnRefreshListener(this::refresh);
        mRefreshLayout.setEnabled(false);

        PermissionHelper mPermissionHelper = new PermissionHelper(this, REQUEST_PERMISSION);
        mPermissionHelper.requestAuthorities(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @Override
    protected void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        mUpdateThread = new UpdateThread();
        mUpdateThread.start();

        mRefreshLayout.setEnabled(true);
    }

    @Override
    protected void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);

        mUpdateThread.interrupt();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONFIGURE:
                if (resultCode == RESULT_OK) {
                    String nodeMac = data.getStringExtra(Constants.KEY_NODE_MAC);
                    getService().removeProvision(nodeMac);
                    setResult(RESULT_OK, data);
                    finish();
                }
                return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_FILTER, 0, R.string.provision_scan_menu_filter)
                .setIcon(R.drawable.ic_filter_list_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FILTER:
                showFilterDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateProvision(List<ScanResult> scanResults) {
        mBleList.clear();
        for (ScanResult scanResult : scanResults) {
            if (mFilterRssiMin != Integer.MIN_VALUE) {
                if (scanResult.getRssi() < mFilterRssiMin) {
                    continue;
                }
            }

            if (mFilterRssiMax != Integer.MAX_VALUE) {
                if (scanResult.getRssi() > mFilterRssiMax) {
                    continue;
                }
            }

            if (mFilterName != null) {
                String filterNameLC = mFilterName.toLowerCase();
                String deviceName = Utils.getBLEDeviceName(scanResult);
                if (deviceName == null || !deviceName.toLowerCase().contains(filterNameLC)) {
                    continue;
                }
            }

            if (mFilterUUID != null) {
                String filterUUIDLc = mFilterUUID.toLowerCase();
                ScanRecord record = scanResult.getScanRecord();
                assert record != null;
                byte[] devUUID = MeshUtils.getProvisioningUUID(record.getBytes());
                assert devUUID != null;
                String devUUIDStr = DataUtil.bigEndianBytesToHexString(devUUID).toLowerCase();
                if (!devUUIDStr.contains(filterUUIDLc)) {
                    continue;
                }
            }

            mBleList.add(scanResult);
        }
        mBleAdapter.notifyDataSetChanged();
    }

    private void showFilterDialog() {
        AlertDialog filterDailog = new AlertDialog.Builder(this)
                .setTitle(R.string.provision_scan_menu_filter)
                .setView(R.layout.provision_scan_filter_dialog)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    AlertDialog alertDialog = (AlertDialog) dialog;
                    CheckBox rssiMinCB = alertDialog.findViewById(R.id.filter_rssi_min_cb);
                    View rssiMinNeg = alertDialog.findViewById(R.id.filter_rssi_min_neg);
                    EditText rssiMinET = alertDialog.findViewById(R.id.filter_rssi_min_edit);
                    assert rssiMinCB != null && rssiMinNeg != null && rssiMinET != null;

                    CheckBox rssiMaxCB = alertDialog.findViewById(R.id.filter_rssi_max_cb);
                    View rssiMaxNeg = alertDialog.findViewById(R.id.filter_rssi_max_neg);
                    EditText rssiMaxET = alertDialog.findViewById(R.id.filter_rssi_max_edit);
                    assert rssiMaxCB != null && rssiMaxNeg != null && rssiMaxET != null;

                    CheckBox nameCB = alertDialog.findViewById(R.id.filter_name_cb);
                    EditText nameET = alertDialog.findViewById(R.id.filter_name_edit);
                    assert nameCB != null && nameET != null;

                    CheckBox uuidCB = alertDialog.findViewById(R.id.filter_uuid_cb);
                    EditText uuidET = alertDialog.findViewById(R.id.filter_uuid_edit);
                    assert uuidCB != null && uuidET != null;

                    if (rssiMinCB.isChecked()) {
                        String rssiMinText = rssiMinET.getText().toString();
                        if (TextUtils.isEmpty(rssiMinText)) {
                            mFilterRssiMin = Integer.MIN_VALUE;
                        } else {
                            mFilterRssiMin = -(Math.abs(Integer.parseInt(rssiMinText)));
                        }
                    } else {
                        mFilterRssiMin = Integer.MIN_VALUE;
                    }

                    if (rssiMaxCB.isChecked()) {
                        String rssiMaxText = rssiMaxET.getText().toString();
                        if (TextUtils.isEmpty(rssiMaxText)) {
                            mFilterRssiMax = Integer.MAX_VALUE;
                        } else {
                            mFilterRssiMax = -(Math.abs(Integer.parseInt(rssiMaxText)));
                        }
                    } else {
                        mFilterRssiMax = Integer.MAX_VALUE;
                    }

                    if (nameCB.isChecked()) {
                        String name = nameET.getText().toString();
                        if (TextUtils.isEmpty(name)) {
                            mFilterName = null;
                        } else {
                            mFilterName = name;
                        }
                    } else {
                        mFilterName = null;
                    }

                    if (uuidCB.isChecked()) {
                        String uuid = uuidET.getText().toString();
                        if (TextUtils.isEmpty(uuid)) {
                            mFilterUUID = null;
                        } else {
                            mFilterUUID = uuid;
                        }
                    } else {
                        mFilterUUID = null;
                    }
                })
                .show();

        CheckBox rssiMinCB = filterDailog.findViewById(R.id.filter_rssi_min_cb);
        View rssiMinNeg = filterDailog.findViewById(R.id.filter_rssi_min_neg);
        EditText rssiMinET = filterDailog.findViewById(R.id.filter_rssi_min_edit);
        assert rssiMinCB != null && rssiMinNeg != null && rssiMinET != null;

        CheckBox rssiMaxCB = filterDailog.findViewById(R.id.filter_rssi_max_cb);
        View rssiMaxNeg = filterDailog.findViewById(R.id.filter_rssi_max_neg);
        EditText rssiMaxET = filterDailog.findViewById(R.id.filter_rssi_max_edit);
        assert rssiMaxCB != null && rssiMaxNeg != null && rssiMaxET != null;

        CheckBox nameCB = filterDailog.findViewById(R.id.filter_name_cb);
        EditText nameET = filterDailog.findViewById(R.id.filter_name_edit);
        assert nameCB != null && nameET != null;

        CheckBox uuidCB = filterDailog.findViewById(R.id.filter_uuid_cb);
        EditText uuidET = filterDailog.findViewById(R.id.filter_uuid_edit);
        assert uuidCB != null && uuidET != null;

        CompoundButton.OnCheckedChangeListener checkListener = (buttonView, isChecked) -> {
            View[] stateViews = new View[0];
            if (buttonView == rssiMinCB) {
                stateViews = new View[]{rssiMinNeg, rssiMinET};
            } else if (buttonView == rssiMaxCB) {
                stateViews = new View[]{rssiMaxNeg, rssiMaxET};
            } else if (buttonView == nameCB) {
                stateViews = new View[]{nameET};
            } else if (buttonView == uuidCB) {
                stateViews = new View[]{uuidET};
            }

            for (View view : stateViews) {
                view.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        };
        rssiMinCB.setOnCheckedChangeListener(checkListener);
        rssiMaxCB.setOnCheckedChangeListener(checkListener);
        nameCB.setOnCheckedChangeListener(checkListener);
        uuidCB.setOnCheckedChangeListener(checkListener);
        rssiMinNeg.setVisibility(View.GONE);
        rssiMinET.setVisibility(View.GONE);
        rssiMaxNeg.setVisibility(View.GONE);
        rssiMaxET.setVisibility(View.GONE);
        nameET.setVisibility(View.GONE);
        uuidET.setVisibility(View.GONE);

        if (mFilterRssiMin != Integer.MIN_VALUE) {
            rssiMinCB.setChecked(true);
            rssiMinET.setText(String.valueOf(Math.abs(mFilterRssiMin)));
        }
        if (mFilterRssiMax != Integer.MAX_VALUE) {
            rssiMaxCB.setChecked(true);
            rssiMaxET.setText(String.valueOf(Math.abs(mFilterRssiMax)));
        }
        if (mFilterName != null) {
            nameCB.setChecked(true);
            nameET.setText(mFilterName);
        }
        if (mFilterUUID != null) {
            uuidCB.setChecked(true);
            uuidET.setText(mFilterUUID);
        }
    }

    private void refresh() {
        if (getService() != null) {
            getService().startScanBle();
        }
        Observable.just(300L)
                .subscribeOn(Schedulers.io())
                .doOnNext(Thread::sleep)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Long aLong) {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        if (!getService().isBTEnable()) {
                            Toast.makeText(ProvisionScanActivity.this,
                                    R.string.main_bt_disable_toast, Toast.LENGTH_SHORT)
                                    .show();
                        }

                        mRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private class UpdateThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    mLog.w("Scan UpdateThread is interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }

                if (isPaused()) {
                    continue;
                }

                List<ScanResult> scanResults = getService().getProvisionList();
                Collections.sort(scanResults, (o1, o2) -> {
                    String name1 = Utils.getBLEDeviceName(o1);
                    if (name1 == null) {
                        name1 = "";
                    }
                    String name2 = Utils.getBLEDeviceName(o2);
                    if (name2 == null) {
                        name2 = "";
                    }

                    int result = name1.compareTo(name2);

                    if (result == 0) {
                        String bssid1 = o1.getDevice().getAddress();
                        String bssid2 = o2.getDevice().getAddress();

                        result = bssid1.compareTo(bssid2);
                    }

                    return result;
                });

                MeshUser user = MeshUser.Instance;
                for (ScanResult scanResult : scanResults) {
                    String address = scanResult.getDevice().getAddress();
                    Node node = user.getNodeForMac(address);
                    if (node != null) {
                        new NodeDeleteTask(address).run();
                    }
                }

                runOnUiThread(() -> updateProvision(scanResults));
            }
        }
    }

    private class BleHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ScanResult scanResult;

        TextView text1;
        TextView text2;

        BleHolder(View itemView) {
            super(itemView);

            text1 = itemView.findViewById(android.R.id.text1);
            text1.setTextColor(Color.BLACK);
            text2 = itemView.findViewById(android.R.id.text2);
            text2.setTextColor(Color.GRAY);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            getService().stopScanBle();

            Intent intent = new Intent(ProvisionScanActivity.this, ProvisioningActivity.class);
            intent.putExtra(Constants.KEY_SCAN_RESULT, scanResult);
            startActivityForResult(intent, REQUEST_CONFIGURE);
        }
    }

    private class BleAdapter extends RecyclerView.Adapter<BleHolder> {

        @NonNull
        @Override
        public BleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new BleHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull BleHolder holder, int position) {
            ScanResult ble = mBleList.get(position);
            holder.scanResult = ble;

            String name = ble.getDevice().getName();
            if (name == null) {
                if (ble.getScanRecord() != null) {
                    name = ble.getScanRecord().getDeviceName();
                }
            }
            if (name == null) {
                name = "Unknow";
            }
            holder.text1.setText(name);
            holder.text2.setText(String.format(Locale.ENGLISH, "%s %d", ble.getDevice().getAddress(), ble.getRssi()));

        }

        @Override
        public int getItemCount() {
            return mBleList.size();
        }
    }
}
