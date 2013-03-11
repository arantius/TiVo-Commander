package com.arantius.tivocommander.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.LinearLayout;

// http://stackoverflow.com/questions/3192595/android-listview-that-does-not-scroll/5646121#5646121
// Like a ListView, but it does not implicitly scroll.
public class LinearListView extends LinearLayout {
  private class Observer extends DataSetObserver {
    LinearListView context;

    public Observer(LinearListView context) {
      this.context = context;
    }

    @Override
    public void onChanged() {
      List<View> oldViews = new ArrayList<View>(context.getChildCount());

      for (int i = 0; i < context.getChildCount(); i++)
        oldViews.add(context.getChildAt(i));

      Iterator<View> iter = oldViews.iterator();

      context.removeAllViews();

      for (int i = 0; i < context.adapter.getCount(); i++) {
        View convertView = iter.hasNext() ? iter.next() : null;
        context.addView(context.adapter.getView(i, convertView, context));
      }
      super.onChanged();
    }

    @Override
    public void onInvalidated() {
      context.removeAllViews();
      super.onInvalidated();
    }
  }

  Adapter adapter;

  Observer observer = new Observer(this);

  public LinearListView(Context context) {
    super(context);
  }

  public LinearListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setAdapter(Adapter adapter) {
    if (this.adapter != null)
      this.adapter.unregisterDataSetObserver(observer);

    this.adapter = adapter;
    adapter.registerDataSetObserver(observer);
    observer.onChanged();
  }
}
