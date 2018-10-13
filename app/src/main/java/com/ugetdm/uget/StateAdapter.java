package com.ugetdm.uget;

import com.ugetdm.uget.lib.*;
import android.content.Context;
//import android.util.Log;
import android.view.*;
import android.widget.*;
import java.lang.Integer;

public class StateAdapter extends BaseAdapter implements SpinnerAdapter {
    protected long      nodePointer;
    protected String[]  stateNames;
    protected Context   context = null;

    protected static int[]  imageIds = {
            android.R.drawable.star_off,                // all
            android.R.drawable.ic_media_play,           // active
            android.R.drawable.ic_media_pause,          // queuing
            android.R.drawable.stat_sys_download_done,  // finished
            android.R.drawable.ic_menu_delete,          // recycled
    };

    protected static int[]  stateValue = {
            0,
            Node.Group.active,
            Node.Group.queuing,
            Node.Group.finished,
            Node.Group.recycled,
    };

//	iButton.setImageDrawable (context.getResources().getDrawable(
//	android.R.drawable.picture_frame));
//android.R.drawable.picture_frame
//android.R.drawable.ic_delete
//android.R.drawable.ic_menu_upload

//Active    - android.R.drawable.ic_media_play
//Queuing   - android.R.drawable.ic_popup_sync
//Pause     - android.R.drawable.ic_media_pause
//Warning   - android.R.drawable.ic_dialog_alert
//Warning   - android.R.drawable.stat_sys_warning
//Info      - android.R.drawable.ic_dialog_info
//Upload    - android.R.drawable.stat_sys_upload
//Finished  - android.R.drawable.stat_sys_download_done
//Recycled  - android.R.drawable.ic_menu_delete
//Batch A-Z - android.R.drawable.ic_menu_sort_alphabetically

//
//android.R.drawable.ic_menu_preferences

    StateAdapter (Context context, long nodePointer)
    {
        this.context = context;
        this.nodePointer = nodePointer;
        this.stateNames = context.getResources().getStringArray(R.array.cnode_state);
    }

    @Override
    public int getCount ()
    {
        return stateNames.length;
    }

    @Override
    public Object getItem (int position)
    {
        return null;
    }

    @Override
    public long getItemId (int position)
    {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        // getViewTypeCount() must > getItemViewType()
        // return value must start with 0.
        // e.g. 0, 1, 2, 3...
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        // getViewTypeCount() must > getItemViewType()
        return 1;
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent)
    {
        int       nChildren;
        long      currentNode;
        TextView  textViewQuantity;
        TextView  textView;
        ImageView imageView;
        LayoutInflater  lInflater;

        if (convertView == null) {
            lInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = lInflater.inflate (R.layout.category_item, parent, false);
        }

        textViewQuantity = (TextView) convertView.findViewById (R.id.cnode_quantity);
        textView = (TextView) convertView.findViewById (R.id.cnode_name);
        imageView = (ImageView) convertView.findViewById (R.id.cnode_image);

        textView.setText (stateNames[position]);
        imageView.setImageResource(imageIds[position]);
        textView.setPadding (3,3,3,3);

        if (position == 0)
            currentNode = nodePointer;
        else
            currentNode = Node.getFakeByGroup(nodePointer, stateValue[position]);

        if (currentNode != 0) {
            nChildren = Node.nChildren(currentNode);
            textViewQuantity.setText(Integer.toString(nChildren));

            // Queuing is NOT empty
            if (position == 2 && nChildren > 0)
                imageView.setImageResource(android.R.drawable.ic_popup_sync);
        }
        else
            textViewQuantity.setText("0");

        return convertView;
    }

}

