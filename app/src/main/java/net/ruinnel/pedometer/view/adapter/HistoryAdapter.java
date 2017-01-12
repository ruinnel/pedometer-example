/*
 * Filename	: HistoryAdapter.java
 * Comment 	:
 * History	: 2017/01/12, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.view.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.ruinnel.pedometer.R;
import net.ruinnel.pedometer.db.bean.History;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.util.Utils;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by ruinnel on 2017. 1. 12..
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
  private static final String TAG = HistoryAdapter.class.getSimpleName();

  private final Context mContext;
  private List<History> mHistories;
  private final SimpleDateFormat mFormater;
  private final int mStrides;

  public HistoryAdapter(Context context, List<History> histories, int strides) {
    mContext = context;
    mStrides = strides;
    mHistories = histories;
    mFormater = new SimpleDateFormat("yyyy.MM.dd");
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
    return new ViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    History item = mHistories.get(position);

    holder.txtDay.setText(mFormater.format(item.day));
    holder.txtSteps.setText(String.valueOf(item.steps));
    double distance = Utils.getDistance(item.steps, mStrides);
    Log.d(TAG, "steps = " + item.steps + ", strides = " + mStrides + ", distance = " + distance);
    holder.txtDistance.setText(String.format(mContext.getString(R.string.format_distance), distance));
  }

  @Override
  public int getItemCount() {
    return mHistories != null ? mHistories.size() : 0;
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private TextView txtDay;
    private TextView txtSteps;
    private TextView txtDistance;

    ViewHolder(View itemView) {
      super(itemView);

      //background = itemView.getBackground();
      txtDay = (TextView) itemView.findViewById(R.id.txt_day);
      txtSteps = (TextView) itemView.findViewById(R.id.txt_steps);
      txtDistance = (TextView) itemView.findViewById(R.id.txt_distance);
    }
  }
}
