package com.espressif.espblemesh.ui.network.node;

import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.espressif.blemesh.user.MeshUser;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.app.MeshApp;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;
import com.espressif.espblemesh.eventbus.blemesh.CompositionDataEvent;
import com.espressif.espblemesh.eventbus.blemesh.LightCTLEvent;
import com.espressif.espblemesh.eventbus.blemesh.ModelAppEvent;
import com.espressif.espblemesh.eventbus.blemesh.RelayEvent;
import com.espressif.espblemesh.model.MeshConnection;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;
import libs.espressif.collection.EspCollections;
import libs.espressif.utils.TextUtils;

public class NodeActivity extends BaseActivity {
    private static final int MENU_ID_INFO = 0x4000;

    private static final int REQUEST_NODE = 0x5000;

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

    private List<Object> mElementModelList;
    private ElementModelAdapter mElementModelAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.node_activity);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_INFO, 0, R.string.node_menu_info)
                .setIcon(R.drawable.outline_info_24)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ID_INFO) {
            showInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInfoDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(mNode.getName())
                .setView(R.layout.node_info_dialog)
                .show();
        TextView cidTV = dialog.findViewById(R.id.node_cid_text);
        TextView pidTV = dialog.findViewById(R.id.node_pid_text);
        TextView vidTV = dialog.findViewById(R.id.node_vid_text);
        TextView crplTV = dialog.findViewById(R.id.node_crpl_text);
        TextView featuresTV = dialog.findViewById(R.id.node_features_text);
        assert cidTV != null && pidTV != null && vidTV != null && crplTV!= null && featuresTV != null;
        String cidStr = mNode.getCid() == null ? "" : String.format("%04X", mNode.getCid());
        cidTV.setText(cidStr);
        String pidStr = mNode.getPid() == null ? "" : String.format("%04X", mNode.getPid());
        pidTV.setText(pidStr);
        String vidStr = mNode.getVid() == null ? "" : String.format("%04X", mNode.getVid());
        vidTV.setText(vidStr);
        String crplStr = mNode.getCrpl() == null ? "" : String.format("%04X", mNode.getCrpl());
        crplTV.setText(crplStr);
        String featuresStr = mNode.getFeatures() == null ? "" : String.format("%04X", mNode.getFeatures());
        featuresTV.setText(featuresStr);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattConnectionEvent(GattConnectionEvent event) {
        if (event.getStatus() != BluetoothGatt.GATT_SUCCESS || event.getState() == BluetoothGatt.STATE_DISCONNECTED) {
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
            int index = EspCollections.index(mElementModelList, obj -> {
                if (obj instanceof Model) {
                    Model model = (Model) obj;
                    return Objects.equals(event.getModeId(), model.getId()) &&
                            Objects.equals(event.getNodeMac(), model.getNodeMac());
                }
                return false;
            });
            if (index >= 0) {
                mElementModelAdapter.notifyItemChanged(index);
            }
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

    private class ElementModelHolder extends RecyclerView.ViewHolder {
        Model model;

        View elementForm;
        View modelForm;

        TextView elementText1;

        TextView modelText;
        CompoundButton modelSwitch;
        ImageView modelImage;

        ElementModelHolder(@NonNull View itemView) {
            super(itemView);

            elementForm = itemView.findViewById(R.id.element_form);
            elementText1 = itemView.findViewById(R.id.element_text1);

            modelForm = itemView.findViewById(R.id.model_form);
            modelForm.setOnClickListener(v -> {
                Intent intent = new Intent(NodeActivity.this, ModelActivity.class);
                MeshApp.getInstance().putCacheAndSaveCacheKeyInIntent(intent,
                        Constants.KEY_MODEL, model,
                        Constants.KEY_NODE, mNode,
                        Constants.KEY_GROUPS, mGroupList);
                startActivityForResult(intent, REQUEST_NODE);
            });
            modelText = itemView.findViewById(R.id.model_text);
            modelSwitch = itemView.findViewById(R.id.model_switch);
            modelSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mMeshConnection.genericOnOff(isChecked, mNode, model.getElementAddress());
            });
            modelImage = itemView.findViewById(R.id.model_image);
            modelImage.setOnClickListener(v -> {
                Intent intent = new Intent(NodeActivity.this, ModelActivity.class);
                MeshApp.getInstance().putCacheAndSaveCacheKeyInIntent(intent,
                        Constants.KEY_MODEL, model,
                        Constants.KEY_NODE, mNode,
                        Constants.KEY_GROUPS, mGroupList);
                intent.putExtra(Constants.KEY_OPERATION, true);
                startActivityForResult(intent, REQUEST_NODE);
            });
        }
    }

    private class ElementModelAdapter extends RecyclerView.Adapter<ElementModelHolder> {

        @NonNull
        @Override
        public ElementModelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.node_element_model_item, parent, false);
            return new ElementModelHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ElementModelHolder holder, int position) {
            Object obj = mElementModelList.get(position);
            if (obj instanceof Element) {
                setElement((Element) obj, holder);
            } else {
                setModel((Model) obj, holder);
            }
        }

        @Override
        public int getItemCount() {
            return mElementModelList.size();
        }

        private void setElement(Element element, ElementModelHolder holder) {
            holder.model = null;

            holder.elementForm.setVisibility(View.VISIBLE);
            holder.modelForm.setVisibility(View.GONE);

            holder.elementText1.setText(String.format("Element: %04X", element.getUnicastAddress()));
        }

        private void setModel(Model model, ElementModelHolder holder) {
            holder.model = model;

            holder.elementForm.setVisibility(View.GONE);
            holder.modelForm.setVisibility(View.VISIBLE);

            holder.modelText.setText(String.format("ID: %s", model.getId()));

            holder.modelSwitch.setVisibility(View.INVISIBLE);
            holder.modelImage.setVisibility(View.INVISIBLE);
            switch (model.getId()) {
                case MeshConstants.MODEL_ID_GENERIC_ONOFF: {
                    holder.modelText.append(" On/Off");
                    if (model.hasAppKey()) {
                        holder.modelSwitch.setVisibility(View.VISIBLE);
                    }
                    break;
                }
                case MeshConstants.MODEL_ID_GENERIC_LEVEL: {
                    holder.modelText.append(" Generic Level");
                    break;
                }
                case MeshConstants.MODEL_ID_CTL: {
                    holder.modelText.append(" CTL");
                    if (model.hasAppKey()) {
                        holder.modelImage.setImageResource(R.drawable.ic_edit_24dp);
                        holder.modelImage.setVisibility(View.VISIBLE);
                    }
                    break;
                }
                case MeshConstants.MODEL_ID_HSL: {
                    holder.modelText.append(" HSL");
                    if (model.hasAppKey()) {
                        holder.modelImage.setImageResource(R.drawable.ic_color_lens_24dp);
                        holder.modelImage.setVisibility(View.VISIBLE);
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }
}
