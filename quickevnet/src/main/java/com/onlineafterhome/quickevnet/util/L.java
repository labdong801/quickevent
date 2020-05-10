package com.onlineafterhome.quickevnet.util;

import android.util.Log;

import com.onlineafterhome.quickevnet.BuildConfig;

/**
 * Log Util
 */
public class L {

    private static final String TAG = "[QuickEvent]";

    private static boolean DEBUG_ON = BuildConfig.DEBUG;

    public static void v(String msg){
        if(DEBUG_ON) Log.v(TAG, msg);
    }

    public static void e(String msg){
        if(DEBUG_ON) Log.e(TAG, msg);
    }

    public static void e(Throwable e){
        if(DEBUG_ON) e.printStackTrace();
    }

    public static void e(String msg, Throwable e){
        if(DEBUG_ON) Log.e(TAG, msg);
        if(DEBUG_ON) e.printStackTrace();
    }
}
