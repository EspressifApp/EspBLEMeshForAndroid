package com.espressif.espblemesh.ui;

import android.Manifest;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.task.NetworkDeleteTask;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.blemesh.utils.MeshUtils;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.ui.network.NetworkActivity;
import com.espressif.espblemesh.ui.provisioning.ProvisionScanActivity;
import com.espressif.espblemesh.ui.settings.SettingsActivity;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import libs.espressif.app.PermissionHelper;
import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;

public class MainActivity extends ServiceActivity {
    private static final int REQUEST_PERMISSION = 0x11;
    private static final int REQUEST_PROVISION_SCAN = 0x12;
    private static final int REQUEST_NETWORK = 0x13;
    private static final int REQUEST_SETTINGS = 0x14;
    private static final int REQUEST_ADD_NETWORK = 0x15;

    private EspLog mLog = new EspLog(getClass());

    private MeshUser mUser;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mNetworkView;
    private List<Network> mNetworkList;
    private NetworkAdapter mNetworkAdapter;

    private ScanFastProvThread mScanFastProvThread;

    private DrawerLayout mDrawer;
    private Button mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, 0, 0);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        setTitle(R.string.main_title);

        mUser = MeshUser.Instance;

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.nav_add_network: {
                    Intent intent = new Intent(MainActivity.this, NetworkAddActivity.class);
                    startActivityForResult(intent, REQUEST_ADD_NETWORK);
                    return true;
                }
                case R.id.nav_provision: {
                    gotoProvision();
                    return true;
                }
                case R.id.nav_settings: {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivityForResult(intent, REQUEST_SETTINGS);
                    return true;
                }
            }

            return false;
        });

        mNetworkView = findViewById(R.id.recycler_view);
        mNetworkList = new ArrayList<>();
        mNetworkAdapter = new NetworkAdapter();

        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mRefreshLayout.setOnRefreshListener(this::refresh);
        mRefreshLayout.setEnabled(false);

        mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(v -> gotoProvision());

        PermissionHelper mPermissionHelper = new PermissionHelper(this, REQUEST_PERMISSION);
        mPermissionHelper.requestAuthorities(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
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
        mNetworkView.setAdapter(mNetworkAdapter);
        updateNetworkList();
    }

    @Override
    protected void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);

        mRefreshLayout.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        }

        switch (requestCode) {
            case REQUEST_PROVISION_SCAN: {
                if (resultCode == RESULT_OK) {
                    updateNetworkList();

                    boolean fastProv = data.getBooleanExtra(Constants.KEY_FAST_FROV, false);
                    long netKeyIndex = data.getLongExtra(Constants.KEY_NETWORK_INDEX, -1);
                    if (fastProv) {
                        mRefreshLayout.setRefreshing(true);
                        setActivityEnable(false);

                        String mac = data.getStringExtra(Constants.KEY_NODE_MAC);
                        Node node = mUser.getNodeForMac(mac);
                        Network network = mUser.getNetworkForKeyIndex(netKeyIndex);

                        stopScanFastProvThread();
                        mScanFastProvThread = new ScanFastProvThread();
                        mScanFastProvThread.node = node;
                        mScanFastProvThread.network = network;
                        mScanFastProvThread.start();
                    } else {
                        if (netKeyIndex > 0) {
                            gotoNetworkActivity(netKeyIndex);
                        }
                    }
                }
                return;
            }
            case REQUEST_ADD_NETWORK: {
                if (resultCode == RESULT_OK) {
                    updateNetworkList();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_provision) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void gotoProvision() {
        Intent intent = new Intent(this, ProvisionScanActivity.class);
        startActivityForResult(intent, REQUEST_PROVISION_SCAN);
    }

    private void gotoNetworkActivity(long networkKeyIndex) {
        Intent intent = new Intent(MainActivity.this, NetworkActivity.class);
        intent.putExtra(Constants.KEY_NETWORK_INDEX, networkKeyIndex);
        startActivityForResult(intent, REQUEST_NETWORK);
    }

    private void setActivityEnable(boolean enable) {
        mNetworkView.setEnabled(enable);
        mFab.setEnabled(enable);
        mDrawer.setDrawerLockMode(enable ? DrawerLayout.LOCK_MODE_UNDEFINED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void updateNetworkList() {
        mNetworkList.clear();
        mNetworkList.addAll(mUser.getNetworkList());
        Collections.sort(mNetworkList, (o1, o2) -> {
            Long keyIndex1 = o1.getKeyIndex();
            Long keyIndex2 = o2.getKeyIndex();
            return keyIndex1.compareTo(keyIndex2);
        });
        mNetworkAdapter.notifyDataSetChanged();
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
                        mNetworkAdapter.notifyDataSetChanged();
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
        ImageView infoIcon;

        NetHolder(View itemView) {
            super(itemView);

            text1 = itemView.findViewById(R.id.text);
            infoIcon = itemView.findViewById(R.id.info_icon);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            infoIcon.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == infoIcon) {
                showNetworkInfo();
            } else {
                gotoNetworkActivity(network.getKeyIndex());
            }
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

        void showNetworkInfo() {
            List<String> nodeMacList = network.getNodeMacList();
            int nodeTotalCount = nodeMacList.size();
            int nodeScanCount = 0;
            for (String mac : network.getNodeMacList()) {
                if (getService().hasScanResult(mac)) {
                    nodeScanCount++;
                }
            }
            String netkey = DataUtil.bigEndianBytesToHexString(network.getNetKey());

            String message = getString(R.string.main_network_info_message,
                    network.getName(), netkey, network.getKeyIndex(), nodeTotalCount, nodeScanCount);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.main_network_info_title)
                    .setMessage(message)
                    .show();
        }
    }

    private class NetworkAdapter extends RecyclerView.Adapter<NetHolder> {

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
