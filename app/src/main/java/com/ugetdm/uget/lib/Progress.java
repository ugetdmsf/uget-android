package com.ugetdm.uget.lib;

public class Progress {
    public long    complete;
    public long    total;
    public long    consumeTime;  // Elapsed (seconds)
    public long    remainTime;   // Left    (seconds)
    public int     downloadSpeed;
    public int     uploadSpeed;
    public int     percent;
    public int     retryCount;
}
