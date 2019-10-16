package com.espressif.espblemesh.ui.network.node;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.espressif.blemesh.model.Model;
import com.espressif.blemesh.model.Node;
import com.espressif.espblemesh.model.MeshConnection;

public class OperationFragment extends Fragment {
    protected Model mModel;
    protected Node mNode;

    protected MeshConnection mMeshConnection;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMeshConnection = MeshConnection.Instance;
    }

    public void setArgs(Model model, Node node) {
        mModel = model;
        mNode = node;
    }
}
