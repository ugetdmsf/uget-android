package com.ugetdm.uget.lib;

import android.content.ContentResolver;

public class Util {
    public native static String stringFromIntUnit(long value, int is_speed);
    public native static String stringFromSeconds(int  seconds, int limit_99_99_99);
}
