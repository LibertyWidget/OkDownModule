package com.okdown.utils;

import android.util.Log;

public class OkLog extends Exception {
    private static final long serialVersionUID = -8641198158155821498L;

    private static boolean isLogEnable = false;

    public static void e(String msg) {
        e("OkLog_tag", msg);
    }

    public static void e(String tag, String msg) {
        if (isLogEnable) Log.e(tag, msg);
    }

    public static void printStackTrace(Throwable t) {
        if (isLogEnable && t != null) t.printStackTrace();
    }

    public OkLog(String detailMessage) {
        super(detailMessage);
    }

    public static OkLog UNKNOWN() {
        return new OkLog("unknown exception!");
    }

    public static OkLog BREAKPOINT_NOT_EXIST() {
        return new OkLog("breakpoint file does not exist!");
    }

    public static OkLog BREAKPOINT_EXPIRED() {
        return new OkLog("breakpoint file has expired!");
    }

    public static OkLog NOT_AVAILABLE() {
        return new OkLog("SDCard isn't available, please check SD card and permission: WRITE_EXTERNAL_STORAGE, and you must pay attention to Android6.0 RunTime Permissions!");
    }

    public static OkLog NET_ERROR() {
        return new OkLog("network error! http response code is 404 or 5xx!");
    }
}
