package com.ugetdm.uget;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.app.Fragment;
import android.app.Activity;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentActivity;

public class CategoryFilter extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container,
	        Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.category_filter, container, false);
	}

	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		ListView listView;
		Activity activity;

		activity = this.getActivity();
		listView = (ListView) activity.findViewById(R.id.category_listview);
		listView.setAdapter(MainActivity.app.categoryAdapter);
//		listView.setItemsCanFocus(true);
//		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setItemChecked(0, true);
		listView = (ListView) activity.findViewById(R.id.state_listview);
		listView.setAdapter(MainActivity.app.stateAdapter);
//		listView.setItemsCanFocus(true);
//		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setItemChecked(0, true);
	}
}

