package com.SmartParking.UI;

import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class ExpandableListViewAdapter extends BaseExpandableListAdapter {

	private Context mContext;
	private List<ExpandableListViewItem> parentItems;

	public ExpandableListViewAdapter(Context context,
			List<ExpandableListViewItem> parentItems) {
		super();

		mContext = context;
		this.parentItems = parentItems;
	}

	@Override
	public String getChild(int groupPosition, int childPosition) {
		// return mContents[groupPosition][childPosition];
		return this.parentItems.get(groupPosition).getChildItemList()
				.get(childPosition).toString();
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		TextView row = (TextView) convertView;
		if (row == null) {
			row = new TextView(mContext);
		}
		
		row.setTextSize(10);
		row.setText("	" + this.getChild(groupPosition, childPosition));
		return row;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		// return mContents[groupPosition].length;
		return this.parentItems.get(groupPosition).getChildItemList().size();
	}

	@Override
	public String[] getGroup(int groupPosition) {
		// return mContents[groupPosition];
		ArrayList nameList = new ArrayList();
		for (ExpandableListViewItem pi : this.parentItems) {
			nameList.add(pi.toString());
		}
		return (String[]) nameList.toArray();
	}

	@Override
	public int getGroupCount() {
		// return mContents.length;
		return this.parentItems.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		TextView row = (TextView) convertView;
		if (row == null) {
			row = new TextView(mContext);
		}

		row.setTextSize(15);
		row.setTypeface(Typeface.DEFAULT_BOLD);
		// row.setText(mTitles[groupPosition]);
		row.setText(this.parentItems.get(groupPosition).toString());
		return row;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
