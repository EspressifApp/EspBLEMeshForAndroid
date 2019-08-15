package com.espressif.espblemesh.ui;

import android.Manifest;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.utils.MeshUtils;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.blemesh.task.NetworkAddTask;
import com.espressif.blemesh.task.NetworkDeleteTask;
import com.espressif.espblemesh.ui.network.NetworkActivity;
import com.espressif.espblemesh.ui.provisioning.ProvisionScanActivity;
import com.espressif.espblemesh.ui.settings.SettingsActivity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import libs.espressif.app.PermissionHelper;
import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;
import libs.espressif.utils.TextUtils;

public class MainActivity extends ServiceActivity {
    private static final int MENU_GROUP_ID = 0x10;

    private static final int MENU_PROVISION_SCAN = 0x01;
    private static final int MENU_SETTINGS = 0x03;

    private static final int REQUEST_PERMISSION = 0x11;
    private static final int REQUEST_PROVISION_SCAN = 0x12;
    private static final int REQUEST_NETWORK = 0x13;

    private EspLog mLog = new EspLog(getClass());

    private MeshUser mUser;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mNetworkView;
    private List<Network> mNetworkList;
    private NetAdapter mNetAdapter;

    private Menu mMenu;
    private ScanFastProvThread mScanFastProvThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUser = MeshUser.Instance;

        mNetworkView = findViewById(R.id.recycler_view);
        mNetworkList = new ArrayList<>();
        mNetAdapter = new NetAdapter();

        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mRefreshLayout.setOnRefreshListener(this::refresh);
        mRefreshLayout.setEnabled(false);

        findViewById(R.id.fab).setOnClickListener(v -> showNewNetworkDialog());

        PermissionHelper mPermissionHelper = new PermissionHelper(this, REQUEST_PERMISSION);
        mPermissionHelper.requestAuthorities(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopScanFastProvThread();
    }

    @Override
    protected void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        mRefreshLayout.setEnabled(true);
        mNetworkView.setAdapter(mNetAdapter);
        updateNetworkList();
    }

    @Override
    protected void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);

        mRefreshLayout.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        menu.add(MENU_GROUP_ID, MENU_PROVISION_SCAN, 0, R.string.main_menu_provision_scan);
        menu.add(MENU_GROUP_ID, MENU_SETTINGS, 10, R.string.main_menu_settings);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PROVISION_SCAN: {
                Intent intent = new Intent(this, ProvisionScanActivity.class);
                startActivityForResult(intent, REQUEST_PROVISION_SCAN);
                return true;
            }
            case MENU_SETTINGS: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PROVISION_SCAN:
                if (resultCode == RESULT_OK) {
                    updateNetworkList();

                    boolean fastProv = data.getBooleanExtra(Constants.KEY_FAST_FROV, false);
                    if (fastProv) {
                        mRefreshLayout.setRefreshing(true);
                        setActivityEnable(false);

                        String mac = data.getStringExtra(Constants.KEY_NODE_MAC);
                        Node node = mUser.getNodeForMac(mac);
                        long netKeyIndex = data.getLongExtra(Constants.KEY_NETWORK_INDEX, -1);
                        Network network = mUser.getNetworkForKeyIndex(netKeyIndex);

                        stopScanFastProvThread();
                        mScanFastProvThread = new ScanFastProvThread();
                        mScanFastProvThread.node = node;
                        mScanFastProvThread.network = network;
                        mScanFastProvThread.start();
                    }
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setActivityEnable(boolean enable) {
        mNetworkView.setEnabled(enable);
        mMenu.setGroupEnabled(MENU_GROUP_ID, enable);
    }

    private void updateNetworkList() {
        mNetworkList.clear();
        mNetworkList.addAll(mUser.getNetworkList());
        Collections.sort(mNetworkList, (o1, o2) -> {
            Long keyIndex1 = o1.getKeyIndex();
            Long keyIndex2 = o2.getKeyIndex();
            return keyIndex1.compareTo(keyIndex2);
        });
        mNetAdapter.notifyDataSetChanged();
    }

    private void refresh() {
        getService().startScanBle();
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
                            Toast.makeText(MainActivity.this, R.string.main_bt_disable_toast, Toast.LENGTH_SHORT)
                                    .show();
                        }

                        mRefreshLayout.setRefreshing(false);
                        mNetAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void stopScanFastProvThread() {
        if (mScanFastProvThread != null) {
            mScanFastProvThread.interrupt();
            mScanFastProvThread = null;

            mRefreshLayout.setRefreshing(false);
            setActivityEnable(true);
        }
    }

    private void showNewNetworkDialog() {
        Set<String> netkeySet = new HashSet<>();
        for (Network network : mNetworkList) {
            netkeySet.add(DataUtil.bigEndianBytesToHexString(network.getNetKey()));
        }
        AlertDialog newDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.main_menu_new_network)
                .setView(R.layout.main_new_network_dialog)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    EditText keyET = ((AlertDialog) dialog).findViewById(R.id.network_key_edit);
                    assert keyET != null;
                    String netKey = keyET.getText().toString();

                    EditText nameET = ((AlertDialog) dialog).findViewById(R.id.network_name_edit);
                    assert nameET != null;
                    String name = nameET.getText().toString();

                    new NetworkAddTask(netKey, name).run();
                    updateNetworkList();
                })
                .show();
        EditText netKeyET = newDialog.findViewById(R.id.network_key_edit);
        TextView netKeyHintTV = newDialog.findViewById(R.id.network_key_hint_text);
        Button okBtn = newDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        okBtn.setEnabled(false);
        assert netKeyET != null && netKeyHintTV != null;
        netKeyET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    netKeyHintTV.setText("");
                    okBtn.setEnabled(false);
                    return;
                }

                if (s.length() != 32) {
                    netKeyHintTV.setText(R.string.main_new_network_key_length_error);
                    okBtn.setEnabled(false);
                    return;
                }

                String keyStr = s.toString();
                if (netkeySet.contains(keyStr.toLowerCase())) {
                    netKeyHintTV.setText(R.string.main_new_network_key_duplicate_error);
                    okBtn.setEnabled(false);
                    return;
                }

                try {
                    new BigInteger(keyStr, 16);
                    netKeyHintTV.setText("");
                    okBtn.setEnabled(true);
                } catch (Exception e) {
                    netKeyHintTV.setText(R.string.main_new_network_key_format_error);
                    okBtn.setEnabled(false);
                }
            }
        });
    }

    private void showDeleteNetworkDialog(long netKeyIndex) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.main_delete_network_dialog_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    new NetworkDeleteTask(netKeyIndex).run();
                    updateNetworkList();
                })
                .show();
    }

    private class NetHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        static final int MENU_NET_DELETE = 0x11;

        Network network;

        TextView text1;
        TextView text2;

        NetHolder(View itemView) {
            super(itemView);

            text1 = itemView.findViewById(android.R.id.text1);
            text1.setTextColor(Color.BLACK);
            text2 = itemView.findViewById(android.R.id.text2);
            text2.setTextColor(Color.GRAY);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, NetworkActivity.class);
            intent.putExtra(Constants.KEY_NETWORK_INDEX, network.getKeyIndex());
            startActivityForResult(intent, REQUEST_NETWORK);
        }

        @Override
        public boolean onLongClick(View v) {
            if (network.getKeyIndex() == Constants.NET_KEY_INDEX_DEFAULT) {
                return true;
            }

            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            Menu menu = popupMenu.getMenu();
            menu.add(Menu.NONE, MENU_NET_DELETE, 0, R.string.main_network_menu_delete);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case MENU_NET_DELETE:
                        showDeleteNetworkDialog(network.getKeyIndex());
                        return true;
                }
                return false;
            });
            popupMenu.show();

            return true;
        }
    }

    private class NetAdapter extends RecyclerView.Adapter<NetHolder> {

        @NonNull
        @Override
        public NetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.main_network_item, parent, false);
            return new NetHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull NetHolder holder, int position) {
            Network network = mNetworkList.get(position);
            holder.network = network;

            holder.text1.setText(network.getName());
            List<String> nodeMacList = network.getNodeMacList();
            int nodeTotalCount = nodeMacList.size();
            int nodeScanCount = 0;
            for (String mac : network.getNodeMacList()) {
                if (getService().hasScanResult(mac)) {
                    nodeScanCount++;
                }
            }
            holder.text1.append(String.format(Locale.ENGLISH, " (%d / %d)", nodeScanCount, nodeTotalCount));

            String netkey = DataUtil.bigEndianBytesToHexString(network.getNetKey());
            holder.text2.setText(netkey);
        }

        @Override
        public int getItemCount() {
            return mNetworkList.size();
        }
    }

    private class ScanFastProvThread extends Thread {
        Node node;
        Network network;

        boolean stop = false;

        @Override
        public void run() {
            mLog.d("ScanFastProvThread start");
            execute();
            mLog.d("ScanFastProvThread end");
        }

        @Override
        public void interrupt() {
            super.interrupt();
            stop = true;
        }

        private void execute() {
            while (!stop) {
                List<ScanResult> nodeList = getService().getNodeScanList();
                for (ScanResult sr : nodeList) {
                    assert sr.getScanRecord() != null;
                    byte[] scanRecord = sr.getScanRecord().getBytes();
                    if (!MeshUtils.isMeshNodeIdentity(scanRecord)) {
                        continue;
                    }

                    byte[][] hashRnd = MeshUtils.getNodeHashAndRandom(scanRecord);
                    assert hashRnd != null;
                    byte[] hash = hashRnd[0];
                    byte[] random = hashRnd[1];

                    byte[] calcHash = MeshUtils.calcNodeHash(network.getIdentityKey(), random, node.getUnicastAddress());

                    if (DataUtil.equleBytes(hash, calcHash)) {
                        runOnUiThread(() -> {
                            stopScanFastProvThread();

                            Intent intent = new Intent(MainActivity.this, NetworkActivity.class);
                            intent.putExtra(Constants.KEY_NETWORK_INDEX, network.getKeyIndex());
                            intent.putExtra(Constants.KEY_SCAN_RESULT, sr);
                            startActivityForResult(intent, REQUEST_NETWORK);
                        });
                        return;
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    mLog.w("ScanFastProvThread is interrupted");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
