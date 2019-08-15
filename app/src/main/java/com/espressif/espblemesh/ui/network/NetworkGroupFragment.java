package com.espressif.espblemesh.ui.network;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Element;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.blemesh.model.message.standard.proxyconfiguration.AddAddressesToFilterMessage;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.MeshApp;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.GattCloseEvent;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelAppEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelSubscriptionEvent;
import com.espressif.espblemesh.model.MeshConnection;
import com.espressif.espblemesh.ui.MainService;
import com.espressif.espblemesh.ui.network.ota.NodeOTAActivity;
import com.espressif.espblemesh.ui.network.ota.OTAPackage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import libs.espressif.log.EspLog;

public class NetworkGroupFragment extends Fragment {
    private static final int REQUEST_NODE = NetworkActivity.REQUEST_NODE;
    private static final int REQUEST_OTA = NetworkActivity.REQUEST_OTA;

    private static final int POPUP_MENU_DELETE = 0x1000;
    private static final int POPUP_MENU_FAST_PROV = 0x1001;
    private static final int POPUP_MENU_OTA = 0x1002;

    private static final int OTA_PERMISSION_WRITE = 0x2000;

    private final EspLog mLog = new EspLog(getClass());

    private MeshConnection mMeshConnection;

    private Network mNetwork;
    private Group mGroup;
    private List<Node> mNodeList;

    private NodeAdapter mNodeAdapter;

    private Switch mGroupSwitch;

    private MeshUser mUser;

    private NetworkActivity mActivity;

    private boolean mViewCreated = false;

    private OTAPackage mOTAPackage;

    private SwipeRefreshLayout mRefreshLayout;
    private Handler mHandler;
    private Runnable mScanOverRunnable;

    private Set<String> mVagrantMacs = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.network_pager_item_fragment, container, false);

        mActivity = (NetworkActivity) getActivity();
        mHandler = new Handler();

        mUser = MeshUser.Instance;
        mMeshConnection = MeshConnection.Instance;

        mNodeList = new ArrayList<>();
        RecyclerView nodesView = view.findViewById(R.id.recycler_view);
        mNodeAdapter = new NodeAdapter();
        nodesView.setAdapter(mNodeAdapter);
        updateNodeList();

        mGroupSwitch = view.findViewById(R.id.group_switch);
        mGroupSwitch.setEnabled(mMeshConnection.isConnected());
        mGroupSwitch.setVisibility(mGroup == null ? View.GONE : View.VISIBLE);
        mGroupSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mNodeList.isEmpty()) {
                return;
            }

            mMeshConnection.onOff(isChecked, null, mGroup.getAddress());
        });

        mRefreshLayout = view.findViewById(R.id.pager_refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mScanOverRunnable = () -> {
            updateNodeList();
            mRefreshLayout.setRefreshing(false);

            stopScanBle();
        };
        mRefreshLayout.setOnRefreshListener(() -> {
            MainService service = mActivity.getService();
            if (service.isBTEnable()) {
                service.startScanBle();
                mHandler.postDelayed(mScanOverRunnable, 2500);
            } else {
                Toast.makeText(mActivity, R.string.main_bt_disable_toast, Toast.LENGTH_SHORT).show();
                mHandler.post(() -> mRefreshLayout.setRefreshing(false));
            }
        });

        mViewCreated = true;

        EventBus.getDefault().register(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);

        mHandler.removeCallbacks(mScanOverRunnable);
        stopScanBle();

        mViewCreated = false;

        mActivity = null;
    }

    public boolean isViewCreated() {
        return mViewCreated;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NODE) {
            return;
        } else if (requestCode == REQUEST_OTA) {
            mOTAPackage = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case OTA_PERMISSION_WRITE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    gotoOTA();
                    mLog.d("Request storage permission granted");
                }
                return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void setGroup(Group group) {
        mGroup = group;
        mNetwork = null;
    }

    public void setNetwork(Network network) {
        mNetwork = network;
        mGroup = null;
    }

    void updateNodeList() {
        mNodeList.clear();
        List<Node> vagrantNodes = new ArrayList<>();
        if (mNetwork != null) {
            for (String nodeMac : mNetwork.getNodeMacList()) {
                Node node = mUser.getNodeForMac(nodeMac);
                mNodeList.add(node);
            }

            List<ScanResult> nodes = mActivity.getService().getNodeScanList();
            mVagrantMacs.clear();
            for (ScanResult scanResult : nodes) {
                String address = scanResult.getDevice().getAddress();
                if (!mNetwork.containsNode(address)) {
                    Node node = new Node();
                    node.setMac(address);
                    node.setName(scanResult.getDevice().getName());
                    vagrantNodes.add(node);

                    mVagrantMacs.add(address);
                }
            }

        } else {
            for (String nodeMac : mGroup.getNodeMacList()) {
                Node node = mUser.getNodeForMac(nodeMac);
                mNodeList.add(node);
            }
        }
        Collections.sort(mNodeList, ((o1, o2) -> o1.getName().compareTo(o2.getName())));
        mNodeList.addAll(vagrantNodes);

        mNodeAdapter.notifyDataSetChanged();
    }

    private void gotoOTA(Node node) {
        mOTAPackage = new OTAPackage();
        mOTAPackage.messager = mMeshConnection.getMessager();
        mOTAPackage.node = node;
        MeshApp app = MeshApp.getInstance();
        String pkgKey = app.putCache(mOTAPackage);
        Intent intent = new Intent(mActivity, NodeOTAActivity.class);
        intent.putExtra(Constants.KEY_OTA_PACKAGE, pkgKey);
        startActivityForResult(intent, REQUEST_OTA);
    }

    private void stopScanBle() {
        if (mActivity.getService() != null && mActivity.getService().isScanning()) {
            mActivity.getService().stopScanBle();
        }
    }

    private class NodeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ScanResult scanResult;
        Node node;

        ImageView icon;
        TextView text1;
        TextView text2;
        ImageView image1;
        Button connectBtn;
        Button configBtn;

        NodeHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.icon);
            text1 = itemView.findViewById(R.id.text1);
            text2 = itemView.findViewById(R.id.text2);
            image1 = itemView.findViewById(R.id.image1);
            connectBtn = itemView.findViewById(R.id.connect_btn);
            configBtn = itemView.findViewById(R.id.configuration_btn);

            connectBtn.setOnClickListener(this);
            configBtn.setOnClickListener(this);

            itemView.setOnLongClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(mActivity, v);
                Menu menu = popupMenu.getMenu();
                menu.add(Menu.NONE, POPUP_MENU_DELETE, 0, R.string.network_group_popup_menu_delete);
                if (mMeshConnection.isConnected()) {
                    menu.add(Menu.NONE, POPUP_MENU_FAST_PROV, 1, R.string.network_group_popup_menu_fast_prov);
                }
//                menu.add(Menu.NONE, POPUP_MENU_OTA, 2, R.string.network_group_popup_menu_ota);
//                menu.add(Menu.NONE, 100, 3, "Add Address Filter");

                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case POPUP_MENU_DELETE:
                            mActivity.deleteNode(node.getMac());
                            return true;
                        case POPUP_MENU_FAST_PROV:
                            if (!node.containsAppKeyIndex(mActivity.getApp().getKeyIndex())) {
                                mMeshConnection.appKeyAdd(node);
                            }
                            mMeshConnection.fastProv(node);
                            return true;
                        case POPUP_MENU_OTA:
                            String writePerm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                            if (ContextCompat.checkSelfPermission(mActivity, writePerm) ==
                                    PackageManager.PERMISSION_GRANTED) {
                                gotoOTA(node);
                            } else {
                                requestPermissions(new String[]{writePerm}, OTA_PERMISSION_WRITE);
                            }
                            return true;
                        case 100:
                            List<byte[]> list = new ArrayList<>();
                            list.add(new byte[]{(byte) 0xf0, 0x00});
                            AddAddressesToFilterMessage filter = new AddAddressesToFilterMessage(node, list);
//                            mMeshConnection.getMessager().addAddressesToFilter(filter);
                            return true;
                    }

                    return false;
                });
                popupMenu.show();

                return true;
            });
        }

        @Override
        public void onClick(View v) {
            if (v == connectBtn) {
                stopScanBle();
                mActivity.connectNode(scanResult);
            } else {
                mActivity.configuration(node);
            }
        }
    }

    private class NodeAdapter extends RecyclerView.Adapter<NodeHolder> {

        @NonNull
        @Override
        public NodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.network_group_item, parent, false);
            return new NodeHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull NodeHolder holder, int position) {
            Node node = mNodeList.get(position);
            holder.node = node;

            ScanResult scanResult = mActivity.getService().getNodeScanResult(node.getMac());
            holder.scanResult = scanResult;

            boolean online = scanResult != null;
            boolean connected = mMeshConnection.isConnected();

            holder.icon.setImageResource(R.drawable.ic_device_hub_24dp);
            boolean hasCompo = node.hasCompositionData();
            boolean hasAppKey = node.containsAppKeyIndex(mActivity.getApp().getKeyIndex());
            if (hasCompo && hasAppKey) {
                for (Element element : node.getElementList()) {
                    Model model = element.getModeForId(MeshConstants.MODEL_ID_ONOFF);
                    if (model != null) {
                        if (model.hasAppKey()) {
                            holder.icon.setImageResource(R.drawable.ic_lightbulb_outline_24dp);
                        }
                        break;
                    }
                }
            }

            holder.text1.setText(node.getName());
            holder.text2.setText(node.getMac());

            if (mVagrantMacs.contains(node.getMac())) {
                holder.text1.setTextColor(mActivity.getResources().getColor(R.color.vagrantNode));
                holder.text2.setTextColor(mActivity.getResources().getColor(R.color.vagrantNode));
            } else {
                holder.text1.setTextColor(mActivity.getResources().getColor(R.color.networkNode));
                holder.text2.setTextColor(mActivity.getResources().getColor(R.color.networkNode));
            }

            if (Objects.equals(node.getMac(), mMeshConnection.getConnectedAddress())) {
                holder.image1.setVisibility(View.VISIBLE);
            } else {
                holder.image1.setVisibility(View.GONE);
            }

            holder.connectBtn.setEnabled(online);
            holder.connectBtn.setVisibility(connected ? View.GONE : View.VISIBLE);

            holder.configBtn.setVisibility(connected ? View.VISIBLE : View.GONE);

            if (mVagrantMacs.contains(node.getMac())) {
                holder.configBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mNodeList.size();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattConnectionEvent(GattConnectionEvent event) {
        int status = event.getStatus();
        int newState = event.getState();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    mGroupSwitch.setEnabled(true);
                    updateNodeList();
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    mGroupSwitch.setEnabled(false);
                    updateNodeList();
                    break;
            }
        } else {
            mGroupSwitch.setEnabled(false);
            updateNodeList();
        }

        if (mOTAPackage != null && mOTAPackage.gattCB != null) {
            mOTAPackage.gattCB.onConnectionStateChange(event.getGatt(), status, newState);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattCloseEvent(GattCloseEvent event) {
        mGroupSwitch.setEnabled(false);
        updateNodeList();

        if (mOTAPackage != null && mOTAPackage.messageCB != null) {
            mOTAPackage.messageCB.onGattClosed();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelAppEvent(ModelAppEvent event) {
        mLog.d("onModelAppStatus " + event.getStatus());
        if (event.getStatus() == 0) {
            if ((mGroup != null && mGroup.hasNode(event.getNodeMac())) ||
                    (mNetwork != null && mNetwork.containsNode(event.getNodeMac()))) {
                mNodeAdapter.notifyDataSetChanged();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelSubscriptionEvent(ModelSubscriptionEvent event) {
        updateNodeList();
    }

    private class MessageCB extends SimpleMessageCallback {
        @Override
        public void onNeedOTAUpdateNotification(byte[] manufacturerId, byte[] binId, byte[] version) {
            if (mOTAPackage != null && mOTAPackage.messageCB != null) {
                mOTAPackage.messageCB.onNeedOTAUpdateNotification(manufacturerId, binId, version);
            }
        }

        @Override
        public void onOTAStartResponse() {
            if (mOTAPackage != null && mOTAPackage.messageCB != null) {
                mOTAPackage.messageCB.onOTAStartResponse();
            }
        }
    }
}
