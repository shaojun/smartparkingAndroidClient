package com.SmartParking.UI;

import java.util.ArrayList;
import java.util.List;

import com.SmartParking.Sampling.ScannedBleDevice;

public class ExpandableListViewItem {

	private List<ExpandableListViewItem> childItemList = new ArrayList<ExpandableListViewItem>();
	public final String DisplayName;

	public ExpandableListViewItem(String displayName) {
		this.DisplayName = displayName;
	}

	public void addChildItem(ExpandableListViewItem childItem) {
		this.childItemList.add(childItem);
	}

	public List<ExpandableListViewItem> getChildItemList() {
		return this.childItemList;
	}

	@Override
	public String toString() {
		return this.DisplayName;
	}

	@Override
	public int hashCode() {
		return this.DisplayName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ExpandableListViewItem))
			return false;

		ExpandableListViewItem target = (ExpandableListViewItem) obj;
		return this.DisplayName.hashCode() == target.DisplayName.hashCode();
	}
}
