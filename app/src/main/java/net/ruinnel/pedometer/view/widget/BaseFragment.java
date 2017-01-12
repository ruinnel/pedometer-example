/*
 * Filename	: BaseFragment.java
 * Comment 	:
 * History	: 2017/01/12, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.view.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import net.ruinnel.pedometer.Pedometer;
import net.ruinnel.pedometer.R;

public abstract class BaseFragment extends Fragment {
  private static final String TAG = BaseFragment.class.getSimpleName();

  protected View mView;
  protected Pedometer mApp;

  private Snackbar mSnackbar;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApp = (Pedometer) getActivity().getApplication();
  }

  @Override
  public void onResume() {
    super.onResume();
    // 화면 이동 시 키보드를 닫음
    if (getActivity() != null) {
      InputMethodManager im = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (im != null) {
        if (getView() != null) {
          im.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
      }
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (mView != null) {
      ViewGroup parent = (ViewGroup) mView.getParent();
      if (parent != null) {
        parent.removeView(mView);
      }
    }
  }

  protected void removeView() {
    if (mView != null) {
      ViewGroup parent = (ViewGroup) mView.getParent();
      if (parent != null) {
        parent.removeView(mView);
      }
    }
  }

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
    View view = getActivity().getWindow().findViewById(android.R.id.content);
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
}
