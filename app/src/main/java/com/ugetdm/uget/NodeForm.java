/*
 *
 *   Copyright (C) 2018-2019 by C.H. Huang
 *   plushuang.tw@gmail.com
 */

package com.ugetdm.uget;

import android.os.Bundle;
import android.view.*;
//import android.util.Log;
//import android.widget.*;
import android.app.Fragment;
//import android.app.Activity;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentActivity;

public class NodeForm extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container,
	        Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.node_form, container, false);
	}

	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}
}

