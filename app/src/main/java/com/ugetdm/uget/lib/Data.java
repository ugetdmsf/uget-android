package com.ugetdm.uget.lib;

public class Data {
    public static final class Group {
        public static final int queuing   = 1 << 0;
        public static final int pause     = 1 << 1;
        public static final int active    = 1 << 2;
        public static final int completed = 1 << 3;
        public static final int upload    = 1 << 4;
        public static final int error     = 1 << 5;
        public static final int finished  = 1 << 6;
        public static final int recycled  = 1 << 7;
    }

    public static final class ProxyType {
        public static final int none    = 0;  // Don't use
        public static final int auto    = 1;
        public static final int http    = 2;
        public static final int socks4  = 3;
        public static final int socks5  = 4;
    }

    // JNI wrap functions
    public native static long    create();
    public native static void    ref(long dataPointer);
    public native static void    unref(long dataPointer);

    public native static boolean get(long dataPointer, Progress progressData);
    public native static boolean get(long dataPointer, Download downloadData);
    public native static boolean get(long dataPointer, Category categoryData);
    public native static void    set(long dataPointer, Download downloadData);
    public native static void    set(long dataPointer, Category categoryData);

    public native static int     getGroup(long dataPointer);
    public native static void    setGroup(long dataPointer, int state);
    public native static String  getName(long dataPointer);
    public native static void    setName(long dataPointer, String name);
    public native static void    setNameByUri(long dataPointer, String uri);

    public native static int     getPriority(long dataPointer);
    public native static void    setPriority(long dataPointer, int priority);

    public native static String  getMessage(long dataPointer);
}

