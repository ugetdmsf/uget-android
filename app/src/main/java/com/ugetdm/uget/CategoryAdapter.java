/*
 *
 *   Copyright (C) 2018-2019 by C.H. Huang
 *   plushuang.tw@gmail.com
 */

package com.ugetdm.uget;

import com.ugetdm.uget.lib.*;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.lang.Integer;

public class CategoryAdapter extends BaseAdapter implements SpinnerAdapter {
    protected long    pointer;
    protected long    pointerGlobal;
    protected Context context = null;

    CategoryAdapter (Context context, long nodePointer, long pointerGlobal)
    {
        this.context = context;
        this.pointer = nodePointer;
        this.pointerGlobal = pointerGlobal;
    }

    @Override
    public int getCount ()
    {
        int  count;

        count = Node.nChildren (pointer) + 1;
        return count;
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
        long      nodePointer;
        long      infoPointer;
        TextView  textViewQuantity;
        TextView  textView;
        ImageView       imageView;
        LayoutInflater  lInflater;

        if (convertView == null) {
            lInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = lInflater.inflate (R.layout.category_item, parent, false);
        }

        textViewQuantity = (TextView) convertView.findViewById (R.id.cnode_quantity);
        textView = (TextView) convertView.findViewById (R.id.cnode_name);
        imageView = (ImageView) convertView.findViewById (R.id.cnode_image);

//        imageView.setImageResource(android.R.drawable.picture_frame);
        imageView.setImageResource(R.mipmap.ic_action_category);
        textView.setPadding (3,3,3,3);

        if (position == 0) {
            nodePointer = Node.getNthChild(pointerGlobal, 0);
            textView.setText (R.string.cnode_total);
        }
        else {
            nodePointer = Node.getNthChild(pointer, position - 1);
            infoPointer = Node.info(nodePointer);
            textView.setText (Info.getName(infoPointer));
        }
        textViewQuantity.setText(Integer.toString(Node.nChildren(nodePointer)));

        return convertView;
    }

}

