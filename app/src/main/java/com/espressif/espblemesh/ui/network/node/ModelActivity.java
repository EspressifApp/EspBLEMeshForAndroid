package com.espressif.espblemesh.ui.network.node;

import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Node;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.app.MeshApp;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelAppEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelSubscriptionEvent;
import com.espressif.espblemesh.model.MeshConnection;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import libs.espressif.log.EspLog;

public class ModelActivity extends BaseActivity {
    private final EspLog mLog = new EspLog(getClass());

    private Model mModel;
    private Node mNode;

    private MeshConnection mMeshConnection;

    private Button mBindAppBtn;

    private View mOperationForm;
    private CompoundButton mOpSwitch;
    private ImageView mOpImage;
    private IndicatorSeekBar mOpSeekBar;

    private List<Group> mAllGroupList;

    private Button mBindGroupBtn;
    private RecyclerView mGroupView;
    private ModelGroupAdapter mGroupAdapter;
    private List<Group> mGroupList;

    private Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.model_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mHandler = new Handler();
        MeshApp app = MeshApp.getInstance();
        Intent getIntent = getIntent();
        mModel = (Model) app.takeCache(getIntent.getStringExtra(Constants.KEY_MODEL));
        mNode = (Node) app.takeCache(getIntent.getStringExtra(Constants.KEY_NODE));
        //noinspection unchecked
        mAllGroupList = (List<Group>) app.takeCache(getIntent.getStringExtra(Constants.KEY_GROUPS));
        mMeshConnection = MeshConnection.Instance;
        setTitle(String.format("%s: %s", mNode.getName(), mModel.getId()));

        mBindAppBtn = findViewById(R.id.model_bind_app_btn);
        mBindAppBtn.setVisibility(mModel.hasAppKey() ? View.GONE : View.VISIBLE);
        mBindAppBtn.setOnClickListener(v -> {
            mBindAppBtn.setEnabled(false);
            mHandler.postDelayed(() -> mBindAppBtn.setEnabled(true), 300);

            if (!mNode.containsAppKeyIndex(mMeshConnection.getApp().getKeyIndex())) {
                mMeshConnection.appKeyAdd(mNode);
            }
            mMeshConnection.modelAppBind(mNode, mModel);
        });

        mOperationForm = findViewById(R.id.model_operation_form);
        mOperationForm.setVisibility(mModel.hasAppKey() ? View.VISIBLE : View.GONE);
        mOpSwitch = findViewById(R.id.model_switch);
        mOpSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mMeshConnection.genericOnOff(isChecked, mNode, mModel.getElementAddress());
        });
        mOpImage = findViewById(R.id.model_image);
        mOpImage.setOnClickListener(v -> gotoOperation());
        mOpSeekBar = findViewById(R.id.model_seek_bar);
        mOpSeekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                int level = 0xffff * seekBar.getProgress() / 100;
                mLog.i("GenericLevel = " + Integer.toHexString(level));
                mMeshConnection.genericLevel(level, mNode, mModel.getElementAddress());

                mOpSeekBar.setEnabled(false);
                mHandler.postDelayed(() -> mOpSeekBar.setEnabled(true), 200);
            }
        });
        showOperationWidget();

        mBindGroupBtn = findViewById(R.id.model_bind_group_btn);
        mBindGroupBtn.setOnClickListener(v -> {
            mBindGroupBtn.setEnabled(false);
            mHandler.postDelayed(() -> mBindGroupBtn.setEnabled(true), 100);

            List<Group> bindGroups = new LinkedList<>();
            for (Group group : mAllGroupList) {
                if (!group.hasModel(mModel.getElementAddress(), mModel.getId())) {
                    bindGroups.add(group);
                }
            }

            if (bindGroups.isEmpty()) {
                Toast.makeText(ModelActivity.this, R.string.node_model_bind_group_no_message,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ModelActivity.this, ModelBindGroupActivity.class);
            MeshApp.getInstance().putCacheAndSaveCacheKeyInIntent(intent,
                    Constants.KEY_BIND_GROUPS, bindGroups,
                    Constants.KEY_MODEL, mModel,
                    Constants.KEY_NODE, mNode);
            startActivity(intent);
        });

        mGroupView = findViewById(R.id.model_group_view);
        mGroupList = new ArrayList<>();
        mGroupAdapter = new ModelGroupAdapter();
        mGroupView.setAdapter(mGroupAdapter);
        updateGroup();

        EventBus.getDefault().register(this);

        boolean gotoOperation = getIntent.getBooleanExtra(Constants.KEY_OPERATION, false);
        if (gotoOperation) {
            gotoOperation();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    private void gotoOperation() {
        Intent intent = new Intent(ModelActivity.this, OperationActivity.class);
        MeshApp.getInstance().putCacheAndSaveCacheKeyInIntent(intent,
                Constants.KEY_MODEL, mModel,
                Constants.KEY_NODE, mNode);
        startActivity(intent);
    }

    private void showOperationWidget() {
        if (!mModel.hasAppKey()) {
            return;
        }
        switch (mModel.getId()) {
            case MeshConstants.MODEL_ID_GENERIC_ONOFF:
                mOpSwitch.setVisibility(View.VISIBLE);
                break;
            case MeshConstants.MODEL_ID_GENERIC_LEVEL:
                findViewById(R.id.model_seek_layout).setVisibility(View.VISIBLE);
                break;
            case MeshConstants.MODEL_ID_HSL:
                mOpImage.setVisibility(View.VISIBLE);
                mOpImage.setImageResource(R.drawable.ic_color_lens_24dp);
                break;
            case MeshConstants.MODEL_ID_CTL:
                mOpImage.setVisibility(View.VISIBLE);
                mOpImage.setImageResource(R.drawable.ic_edit_24dp);
                break;
            default:
                mOpSwitch.setVisibility(View.GONE);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattConnectionEvent(GattConnectionEvent event) {
        if (event.getStatus() != BluetoothGatt.GATT_SUCCESS || event.getState() == BluetoothGatt.STATE_DISCONNECTED) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelAppEvent(ModelAppEvent event) {
        if (event.getStatus() == 0 && Objects.equals(event.getModeId(), mModel.getId()) &&
                Objects.equals(event.getNodeMac(), mModel.getNodeMac())) {
            mBindAppBtn.setVisibility(View.GONE);
            mOperationForm.setVisibility(View.VISIBLE);
            showOperationWidget();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 100)
    public void onModelSubscriptionEvent(ModelSubscriptionEvent event) {
        mLog.i("onModelSubscriptionEvent");
        if (event.getStatus() != 0) {
            return;
        }

        for (Group group : mAllGroupList) {
            if (group == null) {
                continue;
            }

            if (group.getAddress() == event.getGroupAddr()) {
                if (group.hasModel(event.getElementAddr(), event.getModelId())) {
                    mLog.i("onModelSubscriptionEvent Del " + event.getModelId());
                    group.removeModel(event.getNodeMac(), event.getElementAddr(), event.getModelId());
                } else {
                    mLog.i("onModelSubscriptionEvent Add " + event.getModelId());
                    group.addModel(event.getNodeMac(), event.getElementAddr(), event.getModelId());
                }
            }
        }

        updateGroup();
    }

    void updateGroup() {
        mGroupList.clear();
        for (Group group : mAllGroupList) {
            if (group.hasModel(mModel.getElementAddress(), mModel.getId())) {
                mGroupList.add(group);
            }
        }

        mGroupAdapter.notifyDataSetChanged();
    }

    class ModelGroupHolder extends RecyclerView.ViewHolder {
        Group group;

        TextView groupNameTV;
        TextView groupSummaryTV;
        Button groupUnbindBtn;

        ModelGroupHolder(View itemView) {
            super(itemView);

            groupNameTV = itemView.findViewById(R.id.group_name);
            groupSummaryTV = itemView.findViewById(R.id.group_summary);

            groupUnbindBtn = itemView.findViewById(R.id.group_unbind_btn);
            groupUnbindBtn.setOnClickListener(v -> {
                mMeshConnection.modelSubscriptionDelete(mNode, mModel, group.getAddress());
            });
        }
    }

    class ModelGroupAdapter extends RecyclerView.Adapter<ModelGroupHolder> {
        private LayoutInflater mInflater = getLayoutInflater();

        @NonNull
        @Override
        public ModelGroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.node_element_model_group_item, parent, false);
            return new ModelGroupHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ModelGroupHolder holder, int position) {
            Group group = mGroupList.get(position);

            holder.group = group;
            holder.groupNameTV.setText(group.getName());
            holder.groupSummaryTV.setText(String.format("%04X", group.getAddress()));
        }

        @Override
        public int getItemCount() {
            return mGroupList.size();
        }
    }
}
