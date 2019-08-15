package com.espressif.espblemesh.ui.network.node;

import android.bluetooth.BluetoothGatt;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.blemesh.constants.MeshConstants;
import com.espressif.blemesh.model.Element;
import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Network;
import com.espressif.blemesh.model.Node;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.blemesh.CompositionDataEvent;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;
import com.espressif.espblemesh.eventbus.blemesh.LightHSLEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelAppEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelSubscriptionEvent;
import com.espressif.espblemesh.eventbus.blemesh.RelayEvent;
import com.espressif.espblemesh.model.MeshConnection;
import com.espressif.blemesh.user.MeshUser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import libs.espressif.log.EspLog;
import libs.espressif.utils.TextUtils;
import libs.espressif.widget.CircleColorPicker;
import libs.espressif.widget.ColorPicker;
import libs.espressif.widget.LinearColorPicker;

public class NodeConfActivity extends BaseActivity {
    private final EspLog mLog  = new EspLog(getClass());

    private MeshUser mUser;
    private MeshConnection mMeshConnection;

    private Network mNetwork;
    private Node mNode;

    private View mProgressView;
    private View mNodeForm;

    private List<Group> mGroupList;

    private Button mCompositoinBtn;
    private View mCompositionForm;

    private EditText mRelayCountET;
    private EditText mRelayStepET;
    private Button mRelaySetBtn;

    private TextView mCidTV;
    private TextView mPidTV;
    private TextView mVidTV;
    private TextView mCrplTV;
    private TextView mFeaturesTV;

    private List<Object> mElementModelList;
    private ElementModelAdapter mElementModelAdapter;

    private View mHSLColorDisplay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.node_conf_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mMeshConnection = MeshConnection.Instance;
        mUser = MeshUser.Instance;
        mNetwork = mMeshConnection.getNetwork();

        String nodeMac = getIntent().getStringExtra(Constants.KEY_NODE_MAC);
        mNode = mUser.getNodeForMac(nodeMac);
        setTitle(mNode.getName());

        mProgressView = findViewById(R.id.progress);
        mNodeForm = findViewById(R.id.node_form);
        showProgress(false);

        mGroupList = new ArrayList<>();
        mGroupList.addAll(mUser.getGroupListForNetwork(mNetwork.getKeyIndex()));
        Collections.sort(mGroupList, ((o1, o2) -> {
            Long addr1 = o1.getAddress();
            Long addr2 = o2.getAddress();
            return addr1.compareTo(addr2);
        }));

        mCompositoinBtn = findViewById(R.id.node_composition_data_btn);
        mCompositoinBtn.setOnClickListener(v -> requestCompositionData());

        mRelayCountET = findViewById(R.id.relay_count);
        mRelayStepET = findViewById(R.id.relay_step);
        mRelaySetBtn = findViewById(R.id.relay_set_btn);
        mRelaySetBtn.setOnClickListener(v -> {
            String countStr = mRelayCountET.getText().toString();
            String stepStr = mRelayStepET.getText().toString();
            if (TextUtils.isEmpty(countStr) || TextUtils.isEmpty(stepStr)) {
                return;
            }

            int count = Integer.parseInt(countStr);
            int step = Integer.parseInt(stepStr);
            mMeshConnection.relaySet(mNode, true, count, step);
        });

        mCompositionForm = findViewById(R.id.node_composition_data_form);

        mCidTV = findViewById(R.id.node_cid_text);
        mPidTV = findViewById(R.id.node_pid_text);
        mVidTV = findViewById(R.id.node_vid_text);
        mCrplTV = findViewById(R.id.node_crpl_text);
        mFeaturesTV = findViewById(R.id.node_features_text);

        mElementModelList = new ArrayList<>();
        mElementModelAdapter = new ElementModelAdapter();
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(mElementModelAdapter);

        showCompositionButton(!mNode.hasCompositionData());
        setCompositionData();

        if (!mNode.hasCompositionData()) {
            requestCompositionData();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
            mNodeForm.setVisibility(View.GONE);
        } else {
            mProgressView.setVisibility(View.GONE);
            mNodeForm.setVisibility(View.VISIBLE);
        }
    }

    private void requestCompositionData() {
        mMeshConnection.compositionDataGet(mNode);
    }

    private void showCompositionButton(boolean show) {
        if (show) {
            mCompositoinBtn.setVisibility(View.VISIBLE);
            mCompositionForm.setVisibility(View.GONE);
        } else {
            mCompositoinBtn.setVisibility(View.GONE);
            mCompositionForm.setVisibility(View.VISIBLE);
        }
    }

    private void setCompositionData() {
        String cidStr = mNode.getCid() == null ? "" : String.format("%04X", mNode.getCid());
        mCidTV.setText(cidStr);
        String pidStr = mNode.getPid() == null ? "" : String.format("%04X", mNode.getPid());
        mPidTV.setText(pidStr);
        String vidStr = mNode.getVid() == null ? "" : String.format("%04X", mNode.getVid());
        mVidTV.setText(vidStr);
        String crplStr = mNode.getCrpl() == null ? "" : String.format("%04X", mNode.getCrpl());
        mCrplTV.setText(crplStr);
        String featuresStr = mNode.getFeatures() == null ? "" : String.format("%04X", mNode.getFeatures());
        mFeaturesTV.setText(featuresStr);

        mElementModelList.clear();
        Observable.fromIterable(mNode.getElementList())
                .concatMap(element -> {
                    mElementModelList.add(element);
                    return Observable.fromIterable(element.getModelList());
                })
                .doOnNext(model -> mElementModelList.add(model))
                .subscribe();
        mElementModelAdapter.notifyDataSetChanged();
    }

    private void showHSLDialog(long dstAddr) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.node_conf_hsl_dialog)
                .setOnDismissListener(d -> mHSLColorDisplay = null)
                .show();
        View colorDisplay = dialog.findViewById(R.id.color_display);
        CircleColorPicker circleCP = dialog.findViewById(R.id.color_picker1);
        LinearColorPicker linearCP = dialog.findViewById(R.id.color_picker2);
        int[] linearColors = {Color.BLACK, 0xff808080, Color.WHITE};
        assert circleCP != null && colorDisplay != null && linearCP != null;
        mHSLColorDisplay = colorDisplay;
        linearCP.setColors(linearColors);
        linearCP.setPositions(new float[]{0f, 0.5f, 1f});
        linearCP.setTriaAxisPercent(0.5f);
        circleCP.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChangeStart(View v, int color) {
                colorDisplay.setBackgroundColor(color);
                linearCP.setColors(new int[]{Color.BLACK, color, Color.WHITE});
            }

            @Override
            public void onColorChanged(View v, int color) {
                colorDisplay.setBackgroundColor(color);
                linearColors[1] = color;
                linearCP.setColors(new int[]{Color.BLACK, color, Color.WHITE});
            }

            @Override
            public void onColorChangeEnd(View v, int color) {
                colorDisplay.setBackgroundColor(color);
                linearColors[1] = color;
                linearCP.setColors(new int[]{Color.BLACK, color, Color.WHITE});
                mMeshConnection.setLightHSL(color, mNode, dstAddr);
            }
        });
        linearCP.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChangeStart(View v, int color) {
                colorDisplay.setBackgroundColor(color);
            }

            @Override
            public void onColorChanged(View v, int color) {
                colorDisplay.setBackgroundColor(color);
            }

            @Override
            public void onColorChangeEnd(View v, int color) {
                colorDisplay.setBackgroundColor(color);
                mMeshConnection.setLightHSL(color, mNode, dstAddr);
            }
        });

        mMeshConnection.getLightHSL(mNode, dstAddr);
    }

    private class ElementNodeHolder extends RecyclerView.ViewHolder {
        Model model;

        View elementForm;
        View modelForm;

        TextView elementText1;

        TextView modelText1;
        Switch switch1;
        ImageView image1;
        Button bindAppBtn;
        Button bindGroupBtn;

        RecyclerView groupForm;
        List<Group> modelGroupList;
        ModelGroupAdapter modelGroupAdapter;

        ElementNodeHolder(View itemView) {
            super(itemView);

            elementForm = itemView.findViewById(R.id.element_form);
            modelForm = itemView.findViewById(R.id.model_form);

            elementText1 = itemView.findViewById(R.id.element_text1);

            modelText1 = itemView.findViewById(R.id.model_text1);
            switch1 = itemView.findViewById(R.id.switch1);
            switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mMeshConnection.onOff(isChecked, mNode, model.getElementAddress());
            });
            image1 = itemView.findViewById(R.id.image1);
            image1.setOnClickListener(v -> showHSLDialog(model.getElementAddress()));
            bindAppBtn = itemView.findViewById(R.id.button1);
            bindAppBtn.setOnClickListener(v -> {
                if (!mNode.containsAppKeyIndex(mMeshConnection.getApp().getKeyIndex())) {
                    mMeshConnection.appKeyAdd(mNode);
                }
                mMeshConnection.modelAppBind(mNode, model);
            });

            bindGroupBtn = itemView.findViewById(R.id.bind_group_btn);
            bindGroupBtn.setOnClickListener(v -> {
                List<Group> bindGroups = new LinkedList<>();
                for (Group group : mGroupList) {
                    if (!group.hasModel(model.getElementAddress(), model.getId())) {
                        bindGroups.add(group);
                    }
                }

                if (bindGroups.isEmpty()) {
                    Toast.makeText(NodeConfActivity.this, R.string.node_model_bind_group_no_message,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                showBindGroupDialog(bindGroups);
            });

            groupForm = itemView.findViewById(R.id.group_form);
            modelGroupList = new LinkedList<>();
            modelGroupAdapter = new ModelGroupAdapter();
            groupForm.setAdapter(modelGroupAdapter);
            updateGroup();
        }

        private void showBindGroupDialog(List<Group> groups) {
            String[] groupNames = new String[groups.size()];
            int i = 0;
            for (Group group : groups) {
                groupNames[i++] = group.getName();
            }

            new AlertDialog.Builder(NodeConfActivity.this)
                    .setItems(groupNames, (dialog, which) -> {
                        Group group = groups.get(which);
                        mMeshConnection.modelSubscriptionAdd(mNode, model, group.getAddress());
                        dialog.dismiss();
                    })
                    .show();
        }

        void updateGroup() {
            modelGroupList.clear();
            if (model != null) {
                for (Group group : mGroupList) {
                    if (group.hasModel(model.getElementAddress(), model.getId())) {
                        modelGroupList.add(group);
                    }
                }
            }

            modelGroupAdapter.notifyDataSetChanged();
        }

        class ModelGroupHolder extends RecyclerView.ViewHolder {
            Group group;

            TextView groupNameTV;
            Button groupUnbindBtn;

            ModelGroupHolder(View itemView) {
                super(itemView);

                groupNameTV = itemView.findViewById(R.id.group_name);

                groupUnbindBtn = itemView.findViewById(R.id.group_unbind_btn);
                groupUnbindBtn.setOnClickListener(v -> {
                    mMeshConnection.modelSubscriptionDelete(mNode, model, group.getAddress());
                });
            }
        }

        class ModelGroupAdapter extends RecyclerView.Adapter<ModelGroupHolder> {

            @NonNull
            @Override
            public ModelGroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = getLayoutInflater().inflate(R.layout.node_element_model_group_item, parent, false);
                return new ModelGroupHolder(itemView);
            }

            @Override
            public void onBindViewHolder(@NonNull ModelGroupHolder holder, int position) {
                Group group = modelGroupList.get(position);

                holder.group = group;
                holder.groupNameTV.setText(String.format("%s %04X", group.getName(), group.getAddress()));
            }

            @Override
            public int getItemCount() {
                return modelGroupList.size();
            }
        }
    }

    private class ElementModelAdapter extends RecyclerView.Adapter<ElementNodeHolder> {

        @NonNull
        @Override
        public ElementNodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.node_element_model_item, parent, false);
            return new ElementNodeHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ElementNodeHolder holder, int position) {
            Object obj = mElementModelList.get(position);
            if (obj instanceof Element) {
                setElement((Element) obj, holder);
            } else {
                setModel((Model) obj, holder);
            }
        }

        private void setElement(Element element, ElementNodeHolder holder) {
            holder.model = null;

            holder.elementForm.setVisibility(View.VISIBLE);
            holder.modelForm.setVisibility(View.GONE);

            holder.elementText1.setText(String.format("Element: %04X", element.getUnicastAddress()));
        }

        private void setModel(Model model, ElementNodeHolder holder) {
            holder.model = model;

            holder.elementForm.setVisibility(View.GONE);
            holder.modelForm.setVisibility(View.VISIBLE);

            holder.modelText1.setText(model.getId());
            holder.switch1.setVisibility(View.GONE);
            holder.image1.setVisibility(View.GONE);

            switch (model.getId()) {
                case MeshConstants.MODEL_ID_ONOFF: {
                    holder.modelText1.append(" On/Off");
                    if (model.hasAppKey()) {
                        holder.switch1.setVisibility(View.VISIBLE);
                    }
                    break;
                }
                case MeshConstants.MODEL_ID_HSL: {
                    holder.modelText1.append(" HSL");
                    if (model.hasAppKey()) {
                        // TODO
                        holder.image1.setVisibility(View.VISIBLE);
                    }
                    break;
                }
                default: {
                    break;
                }
            }

            if (model.hasAppKey()) {
                holder.bindAppBtn.setVisibility(View.GONE);
            } else {
                holder.bindAppBtn.setVisibility(View.VISIBLE);
            }

            holder.updateGroup();
        }

        @Override
        public int getItemCount() {
            return mElementModelList.size();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattConnectionEvent(GattConnectionEvent event) {
        if (event.getStatus() != BluetoothGatt.GATT_SUCCESS || event.getState() == BluetoothGatt.STATE_DISCONNECTED) {
            // TODO Toast
            finish();
        }
    }

    @Subscribe
    public void onCompositionDataEvent(CompositionDataEvent event) {
        if (event.getStatus() == 0) {
            mMeshConnection.relaySet(mNode, true);

            runOnUiThread(() -> {
                showCompositionButton(false);
                setCompositionData();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelAppEvent(ModelAppEvent event) {
        if (event.getStatus() == 0) {
            mElementModelAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelSubscriptionEvent(ModelSubscriptionEvent event) {
        if (event.getStatus() == 0) {
            mElementModelAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe
    public void onRelayEvent(RelayEvent event) {
        if (event.getState() == 1) {
            runOnUiThread(() -> {
                mRelayCountET.setText("");
                mRelayStepET.setText("");
            });
        }

        if (!mNode.containsAppKeyIndex(mMeshConnection.getApp().getKeyIndex())) {
            mMeshConnection.appKeyAdd(mNode);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLightHSLEvent(LightHSLEvent event) {
        if (mHSLColorDisplay != null) {
            int color = Color.rgb(event.getRed(), event.getGreen(), event.getBlue());
            mHSLColorDisplay.setBackgroundColor(color);
        }
    }
}
