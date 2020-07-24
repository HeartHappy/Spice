/*
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */


package com.vesystem.opaque;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.vesystem.spice.interfaces.ISpiceConnect;
import com.vesystem.spice.model.KMessageEvent;

import org.freedesktop.gstreamer.GStreamer;
import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;


/**
 * Created Date: 2020/7/7
 *
 * @author ChenRui
 * ClassDescription：该类包名、类名不能改动
 */
public class SpiceCommunicator {


    private final static String TAG = "SpiceCommunicator";

    public native int FetchVmNames(String URI, String user, String password, String sslCaFile, boolean sslStrict);

    public native int CreateOvirtSession(String uri,
                                         String user,
                                         String password,
                                         String sslCaFile,
                                         boolean sound, boolean sslStrict);

    public native int StartSessionFromVvFile(String fileName, boolean sound);

    public native int SpiceClientConnect(String ip,
                                         String port,
                                         String tport,
                                         String password,
                                         String ca_file,
                                         String ca_cert,
                                         String cert_subj,
                                         boolean sound);


    public native void SpiceClientDisconnect();

    public native void SpiceButtonEvent(int x, int y, int metaState, int pointerMask, boolean rel);

    public native void SpiceKeyEvent(boolean keyDown, int virtualKeyCode);

    public native void UpdateBitmap(Bitmap bitmap, int x, int y, int w, int h);

    public native void SpiceRequestResolution(int x, int y);

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("spice");
    }


    public boolean isConnectSucceed;
    public boolean isClickDisconnect;


    private static WeakReference<SpiceCommunicator> myself;


    public SpiceCommunicator(Context context) {
        WeakReference<Context> wrContext = new WeakReference<>(context);
        myself = new WeakReference<>(this);
        try {
            new GStreamer(wrContext.get());
        } catch (Exception e) {
            e.printStackTrace();
            EventBus.getDefault().post(new KMessageEvent(KMessageEvent.SPICE_CONNECT_FAILURE, e.getMessage()));
        }
    }

    /**
     * Launches a new thread which performs a plain SPICE connection.
     */
    public void connectSpice(String ip, String port, String tport, String password, String cf, String ca, String cs, boolean sound) {
//        Log.e(TAG, "connectSpice:ip: " + ip + ", port:" + port + ", password:" + password + ",tport:" + tport + ", " + cf + ", " + cs);
        SpiceClientConnect(ip, port, tport, password, cf, ca, cs, sound);
        //退出时被释放内存，需要空判
        if (spiceConnect != null) {
            spiceConnect.onConnectFail();
        }
    }


    public void disconnect() {
        SpiceClientDisconnect();
        if (spiceConnect != null) {
            spiceConnect = null;
        }
        myself = null;
    }

    public void sendSpiceKeyEvent(boolean keyDown, int virtualKeyCode) {
//        Log.i(TAG, "sendSpiceKeyEvent: down: " + keyDown + " code: " + virtualKeyCode);
        SpiceKeyEvent(keyDown, virtualKeyCode);
    }


    public void writePointerEvent(int x, int y, int metaState, int pointerMask, boolean rel) {
        Log.i(TAG, "sendMouseEvent: " + x + "x" + y + "," + "metaState: " +
                metaState + ", pointerMask: " + pointerMask + ",rel:" + rel);
        SpiceButtonEvent(x, y, metaState, pointerMask, rel);
        if (myself.get().spiceConnect != null) {
            myself.get().spiceConnect.onMouseUpdate(x, y);
        }
    }


    public static void sendMessage(int message) {
//        if (myself == null || myself.get() == null) return;
        Log.i(TAG, "sendMessage called with message: " + message);
        EventBus.getDefault().post(new KMessageEvent(message));
    }

    public static void sendMessageWithText(int message, String messageText) {
        Log.i(TAG, "sendMessageWithText: " + messageText);
       /* if (myself == null || myself.get() == null) return;
        Log.d(TAG, "sendMessage called with message: " + messageText);
        Bundle b = new Bundle();
        b.putString("message", messageText);
        Message m = new Message();
        m.what = message;
        m.setData(b);*/
        EventBus.getDefault().post(new KMessageEvent(message));
    }


    public void onSettingsChanged(int width, int height, int bpp) {
        Log.i(TAG, "onSettingsChanged called, wxh: " + width + "x" + height);
        if (myself == null || myself.get() == null) return;
        if (myself.get().spiceConnect != null) {
            if (!isConnectSucceed) {
                isConnectSucceed = true;
                myself.get().spiceConnect.onConnectSucceed();
            }
            myself.get().spiceConnect.onUpdateBitmapWH(width, height);
        }
    }

    private static void OnSettingsChanged(int inst, int width, int height, int bpp) {
//        Log.i(TAG, "OnSettingsChanged: inst:" + inst);
        if (myself == null || myself.get() == null) return;
        myself.get().onSettingsChanged(width, height, bpp);
    }

    private Bitmap bitmap;

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    private ISpiceConnect spiceConnect;

    public void setSpiceConnect(ISpiceConnect spiceConnect) {
        this.spiceConnect = spiceConnect;
    }

    private static void OnGraphicsUpdate(int inst, int x, int y, int width, int height) {
        //android.util.Log.i(TAG, "OnGraphicsUpdate called: " + x +", " + y + " + " + width + "x" + height );
        if (myself == null || myself.get() == null || myself.get().bitmap == null) {
            return;
        }
        synchronized (myself.get().bitmap) {
//            Log.i(TAG, "OnGraphicsUpdate: 更新Bitmap");
            myself.get().UpdateBitmap(myself.get().bitmap, x, y, width, height);
        }
        if (myself.get().spiceConnect != null) {
            myself.get().spiceConnect.onUpdateBitmap(x, y, width, height);
        }
    }

    private static void OnMouseUpdate(int x, int y) {
        Log.i(TAG, "OnMouseUpdate: X:" + x + ",Y:" + y);
        if (myself == null || myself.get() == null) return;
        //android.util.Log.i(TAG, "OnMouseUpdate called: " + x +", " + y);
        if (myself.get().spiceConnect != null) {
            myself.get().spiceConnect.onMouseUpdate(x, y);
        }
    }

    private static void OnMouseMode(boolean relative) {
        if (myself == null || myself.get() == null) return;
        Log.i(TAG, "OnMouseMode called, relative: " + relative);
//        myself.get().wrCanvas.get().mouseMode(relative);
        if (myself.get().spiceConnect != null) {
            myself.get().spiceConnect.onMouseMode(relative);
        }
    }
}
