/*
 * Filename	: HistoryFragment.java
 * Comment 	:
 * History	: 2017/01/12, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.common.collect.Lists;
import net.ruinnel.pedometer.R;
import net.ruinnel.pedometer.Settings;
import net.ruinnel.pedometer.db.DatabaseManager;
import net.ruinnel.pedometer.db.bean.History;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.view.adapter.HistoryAdapter;
import net.ruinnel.pedometer.view.widget.BaseFragment;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by ruinnel on 2017. 1. 12..
 */
public class HistoryFragment extends BaseFragment {
  private static final String TAG = HistoryFragment.class.getSimpleName();

  private static final int LOAD_ONCE = 20;

  @Inject
  Settings mSettings;

  @Inject
  DatabaseManager mDbManager;

  @BindView(R.id.recycler_view)
  RecyclerView mRecyclerView;

  private List<History> mHistories;
  private HistoryAdapter mAdapter;
  private LinearLayoutManager mLayoutManager;

  // for indicate recycler view scroll..
  private boolean mNowLoading;
  private int mOffset;

  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (Settings.ACTION_STEP.equals(action)) {
        // 오늘 데이터만 변경
        if (mHistories != null && mHistories.size() > 0) {
          History history = mDbManager.todayHistory();
          mHistories.get(0).steps = history.steps;
          mAdapter.notifyDataSetChanged();
        }
      }
    }
  };

  private void loadNext() {
    List<History> temp = mDbManager.getHistories(mOffset, LOAD_ONCE);
    if (temp != null && !temp.isEmpty()) {
      mOffset = mOffset + LOAD_ONCE;
      mHistories.addAll(temp);
      mRecyclerView.post(new Runnable() {
        @Override
        public void run() {
          mAdapter.notifyDataSetChanged();
        }
      });
      mNowLoading = false;
    } else {
      showSnackbar(R.string.reach_list_last_data);
    }

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    if (mView == null) {
      mView = inflater.inflate(R.layout.frag_history, container, false);
    } else {
      removeView();
    }

    mApp.component().inject(this);
    ButterKnife.bind(this, mView);

    mNowLoading = false;
    mOffset = 0;
    mHistories = Lists.newArrayList();
    mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    mAdapter = new HistoryAdapter(getContext(), mHistories, mSettings.getStrides());
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setLayoutManager(mLayoutManager);
    mRecyclerView.setAnimation(null);
    mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(RecyclerView recyclerView, int newState) {}

      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (dy > 0) { //check for scroll down
          int visibleItemCount = mLayoutManager.getChildCount();
          int totalItemCount = mLayoutManager.getItemCount();
          int pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

          if (!mNowLoading) {
            if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
              Log.v(TAG, "reach last item!");
              mNowLoading = true;
              loadNext();
            }
          }
        }
      }
    });

    loadNext();

    return mView;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (!getUserVisibleHint()) {
      return;
    }

    // TODO fragment가 보여질때 실행할 로직.
    registerReceiver();
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (isVisibleToUser && isResumed()) {
      onResume();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unregisterReceiver();
  }

  private void registerReceiver() {
    unregisterReceiver();
    IntentFilter filter = new IntentFilter(Settings.ACTION_STEP);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
  }

  private void unregisterReceiver() {
    try {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    } catch (Exception e) {
      Log.w(TAG, "unregisterReceiver fail.");
    }
  }
}
