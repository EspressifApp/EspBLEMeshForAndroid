package com.espressif.espblemesh.ui.network;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.espressif.blemesh.model.Group;
import com.espressif.blemesh.model.Network;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.blemesh.task.GroupAddTask;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import libs.espressif.utils.TextUtils;

public class NetworkNewGroupActivity extends BaseActivity {
    private Network mNetwork;

    private View mProgressView;
    private View mContentForm;

    private EditText mGroupNameET;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_group_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        long netKeyIndex = getIntent().getLongExtra(Constants.KEY_NETWORK_INDEX, -1);
        mNetwork = MeshUser.Instance.getNetworkForKeyIndex(netKeyIndex);

        mProgressView = findViewById(R.id.progress);
        mContentForm = findViewById(R.id.content_form);

        mGroupNameET = findViewById(R.id.group_name);

        Button okBtn = findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(v -> {
            String name = mGroupNameET.getText().toString();
            if (TextUtils.isEmpty(name)) {
                mGroupNameET.setError(getString(R.string.group_name_error));
            } else {
                mGroupNameET.setError(null);
                showProgress(true);
                Observable.just(new GroupAddTask(name, mNetwork))
                        .subscribeOn(Schedulers.io())
                        .map(GroupAddTask::run)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Group>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                            }

                            @Override
                            public void onNext(Group group) {
                                showProgress(false);
                                Intent intent = new Intent();
                                intent.putExtra(Constants.KEY_GROUP_ADDRESS, group.getAddress());
                                setResult(RESULT_OK, intent);
                                finish();
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onComplete() {
                            }
                        });

            }
        });
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
            mContentForm.setVisibility(View.GONE);
        } else {
            mProgressView.setVisibility(View.GONE);
            mContentForm.setVisibility(View.VISIBLE);
        }
    }
}
