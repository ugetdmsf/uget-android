package com.ugetdm.uget;

import com.ugetdm.uget.lib.*;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.*;
import android.widget.*;

import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;

public class DownloadAdapter extends BaseAdapter {
    protected long    pointer;
    protected Context context = null;
    // setMinimumWidth()
    protected int     percentMinWidth = 0;
    protected int     retryMinWidth = 0;
    protected int     speedMinWidth = 0;
    protected int     sizeMinWidth = 0;

    DownloadAdapter (Context context, long nodePointer)
    {
        this.context = context;
        this.pointer = nodePointer;
    }

    @Override
    public int getCount ()
    {
        int  count;

        count = Node.nChildren(pointer);
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
        long       nodePointer;
        long       dataPointer;
        int        state = 0;
        Progress   progress = null;
        boolean    hasProgress = false;
        boolean    hasMessage = false;
        String     message = null;
        String     name = null;
        ProgressBar    progressBar;
        TextView       textView;
        ImageView      imageView;


        if (convertView == null) {
            LayoutInflater  lInflater;

            lInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = lInflater.inflate (R.layout.download_item, parent, false);
        }

        nodePointer = Node.getNthChild(pointer, position);
        if (nodePointer != 0) {
            dataPointer = Node.data(nodePointer);
            state = Info.getGroup(dataPointer);
            name = Info.getName(dataPointer);
            message = Info.getMessage(dataPointer);
            // get progress info
            progress = new Progress();
            hasProgress = Info.get(dataPointer, progress);
        }

        // get imageView(status icon) & textView(name)
        imageView = (ImageView) convertView.findViewById(R.id.dnode_image);
        textView = (TextView) convertView.findViewById(R.id.dnode_name);
        if (nodePointer == 0) {
            textView.setText("Removed");
            imageView.setImageResource(android.R.drawable.ic_delete);
            return convertView;
        }

        // status icon
        if ((state & Node.Group.finished) > 0)
            imageView.setImageResource (android.R.drawable.stat_sys_download_done);
        else if ((state & Node.Group.recycled) > 0)
            imageView.setImageResource (android.R.drawable.ic_menu_delete);
        else if ((state & Node.Group.pause) > 0)
            imageView.setImageResource (android.R.drawable.ic_media_pause);
        else if ((state & Node.Group.error) > 0)
            imageView.setImageResource (android.R.drawable.stat_notify_error);
        else if ((state & Node.Group.upload) > 0)
            imageView.setImageResource (android.R.drawable.stat_sys_upload);
        else if ((state & Node.Group.queuing) > 0)
            imageView.setImageResource (android.R.drawable.presence_invisible);
        else if ((state & Node.Group.active) > 0)
            imageView.setImageResource (android.R.drawable.ic_media_play);
        else //  if (state == 0)    //  == Node.Group.queuing
            imageView.setImageResource (android.R.drawable.presence_invisible);
//        else
//            imageView.setImageResource (0);

        // ------------------------------------------------
        // line 1: name + retry count

        // name
        textView.setText(name);

        // retry count
        textView = (TextView) convertView.findViewById(R.id.dnode_retry);
        String retryString = context.getResources().getString(R.string.dnode_retry);
        if (progress.retryCount == 0) {
//          textView.setVisibility(View.GONE);
            textView.setText("");
        }
        else {
//          textView.setVisibility(View.VISIBLE);
            if (progress.retryCount > 99)
                textView.setText(' ' + retryString + ">99");
            else
                textView.setText(' ' + retryString + ":" + Integer.toString(progress.retryCount));
            if (retryMinWidth == 0)
                retryMinWidth = calcTextWidth(textView, ' ' + retryString + ">999");  // + '9'
//          textView.setLayoutParams(new LinearLayout.LayoutParams(retryMinWidth,
//                  LinearLayout.LayoutParams.WRAP_CONTENT));
//          textView.setMinimumWidth(retryMinWidth);
            textView.getLayoutParams().width = retryMinWidth;
            textView.requestLayout();
        }

        // ------------------------------------------------
        // line 2: progress bar + percent

        // progress bar
        progressBar = (ProgressBar) convertView.findViewById(R.id.dnode_progress);
        progressBar.setProgress((int) progress.percent);
        progressBar.getProgressDrawable().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);

        // percent
        textView = (TextView) convertView.findViewById(R.id.dnode_percent);
        if (progress.total > 0 && progress.percent <= 100)
            textView.setText(Integer.toString(progress.percent) + '%');
        else
            textView.setText("");
//          textView.setText(String.format("%1$.1f%%", progress.percent));
        if (percentMinWidth == 0)
            percentMinWidth = calcTextWidth(textView, "0000%");  // + '0'
//      textView.setLayoutParams(new LinearLayout.LayoutParams(percentMinWidth,
//              LinearLayout.LayoutParams.WRAP_CONTENT));
//      textView.setMinimumWidth(percentMinWidth);
        textView.getLayoutParams().width = percentMinWidth;
        textView.requestLayout();

        // ------------------------------------------------
        // line 3: (message + size) or (speed + time left + size)

        if ((state & Node.Group.error) != 0 ) {
            // message
            if (message != null) {
                hasMessage = true;
                // show message
                textView = (TextView) convertView.findViewById(R.id.dnode_message);
                textView.setVisibility(View.VISIBLE);
//                textView.setSelected(true);    // android:ellipsize="marquee"
                textView.setText(message);
                // clear speed
                textView = (TextView) convertView.findViewById(R.id.dnode_speed);
                textView.setVisibility(View.GONE);
                textView.setText("");
                // clear time left
                textView = (TextView) convertView.findViewById(R.id.dnode_left);
                textView.setVisibility(View.GONE);
                textView.setText("");  // + ' '
            }
        }
        else {
            // clear message
            textView = (TextView) convertView.findViewById(R.id.dnode_message);
            textView.setVisibility(View.GONE);
//            textView.setSelected(false);    // android:ellipsize="marquee"
            textView.setText("");
            // speed
            textView = (TextView) convertView.findViewById(R.id.dnode_speed);
            textView.setVisibility(View.VISIBLE);
            if ((state & Node.Group.active) == 0)
                textView.setText("");
            else
                textView.setText(Util.stringFromIntUnit(progress.downloadSpeed, 1));
            if (speedMinWidth == 0)
                speedMinWidth = calcTextWidth(textView, "00000 WiB/s");  // + '0'
//          textView.setTextColor(textView.getResources().getColor(android.R.color.primary_text_dark));
//          textView.setTextColor(textView.getResources().getColor(android.R.color.white));
            textView.getLayoutParams().width = speedMinWidth;
            textView.requestLayout();

            // time left
            textView = (TextView) convertView.findViewById(R.id.dnode_left);
            textView.setVisibility(View.VISIBLE);
            if ((state & Node.Group.active) == 0 || progress.remainTime == 0)
                textView.setText("");
            else {
                String timeLeftString = Util.stringFromSeconds((int) progress.remainTime, 1);
//              if (timeLeftString.startsWith("00:"))
//                  timeLeftString.replace("00:", "   ");
                textView.setText(timeLeftString + " " +
                        context.getResources().getString(R.string.dnode_left));
            }
        }

        // size
        String sizeText;
        int    sizeTextWidth;
        textView = (TextView) convertView.findViewById(R.id.dnode_size);
        if (progress.total == 0) {
            sizeTextWidth = 0;
            textView.setText("");
        }
        else if (progress.total == progress.complete) {
            sizeText = Util.stringFromIntUnit(progress.total, 0);
            sizeTextWidth = calcTextWidth(textView, sizeText);
            textView.setText(sizeText);
        }
        else {
            sizeText = Util.stringFromIntUnit(progress.complete, 0) + " / " +
                    Util.stringFromIntUnit(progress.total, 0);
            sizeTextWidth = calcTextWidth(textView, sizeText);
            textView.setText(sizeText);
        }
        // adjust width of size field
        if (hasMessage) {
            if (progress.total == 0)
                textView.setVisibility(View.GONE);
            else {
                textView.getLayoutParams().width = sizeTextWidth;
                textView.requestLayout();
            }
        }
        else {
            textView.setVisibility(View.VISIBLE);
            if (sizeMinWidth == 0)
                sizeMinWidth = calcTextWidth(textView, "00000 WiB / 00000 WiB");  // + '0' + '0'
            textView.getLayoutParams().width = sizeMinWidth;
            textView.requestLayout();
        }

        progress = null;

//      iButton.setImageDrawable (context.getResources().getDrawable(
//              android.R.drawable.picture_frame));
// android.R.drawable.picture_frame
// android.R.drawable.ic_delete
// android.R.drawable.ic_menu_upload


// Queuing   - android.R.drawable.ic_popup_sync
// Active    - android.R.drawable.ic_media_play
// Pause     - android.R.drawable.ic_media_pause
// Warning   - android.R.drawable.ic_dialog_alert
// Warning   - android.R.drawable.stat_sys_warning
// Info      - android.R.drawable.ic_dialog_info
// Upload    - android.R.drawable.stat_sys_upload
// Finished  - android.R.drawable.stat_sys_download_done
// Recycled  - android.R.drawable.ic_menu_delete
// Batch A-Z - android.R.drawable.ic_menu_sort_alphabetically

//
// android.R.drawable.ic_menu_preferences
        return convertView;
    }


    public int calcTextWidth(TextView textView, String text)
    {
        Paint paint = new Paint();
        Rect bounds = new Rect();

        paint.setTypeface(textView.getTypeface());
        float textSize = textView.getTextSize();
        paint.setTextSize(textSize);
        paint.getTextBounds(text, 0, text.length(), bounds);

        return bounds.width();
    }

    /*
    public void correctWidth(TextView textView, int desiredWidth)
    {
        Paint paint = new Paint();
        Rect bounds = new Rect();

        paint.setTypeface(textView.getTypeface());
        float textSize = textView.getTextSize();
        paint.setTextSize(textSize);
        String text = textView.getText().toString();
        paint.getTextBounds(text, 0, text.length(), bounds);

        while (bounds.width() > desiredWidth)
        {
            textSize--;
            paint.setTextSize(textSize);
            paint.getTextBounds(text, 0, text.length(), bounds);
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }
    */
}

