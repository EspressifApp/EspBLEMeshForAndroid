package com.espressif.espblemesh.ui.network.fastprov;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.LongSparseArray;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.db.entity.FastGroupDB;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.blemesh.FastProvAddrEvent;
import com.espressif.espblemesh.model.FastGroup;
import com.espressif.espblemesh.model.MeshConnection;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.espblemesh.ui.settings.SettingsActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import libs.espressif.log.EspLog;

public class FastProvedActivity extends BaseActivity {
    private static final long GROUP_ADDR_DEF = MeshConstants.ADDRESS_GROUP_MIN;

    private static final long FAST_GROUP_ALL_ID = 0L;
    private static final int FAST_GROUP_ALL_POSITION = 0;

    private static final int MENU_ITEM_ADD_GROUP = 0x100;
    private static final int MENU_ITEM_EDIT_NODES = 0x101;
    private static final int MENU_ITEM_DELETE_GROUP = 0x102;

    private static final int REQUEST_FAST_GROUP_ADD = 0x203;

    private final EspLog mLog = new EspLog(getClass());

    private MeshConnection mMeshConnection;
    private Network mNetwork;

    private CompoundButton mSwitch;
    private Button mGetBtn;

    private boolean mAllCheck = false;

    private boolean mNodeEditable = false;

    private TextView mCountTV;
    private boolean mShowCount = false;

    private final Set<Long> mAllAddrSet = new HashSet<>();
    private final LongSparseArray<NodeStatus> mAddrStatusMap = new LongSparseArray<>();

    private RecyclerView mFastNodeView;
    private FastNodeAdapter mFastNodeAdapter;
    private final List<Long> mShowAddrList = new LinkedList<>();

    private int mGroupSelection;
    private RecyclerView mGroupView;
    private List<FastGroup> mGroupList;
    private GroupAdapter mGroupAdapter;
    private View mGroupMenuBtn;
    private View mGroupEditView;
    private View mGroupMoveNodeBtn;
    private View mGroupRemoveNodeBtn;

    private Handler mHandler;

    private OnOffThread mOnOffThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fast_proved_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mMeshConnection = MeshConnection.Instance;
        mNetwork = mMeshConnection.getNetwork();

        mHandler = new Handler();

        mSwitch = findViewById(R.id.fast_proved_switch);
        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mAllCheck = true;
            mFastNodeAdapter.notifyDataSetChanged();

            Collection<Long> addrList = mGroupSelection == FAST_GROUP_ALL_POSITION ? mAllAddrSet
                    : mGroupList.get(mGroupSelection).getNodeAddrs();
            for (Long addr : addrList) {
                NodeStatus status = mAddrStatusMap.get(addr);
                status.on = isChecked;
            }
            mHandler.post(() -> mAllCheck = false);

            if (!mMeshConnection.isConnected()) {
                return;
            }

            FastGroup fastGroup = mGroupList.get(mGroupSelection);
            onOff(isChecked, fastGroup.getAddr());
        });

        mGetBtn = findViewById(R.id.fast_proved_get);
        mGetBtn.setOnClickListener(v -> {
            if (!mMeshConnection.isConnected()) {
                Toast.makeText(this, R.string.fast_provisioned_no_connection, Toast.LENGTH_SHORT).show();
                return;
            }

            mGetBtn.setEnabled(false);
            Node node = mMeshConnection.getConnectedNode();
            if (node != null) {
                mMeshConnection.fastProvNodeAddrGet(node);
            }
        });

        mCountTV = findViewById(R.id.fast_proved_count);
        mCountTV.setOnClickListener(v -> {
            mShowCount = !mShowCount;

            if (mShowCount) {
                mCountTV.setText(String.valueOf(mShowAddrList.size()));
            } else {
                mCountTV.setText("");
            }
        });

        View mDeleteAllIB = findViewById(R.id.fast_prov_delete_all);
        mDeleteAllIB.setOnClickListener(v -> showDeleteAllDialog());

        mFastNodeView = findViewById(R.id.fast_proved_recycler_view);
        mFastNodeAdapter = new FastNodeAdapter();
        mFastNodeView.setAdapter(mFastNodeAdapter);

        mGroupSelection = FAST_GROUP_ALL_POSITION;
        mGroupView = findViewById(R.id.fast_proved_group_view);
        mGroupList = new ArrayList<>();
        addFastGroupAll();
        loadFastGroupFromDB();
        for (FastGroup group : mGroupList) {
            mLog.e("GroupAddr = " + group.getAddr());
        }
        mGroupAdapter = new GroupAdapter();
        mGroupView.setAdapter(mGroupAdapter);

        mGroupMenuBtn = findViewById(R.id.fast_proved_group_menu);
        mGroupMenuBtn.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            Menu menu = popupMenu.getMenu();
            menu.add(Menu.NONE, MENU_ITEM_ADD_GROUP, 0, R.string.fast_provsioned_group_menu_add_group);
            menu.add(Menu.NONE, MENU_ITEM_EDIT_NODES, 0, R.string.fast_provsioned_group_menu_edit_node);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case MENU_ITEM_ADD_GROUP:
                        newFastGroup();
                        break;
                    case MENU_ITEM_EDIT_NODES:
                        editFastGroup(true);
                        break;
                }
                return true;
            });
            popupMenu.show();
        });
        mGroupEditView = findViewById(R.id.fast_proved_edit_form);
        mGroupMoveNodeBtn = mGroupEditView.findViewById(R.id.fast_proved_move);
        mGroupMoveNodeBtn.setOnClickListener(v -> {
            List<Long> checkNodes = getCheckedNodeAddrs();
            if (checkNodes.isEmpty()) {
                return;
            }

            showMoveDialog(checkNodes);
        });
        mGroupRemoveNodeBtn = mGroupEditView.findViewById(R.id.fast_proved_remove);
        mGroupRemoveNodeBtn.setOnClickListener(v -> {
            if (mGroupSelection == FAST_GROUP_ALL_POSITION) {
                Toast.makeText(this, R.string.fast_provsioned_group_remove_all_toast, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            List<Long> checkNodes = getCheckedNodeAddrs();
            if (checkNodes.isEmpty()) {
                return;
            }

            showRemoveDialog(checkNodes);
        });

        SharedPreferences fastPref = getSharedPreferences(getFastProvPrefName(), MODE_PRIVATE);
        Set<String> addrStrSet = fastPref.getAll().keySet();
        Set<Long> addrSet = new HashSet<>();
        for (String str : addrStrSet) {
            addrSet.add(Long.parseLong(str));
        }
        addAddress(addrSet);
        onFastProvNodeAddrAddComplete();

        mOnOffThread = new OnOffThread(this);
        mOnOffThread.start();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mOnOffThread.interrupt();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (mNodeEditable) {
            editFastGroup(false);
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_FAST_GROUP_ADD) {
            if (resultCode == RESULT_OK && data != null) {
                long id = data.getLongExtra(Constants.KEY_ID, -1);
                String name = data.getStringExtra(Constants.KEY_NAME);
                long addr = data.getLongExtra(Constants.KEY_GROUP_ADDRESS, -1);
                onFastGroupCreated(id, name, addr);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getFastProvPrefName() {
        return "fast-prov-" + mNetwork.getKeyIndex();
    }

    @Subscribe
    public void onFastNodeAddrStatus(FastProvAddrEvent event) {
        Node fastNode = MeshUser.Instance.getNodeForMac(event.getNodeMac());
        addAddress(new long[]{fastNode.getUnicastAddress()});
        Set<Long> newAddrSet = addAddress(event.getAddrArray());
        SharedPreferences sp = getSharedPreferences(getFastProvPrefName(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(String.valueOf(fastNode.getUnicastAddress()), "");
        for (Long addr : newAddrSet) {
            editor.putString(String.valueOf(addr), "");
        }
        editor.apply();
        onFastProvNodeAddrAddComplete();
    }

    private List<Long> getCheckedNodeAddrs() {
        List<Long> result = new ArrayList<>();
        for (Long addr : mAllAddrSet) {
            NodeStatus status = mAddrStatusMap.get(addr);
            boolean checked = status != null && status.checked;
            if (checked) {
                result.add(addr);
            }
        }
        return result;
    }

    private void addFastGroupAll() {
        FastGroup group = new FastGroup();
        group.setId(FAST_GROUP_ALL_ID);
        group.setName(getString(R.string.fast_provsioned_group_all));
        group.setAddr(GROUP_ADDR_DEF);
        mGroupList.add(group);
    }

    private void loadFastGroupFromDB() {
        List<FastGroupDB> groupDBS = MeshObjectBox.getInstance().loadFastGroups();
        for (FastGroupDB db : groupDBS) {
            FastGroup group = new FastGroup();
            group.setId(db.id);
            group.setName(db.name);
            group.setAddr(db.addr);
            group.setNodeAddrs(db.getNodeAddrListForNodes());
            mGroupList.add(group);
        }
    }

    private void sortAddressList() {
        if (!mShowAddrList.isEmpty()) {
            Collections.sort(mShowAddrList, Long::compareTo);
        }
    }

    private Set<Long> addAddress(Collection<Long> addrColl) {
        mLog.d("Add Address Collection size=" + addrColl.size());
        Set<Long> newAddrs = new HashSet<>();
        synchronized (mAllAddrSet) {
            for (Long addr : addrColl) {
                if (!mAllAddrSet.contains(addr)) {
                    mAllAddrSet.add(addr);

                    newAddrs.add(addr);
                }
            }
        }

        return newAddrs;
    }

    private Set<Long> addAddress(long[] addressArray) {
        mLog.d("Add Address Array length=" + addressArray.length);
        Set<Long> newAddrs = new HashSet<>();
        synchronized (mAllAddrSet) {
            for (long addr : addressArray) {
                if (!mAllAddrSet.contains(addr)) {
                    mAllAddrSet.add(addr);

                    newAddrs.add(addr);
                }
            }
        }

        return newAddrs;
    }

    private void onFastProvNodeAddrAddComplete() {
        mLog.d("onFastProvNodeAddrAddComplete");
        runOnUiThread(() -> {
            List<Long> tempList;
            synchronized (mAllAddrSet) {
                for (int i = 0; i < mAddrStatusMap.size(); i++) {
                    Long addr = mAddrStatusMap.keyAt(i);
                    if (!mAllAddrSet.contains(addr)) {
                        mAddrStatusMap.remove(i--);
                    }
                }
                for (Long addr : mAllAddrSet) {
                    NodeStatus status = mAddrStatusMap.get(addr);
                    if (status == null) {
                        status = new NodeStatus();
                        mAddrStatusMap.put(addr, status);
                    }
                }

                tempList = new ArrayList<>(mAllAddrSet);
            }

            updateAddr(tempList);

            mGetBtn.setEnabled(true);
        });
    }

    private void updateAddr(Collection<Long> addrs) {
        runOnUiThread(() -> {
            Collection<Long> nodeList;
            if (mGroupSelection == FAST_GROUP_ALL_POSITION) {
                nodeList = addrs;
            } else {
                FastGroup group = mGroupList.get(mGroupSelection);
                nodeList = group.getNodeAddrs();
            }
            synchronized (mShowAddrList) {
                mShowAddrList.clear();
                mShowAddrList.addAll(nodeList);
                sortAddressList();
                mFastNodeAdapter.notifyDataSetChanged();

                for (Long showAddr : mShowAddrList) {
                    if (mAddrStatusMap.get(showAddr) == null) {
                        mAddrStatusMap.put(showAddr, new NodeStatus());
                    }
                }
            }

            if (mShowCount) {
                mCountTV.setText(String.valueOf(mShowAddrList.size()));
            } else {
                mCountTV.setText("");
            }
        });
    }

    private void deleteAllFastProved() {
        SharedPreferences sp = getSharedPreferences(getFastProvPrefName(), MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    private void showDeleteAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.fast_provisioned_delete_all_title)
                .setMessage(R.string.fast_provisioned_delete_all_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    deleteAllFastProved();
                    mShowAddrList.clear();
                    mAllAddrSet.clear();
                    mFastNodeAdapter.notifyDataSetChanged();
                    mCountTV.setText("");

                    deleteNodeFromFastGroup();
                })
                .show();
    }

    private void deleteNodeFromFastGroup() {
        for (FastGroup group : mGroupList) {
            group.setNodeAddrs(Collections.emptyList());

            if (group.getId() == FAST_GROUP_ALL_ID) {
                continue;
            }

            FastGroupDB db = new FastGroupDB();
            db.id = group.getId();
            db.name = group.getName();
            db.addr = group.getAddr();
            db.nodes = null;
            MeshObjectBox.getInstance().addOrUpdateFastGroup(db);
        }
    }

    private void addOnOffTaskInQueue(boolean on, Node node, long addr) {
        OnOffParams params = new OnOffParams(on, node, addr);
        if (mOnOffThread != null) {
            mOnOffThread.addTask(params);
        }
    }

    private void onOff(boolean on, long addr) {
        addOnOffTaskInQueue(on, null, addr);
    }

    private class FastNodeHolder extends RecyclerView.ViewHolder {
        long addr;

        CompoundButton switchCB;
        CompoundButton editCB;


        CompoundButton.OnCheckedChangeListener checkListener;

        FastNodeHolder(View itemView) {
            super(itemView);

            switchCB = itemView.findViewById(R.id.fast_proved_item_switch);
            editCB = itemView.findViewById(R.id.fast_proved_item_check);

            checkListener = (buttonView, isChecked) -> {
                if (buttonView == switchCB) {
                    NodeStatus status = mAddrStatusMap.get(addr);
                    if (status != null) {
                        status.on = isChecked;
                    }

                    if (mAllCheck) {
                        return;
                    }

                    if (!mMeshConnection.isConnected()) {
                        return;
                    }

                    onOff(isChecked, addr);
                } else if (buttonView == editCB) {
                    NodeStatus status = mAddrStatusMap.get(addr);
                    if (status != null) {
                        status.checked = isChecked;
                    }
                }
            };
            switchCB.setOnCheckedChangeListener(checkListener);
            editCB.setOnCheckedChangeListener(checkListener);
        }
    }

    private class NodeStatus {
        boolean on = false;
        boolean checked = false;
    }

    private class FastNodeAdapter extends RecyclerView.Adapter<FastNodeHolder> {

        @NonNull
        @Override
        public FastNodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.fast_proved_item, parent, false);
            return new FastNodeHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FastNodeHolder holder, int position) {
            long addr = mShowAddrList.get(position);
            holder.addr = addr;
            String text = String.format("%04X", addr);
            holder.switchCB.setText(text);
            holder.editCB.setText(text);
            holder.switchCB.setOnCheckedChangeListener(null);
            if (mAllCheck) {
                holder.switchCB.setChecked(mSwitch.isChecked());
            } else {
                NodeStatus status = mAddrStatusMap.get(addr);
                boolean on = status != null && status.on;
                holder.switchCB.setChecked(on);
            }
            holder.switchCB.setOnCheckedChangeListener(holder.checkListener);

            if (mNodeEditable) {
                holder.editCB.setVisibility(View.VISIBLE);
                holder.switchCB.setVisibility(View.GONE);

                NodeStatus status = mAddrStatusMap.get(addr);
                if (status != null) {
                    holder.editCB.setChecked(status.checked);
                }
            } else {
                holder.editCB.setVisibility(View.GONE);
                holder.switchCB.setVisibility(View.VISIBLE);

                NodeStatus status = mAddrStatusMap.get(addr);
                if (status != null) {
                    status.checked = false;
                }
                holder.editCB.setChecked(false);
            }
        }

        @Override
        public int getItemCount() {
            return mShowAddrList.size();
        }
    }

    private class GroupHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        FastGroup group;
        TextView nameTV;

        GroupHolder(View itemView) {
            super(itemView);

            nameTV = itemView.findViewById(R.id.fast_group_name);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int oldSelection = mGroupSelection;
            mGroupSelection = getAdapterPosition();
            mGroupAdapter.notifyItemChanged(oldSelection);
            mGroupAdapter.notifyItemChanged(mGroupSelection);

            Collection<Long> nodeList = mGroupSelection == FAST_GROUP_ALL_POSITION ? mAllAddrSet : group.getNodeAddrs();
            updateAddr(nodeList);
        }

        @Override
        public boolean onLongClick(View v) {
            if (getAdapterPosition() == FAST_GROUP_ALL_POSITION) {
                return true;
            }

            PopupMenu popupMenu = new PopupMenu(FastProvedActivity.this, v);
            Menu menu = popupMenu.getMenu();
            menu.add(Menu.NONE, MENU_ITEM_DELETE_GROUP, 0, R.string.fast_provsioned_group_menu_delete_group);
            popupMenu.setOnMenuItemClickListener(item -> {
                deleteFastGroup(group, getAdapterPosition());
                return true;
            });
            popupMenu.show();
            return true;
        }
    }

    private class GroupAdapter extends RecyclerView.Adapter<GroupHolder> {

        @NonNull
        @Override
        public GroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.fast_group_item, parent,
                    false);
            return new GroupHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupHolder holder, int position) {
            FastGroup group = mGroupList.get(position);
            holder.group = group;
            holder.nameTV.setText(group.getName());
            int textColor = mGroupSelection == position ? getResources().getColor(R.color.colorAccent)
                    : Color.BLACK;
            holder.nameTV.setTextColor(textColor);
        }

        @Override
        public int getItemCount() {
            return mGroupList.size();
        }
    }

    private void onFastGroupCreated(long id, String name, long addr) {
        FastGroup group = new FastGroup();
        group.setId(id);
        group.setName(name);
        group.setAddr(addr);
        mGroupList.add(group);
        mGroupAdapter.notifyItemInserted(mGroupList.size() - 1);
        mGroupView.scrollToPosition(mGroupList.size() - 1);
    }

    private void newFastGroup() {
        Intent intent = new Intent(this, FastGroupCreateActivity.class);
        startActivityForResult(intent, REQUEST_FAST_GROUP_ADD);
    }

    private void deleteFastGroup(FastGroup group, int position) {
        if (group.getId() == FAST_GROUP_ALL_ID) {
            return;
        }

        MeshObjectBox.getInstance().removeFastGroup(group.getId());
        MeshObjectBox.getInstance().deleteAddress(group.getAddr());
        if (mMeshConnection.isConnected()) {
            mMeshConnection.unbindFastGroup(null, 0xffff, group.getAddr(), group.getNodeAddrs());
        }

        mGroupList.remove(position);
        mGroupAdapter.notifyItemRemoved(position);

        if (position == mGroupSelection) {
            mGroupSelection = FAST_GROUP_ALL_POSITION;
            mGroupAdapter.notifyItemChanged(FAST_GROUP_ALL_POSITION);
            updateAddr(mAllAddrSet);
        }
    }

    private void editFastGroup(boolean editable) {
        if (editable) {
            mNodeEditable = true;
            mFastNodeAdapter.notifyDataSetChanged();
            mGroupEditView.setVisibility(View.VISIBLE);
        } else {
            mNodeEditable = false;
            mFastNodeAdapter.notifyDataSetChanged();
            mGroupEditView.setVisibility(View.GONE);
        }
    }

    private void showMoveDialog(List<Long> nodeAddrs) {
        List<FastGroup> groups = new ArrayList<>(mGroupList.size());
        List<CharSequence> groupNames = new ArrayList<>(mGroupList.size());
        for (int i = 0; i < mGroupList.size(); i++) {
            if (i == FAST_GROUP_ALL_POSITION || i == mGroupSelection) {
                continue;
            }

            FastGroup group = mGroupList.get(i);
            groups.add(group);
            groupNames.add(group.getName());
        }

        CharSequence[] groupNameItems = new CharSequence[groupNames.size()];
        groupNames.toArray(groupNameItems);

        new AlertDialog.Builder(this)
                .setTitle(R.string.fast_provsioned_group_move_dialog_title)
                .setItems(groupNameItems, (dialog, which) -> {
                    FastGroup group = groups.get(which);
                    group.addNodeAddrs(nodeAddrs);

                    FastGroupDB db = new FastGroupDB();
                    db.id = group.getId();
                    db.name = group.getName();
                    db.addr = group.getAddr();
                    db.setNodeForList(group.getNodeAddrs());
                    MeshObjectBox.getInstance().addOrUpdateFastGroup(db);

                    if (mMeshConnection.isConnected()) {
                        mMeshConnection.bindFastGroup(null, 0xffff, group.getAddr(), nodeAddrs);
                    }
                })
                .show();
    }

    private void showRemoveDialog(List<Long> nodeAddrs) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.fast_provsioned_group_remove_dialog_title)
                .setMessage(R.string.fast_provsioned_group_remove_dialog_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    FastGroup group = mGroupList.get(mGroupSelection);
                    group.removeNodeAddrs(nodeAddrs);
                    if (mMeshConnection.isConnected()) {
                        mMeshConnection.unbindFastGroup(null, 0xffff, group.getAddr(), nodeAddrs);
                    }

                    FastGroupDB db = new FastGroupDB();
                    db.id = group.getId();
                    db.name = group.getName();
                    db.addr = group.getAddr();
                    db.setNodeForList(group.getNodeAddrs());
                    MeshObjectBox.getInstance().addOrUpdateFastGroup(db);

                    updateAddr(group.getNodeAddrs());
                })
                .show();
    }

    private class OnOffParams {
        boolean on;
        Node node;
        long addr;

        OnOffParams(boolean on, Node node, long addr) {
            this.on = on;
            this.node = node;
            this.addr = addr;
        }
    }

    private class OnOffThread extends Thread {
        volatile boolean stop = false;

        FastProvedActivity activity;
        LinkedBlockingQueue<OnOffParams> taskQueue = new LinkedBlockingQueue<>();

        OnOffThread(FastProvedActivity activity) {
            this.activity = activity;
        }

        @Override
        public void interrupt() {
            stop = true;
            taskQueue.clear();
            super.interrupt();
            activity = null;
        }

        void addTask(OnOffParams p) {
            taskQueue.add(p);
        }

        @Override
        public void run() {
            long interval = SettingsActivity.getMessageBackpressure(activity);
            while (!stop) {
                try {
                    OnOffParams params = taskQueue.take();
                    mMeshConnection.genericOnOff(params.on, params.node, params.addr);

                    sleep(interval);
                } catch (InterruptedException e) {
                    mLog.w("OnOff Thread is interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            mLog.d("OnOffThread over");
        }
    }
}
