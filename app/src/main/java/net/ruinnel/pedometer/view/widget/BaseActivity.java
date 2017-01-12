/*
 * Filename	: BaseActivity.java
 * Function	:
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.view.widget;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import net.ruinnel.pedometer.Pedometer;
import net.ruinnel.pedometer.R;

public class BaseActivity extends AppCompatActivity {
  private static final String TAG = BaseActivity.class.getSimpleName();

  public interface OnBackPressedListener {
    void onBack();
  }

  protected Pedometer mApp;
  protected Snackbar mSnackbar;

  protected Fragment mCurrentFragment;
  protected int mCurrentIdx;

  private OnBackPressedListener mListener;

  public void showSnackbar(int strResId) {
    showSnackbar(getString(strResId), null);
  }

  public void showSnackbar(String str) {
    showSnackbar(str, null);
  }

  public void showSnackbar(int strResId, View.OnClickListener listener) {
    showSnackbar(getString(strResId), listener);
  }

  public void showSnackbar(CharSequence msg, final View.OnClickListener listener) {
    if (mSnackbar != null) {
      mSnackbar.dismiss();
    }
    View view = getWindow().findViewById(android.R.id.content);
    mSnackbar = Snackbar.make(view, msg, Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.close, new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (listener == null) {
          mSnackbar.dismiss();
        } else {
          listener.onClick(view);
        }
      }
    });
    mSnackbar.setDuration(Snackbar.LENGTH_LONG);
    mSnackbar.show();
  }

  public void setOnBackPressedListener(OnBackPressedListener listener) {
    mListener = listener;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApp = (Pedometer) getApplication();
  }

  @Override
  public void onBackPressed() {
    if (mListener != null) {
      mListener.onBack();
    } else {
      super.onBackPressed();
    }
  }
}
