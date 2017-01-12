/*
 * Filename	: ViewPagerAdapter.java
 * Comment 	:
 * History	: 2017/01/12, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.view.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import com.google.common.collect.Lists;
import net.ruinnel.pedometer.R;
import net.ruinnel.pedometer.view.fragment.HistoryFragment;
import net.ruinnel.pedometer.view.fragment.MainFragment;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewPagerAdapter extends FragmentPagerAdapter {
  private static final String TAG = ViewPagerAdapter.class.getSimpleName();

  private List<Class<? extends Fragment>> mFragments = Lists.newArrayList();
  private List<String> mTitles = Lists.newArrayList();

  private Map<Integer, Fragment> mInstances = new HashMap<Integer, Fragment>();

  public ViewPagerAdapter(Context context, FragmentManager fm) {
    super(fm);

    mFragments.add(MainFragment.class);
    mFragments.add(HistoryFragment.class);

    mTitles.add(context.getString(R.string.tab_main));
    mTitles.add(context.getString(R.string.tab_history));
  }

  @Override
  public int getCount() {
    return mFragments.size();
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return mTitles.get(position);
  }

  @Override
  public Fragment getItem(int position) {
    Class<? extends Fragment> fragmentClass = mFragments.get(position);
    Fragment fragment = null;
    Constructor<? extends Fragment> constructor;
    try {
      fragment = mInstances.get(position);
      if (fragment == null) {
        constructor = fragmentClass.getConstructor();
        fragment = constructor.newInstance();
        mInstances.put(position, fragment);
      }

    } catch (Exception e) {
      Log.w(TAG, "fragment load fail!", e);
    }

    return fragment;
  }
}
