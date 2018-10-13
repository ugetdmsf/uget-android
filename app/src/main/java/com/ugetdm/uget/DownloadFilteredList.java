package com.ugetdm.uget;

import android.os.Bundle;
import android.view.*;
import android.app.Activity;
import android.app.Fragment;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentActivity;

public class DownloadFilteredList extends Fragment {
    boolean  dualPane;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.download_filtered_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        View      view;
        Activity  activity;

        activity = getActivity();
        view = activity.findViewById(R.id.category_filter);
        dualPane = view != null && view.getVisibility() == View.VISIBLE;

//		if (savedInstanceState != null) {
        // Restore last state for checked position.
//			mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
//		}

        if (dualPane) {
            view = activity.findViewById(R.id.state_spinner);
            if (view != null)
                view.setVisibility(View.GONE);
            view = activity.findViewById(R.id.category_spinner);
            if (view != null)
                view.setVisibility(View.GONE);
        }
    }
}

