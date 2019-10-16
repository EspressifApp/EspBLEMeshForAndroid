package com.espressif.espblemesh.ui.network.node;

import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Node;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.app.MeshApp;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.espblemesh.eventbus.GattConnectionEvent;
import com.espressif.espblemesh.model.MeshConnection;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class ModelBindGroupActivity extends BaseActivity {
    private Model mModel;
    private Node mNode;

    private MeshConnection mMeshConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.model_bind_group_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        MeshApp app = MeshApp.getInstance();
        Intent intent = getIntent();
        mModel = (Model) app.takeCache(intent.getStringExtra(Constants.KEY_MODEL));
        mNode = (Node) app.takeCache(intent.getStringExtra(Constants.KEY_NODE));
        mMeshConnection = MeshConnection.Instance;

        RecyclerView groupView = findViewById(R.id.recycler_view);
        @SuppressWarnings("unchecked")
        List<Group> bindGroups = (List<Group>) app.takeCache(intent.getStringExtra(Constants.KEY_BIND_GROUPS));
        GroupBindAdapter adapter = new GroupBindAdapter(bindGroups);
        groupView.setAdapter(adapter);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGattConnectionEvent(GattConnectionEvent event) {
        if (!event.isConnected()) {
            finish();
        }
    }

    class GroupBindHolder extends RecyclerView.ViewHolder {
        Group group;

        TextView text1;
        TextView text2;

        GroupBindHolder(@NonNull View itemView) {
            super(itemView);

            text1 = itemView.findViewById(R.id.text1);
            text2 = itemView.findViewById(R.id.text2);

            itemView.setOnClickListener(v -> {
                mMeshConnection.modelSubscriptionAdd(mNode, mModel, group.getAddress());
                finish();
            });
        }
    }

    class GroupBindAdapter extends RecyclerView.Adapter<GroupBindHolder> {
        private LayoutInflater mInflater = getLayoutInflater();
        private List<Group> groups;

        GroupBindAdapter(List<Group> groups) {
            this.groups = groups;
        }

        @NonNull
        @Override
        public GroupBindHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.model_bind_group_item, parent, false);
            return new GroupBindHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupBindHolder holder, int position) {
            Group group = groups.get(position);

            holder.group = group;
            holder.text1.setText(group.getName());
            holder.text2.setText(String.format("%04X", group.getAddress()));
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }
    }
}
