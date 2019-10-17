package com.espressif.espblemesh.ui.network;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.espressif.blemesh.client.callback.MeshGattCallback;
import com.espressif.blemesh.model.App;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.task.GroupDeleteTask;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.GattCloseEvent;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;
import com.espressif.espblemesh.eventbus.GattNodeServiceEvent;
import com.espressif.espblemesh.eventbus.blemesh.FastProvAddrEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelSubscriptionEvent;
import com.espressif.espblemesh.model.MeshConnection;
import com.espressif.espblemesh.ui.ServiceActivity;
import com.espressif.espblemesh.ui.network.fastprov.FastProvedActivity;
import com.espressif.espblemesh.ui.network.node.NodeActivity;
import com.espressif.espblemesh.ui.settings.SettingsActivity;
import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.github.rubensousa.floatingtoolbar.FloatingToolbarMenuBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import libs.espressif.log.EspLog;

public class NetworkActivity extends ServiceActivity {
    private static final int MENU_GROUP_ALL = 0x00;
    private static final int MENU_GROUP_GROUP = 0x01;
    private static final int MENU_GROUP_FAST_PROV = 0x02;

    private static final int MENU_ID_DISCONNECT_BLE = 0x100;
    private static final int MENU_ID_GROUP_ADD = 0x110;
    private static final int MENU_ID_GROUP_DELETE = 0x111;
    private static final int MENU_ID_FAST_PROVED = 0x120;
    private static final int MENU_ID_HIDE_BOTTOM = 0x300;

    private static final int REQUEST_GROUP_ADD = 0x200;
    public static final int REQUEST_NODE = 0x201;
    public static final int REQUEST_OTA = 0x202;

    private final EspLog mLog = new EspLog(getClass());

    private Network mNetwork;
    private List<Group> mGroupList;
    private List<NetworkGroupFragment> mFragments;

    private MeshUser mUser;
    private App mApp;
    private MeshConnection mMeshConnection;

    private View mProgressView;
    private View mContentForm;

    private ViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;

    private Menu mMenu;

    private ScanResult mFastScanResult;

    FloatingToolbar mFloatingToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.network_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mUser = MeshUser.Instance;
        long usedAppKeyIndex = SettingsActivity.getUsedAppKeyIndex(this);
        mApp = mUser.getAppForKeyIndex(usedAppKeyIndex);

        long netKeyIndex = getIntent().getLongExtra(Constants.KEY_NETWORK_INDEX, -1);
        mNetwork = mUser.getNetworkForKeyIndex(netKeyIndex);
        assert mNetwork != null;
        setTitle(mNetwork.getName());

        int messagePostCount = SettingsActivity.getMessagePostCount(getApplicationContext());
        mMeshConnection = MeshConnection.Instance;
        mMeshConnection.setApp(mApp);
        mMeshConnection.setNetwork(mNetwork);
        mMeshConnection.setMessagePostCount(messagePostCount);

        FloatingActionButton fab = findViewById(R.id.fab);
        mFloatingToolbar = findViewById(R.id.floatingToolbar);
        mFloatingToolbar.attachFab(fab);
        mFloatingToolbar.attachAppBarLayout(findViewById(R.id.appBarLayout));
        fab.setOnClickListener(v -> mFloatingToolbar.show());
        mFloatingToolbar.setMenu(new FloatingToolbarMenuBuilder(this)
                .addItem(MENU_ID_GROUP_ADD, R.drawable.ic_add_circle_outline_24dp, R.string.network_menu_group_add)
                .addItem(MENU_ID_GROUP_DELETE, R.drawable.ic_delete_24dp, R.string.network_menu_group_delete)
                .addItem(MENU_ID_FAST_PROVED, R.drawable.baseline_grain_24, R.string.network_menu_fast_proved)
                .addItem(MENU_ID_HIDE_BOTTOM, R.drawable.baseline_unfold_less_24, R.string.network_menu_hide_bottombar)
                .build());
        mFloatingToolbar.setClickListener(new FloatingToolbar.ItemClickListener() {
            @Override
            public void onItemClick(MenuItem item) {
                NetworkActivity.this.onMenuItemSelected(item);
            }

            @Override
            public void onItemLongClick(MenuItem item) {
                Toast.makeText(NetworkActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });

        mGroupList = new ArrayList<>();
        mFragments = new ArrayList<>();
        mGroupList.add(null);
        NetworkGroupFragment fragment = new NetworkGroupFragment();
        fragment.setNetwork(mNetwork);
        mFragments.add(fragment);
        List<Long> netGroupAddrList = mNetwork.getGroupAddressList();
        Collections.sort(netGroupAddrList, Long::compareTo);
        for (long groupAddr : netGroupAddrList) {
            Group group = mUser.getGroupForAddress(groupAddr);
            addGroupPagerItem(group);
        }

        mProgressView = findViewById(R.id.progress);
        mContentForm = findViewById(R.id.content_form);

        mViewPager = findViewById(R.id.view_pager);
        mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        mViewPager.setAdapter(mViewPagerAdapter);

        mFastScanResult = getIntent().getParcelableExtra(Constants.KEY_SCAN_RESULT);
        if (mFastScanResult != null) {
            mLog.i("Find Fast ScanResult, try connecting");
            connectNode(mFastScanResult);
        }
    }

    @Override
    protected boolean willAutoScan() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnectNode();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;

        menu.add(MENU_GROUP_ALL, MENU_ID_DISCONNECT_BLE, 0, R.string.network_menu_disconnect_ble)
                .setIcon(R.drawable.ic_bluetooth_disabled_24dp)
                .setEnabled(false)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    private boolean onMenuItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_DISCONNECT_BLE: {
                disconnectNode();
                EventBus.getDefault().post(new GattCloseEvent());
                return true;
            }
            case MENU_ID_GROUP_ADD: {
                Intent intent = new Intent(this, NetworkNewGroupActivity.class);
                intent.putExtra(Constants.KEY_NETWORK_INDEX, mNetwork.getKeyIndex());
                startActivityForResult(intent, REQUEST_GROUP_ADD);
                return true;
            }
            case MENU_ID_GROUP_DELETE: {
                if (mViewPager.getCurrentItem() == 0) {
                    Toast.makeText(this, R.string.network_group_delete_all_message, Toast.LENGTH_SHORT).show();
                } else {
                    showGroupDeleteDialog();
                }
                return true;
            }
            case MENU_ID_FAST_PROVED: {
                Intent intent = new Intent(this, FastProvedActivity.class);
                startActivity(intent);
                return true;
            }
            case MENU_ID_HIDE_BOTTOM: {
                mFloatingToolbar.hide();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onMenuItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GROUP_ADD: {
                if (resultCode == RESULT_OK) {
                    long groupAddress = data.getLongExtra(Constants.KEY_GROUP_ADDRESS, -1);
                    Group group = mUser.getGroupForAddress(groupAddress);
                    mLog.i("Group name = " + group.getName());
                    mGroupList.add(group);
                    NetworkGroupFragment fragment = new NetworkGroupFragment();
                    fragment.setGroup(group);
                    mFragments.add(fragment);
                    mViewPagerAdapter.notifyDataSetChanged();
                }
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    App getApp() {
        return mApp;
    }

    void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
            mContentForm.setVisibility(View.GONE);
        } else {
            mProgressView.setVisibility(View.GONE);
            mContentForm.setVisibility(View.VISIBLE);
        }
    }

    void attachInFloatingToolbar(RecyclerView recyclerView) {
        if (mFloatingToolbar != null) {
            mFloatingToolbar.attachRecyclerView(recyclerView);
        }
    }

    @Subscribe
    public void onDiscoverNodeService(GattNodeServiceEvent event) {
        if (event.getCode() == MeshGattCallback.CODE_SUCCESS) {
            Node connectedNode = mUser.getNodeForMac(mMeshConnection.getConnectedAddress());
            if (connectedNode != null) {
                if (mFastScanResult == null) {
                    mMeshConnection.appKeyAdd(connectedNode);
                } else {
                    mMeshConnection.fastProvNodeAddrGet(connectedNode);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattConnectionEvent(GattConnectionEvent event) {
        if (event.getStatus() == BluetoothGatt.GATT_SUCCESS) {
            switch (event.getState()) {
                case BluetoothGatt.STATE_CONNECTED:
                    mMenu.setGroupEnabled(MENU_GROUP_ALL, true);
                    showProgress(false);
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    showProgress(false);
                    break;
            }
        } else {
            showProgress(false);
        }

        if (!mMeshConnection.isConnected()) {
            Toast.makeText(this, R.string.network_ble_disconnect_toast, Toast.LENGTH_SHORT).show();
        }
    }

    @Subscribe
    public void onFastNodeAddrStatus(FastProvAddrEvent event) {
        mFastScanResult = null;
    }

    private void addGroupPagerItem(Group group) {
        NetworkGroupFragment fragment = new NetworkGroupFragment();
        fragment.setGroup(group);
        mGroupList.add(group);
        mFragments.add(fragment);
    }

    void connectNode(ScanResult scanResult) {
        showProgress(true);
        mMeshConnection.connectNode(scanResult);
    }

    void disconnectNode() {
        mMeshConnection.disconnectNode();

        if (mMenu != null) {
            mMenu.setGroupEnabled(MENU_GROUP_ALL, false);
        }
    }

    void configuration(Node node) {
        Intent intent = new Intent(this, NodeActivity.class);
        intent.putExtra(Constants.KEY_NODE_MAC, node.getMac());
        startActivity(intent);
    }

    private void showGroupDeleteDialog() {
        int selection = mViewPager.getCurrentItem();
        Group group = mGroupList.get(selection);
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.network_group_delete_dialog_message, group.getName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    new GroupDeleteTask(group.getAddress()).run();

                    mGroupList.remove(selection);
                    mFragments.remove(selection);
                    mViewPagerAdapter.notifyDataSetChanged();
                    mViewPager.setCurrentItem(0);
                })
                .show();


    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: {
                    return getString(R.string.network_group_all);
                }
                default: {
                    Group group = mGroupList.get(position);
                    return group.getName();
                }
            }
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }
    }
}
