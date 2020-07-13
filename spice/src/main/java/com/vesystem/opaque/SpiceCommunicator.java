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

import com.vesystem.spice.interfaces.KSpiceConnect;
import com.vesystem.spice.interfaces.KSpiceConnectable;
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
public class SpiceCommunicator implements KSpiceConnectable {

//    private HashMap<String, Integer> deviceToFdMap = new HashMap<>();

//    private WeakReference<UsbManager> mWRUsbManager;
    //    UsbManager mUsbManager;
   /* private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: 广播接收器");
            String action = intent.getAction();
            if (RemoteClientLibConstants.ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    int vid = device.getVendorId();
                    int pid = device.getProductId();
                    String mapKey = vid + ":" + pid;
                    synchronized (Objects.requireNonNull(deviceToFdMap.get(mapKey))) {
                        Objects.requireNonNull(deviceToFdMap.get(mapKey)).notify();
                    }
                }
            }
        }
    };*/


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


    //TODO 断开连接崩溃问题
    public native void SpiceClientDisconnect();

    public native void SpiceButtonEvent(int x, int y, int metaState, int pointerMask, boolean rel);

    public native void SpiceKeyEvent(boolean keyDown, int virtualKeyCode);

    public native void UpdateBitmap(Bitmap bitmap, int x, int y, int w, int h);

    public native void SpiceRequestResolution(int x, int y);

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("spice");
    }

    final static int LCONTROL = 29;
    //    final static int RCONTROL = 285;
    final static int LALT = 56;
    final static int RALT = 312;
    final static int LSHIFT = 42;
    //    final static int RSHIFT = 54;
    final static int LWIN = 347;
//    final static int RWIN = 348;

    int remoteMetaState = 0;

    private int width = 0;
    private int height = 0;

    public boolean isConnectSucceed;
    public boolean isClickDisconnect;


//    private Thread thread = null;

//    private int lastRequestedWidth = -1;
//    private int lastRequestedHeight = -1;


    //    private static SpiceCommunicator myself = null;
    private static WeakReference<SpiceCommunicator> myself;
    //    private Bitmap bitmap = null;
//    private Handler handler;
//    private boolean isRequestingNewDisplayResolution;


    public SpiceCommunicator(Context context) {
        WeakReference<Context> wrContext = new WeakReference<>(context);
//        mWRUsbManager = new WeakReference<>((UsbManager) wrContext.get().getSystemService(Context.USB_SERVICE));
//        this.isRequestingNewDisplayResolution = res;
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
        Log.e(TAG, "connectSpice:ip: " + ip + ", port:" + port + ", password:" + password + ",tport:" + tport + ", " + cf + ", " + cs);
        SpiceClientConnect(ip, port, tport, password, cf, ca, cs, sound);
        EventBus.getDefault().post(new KMessageEvent(KMessageEvent.SPICE_CONNECT_FAILURE));
    }


    public void disconnect() {
        SpiceClientDisconnect();
        if (spiceConnect != null) {
            spiceConnect = null;
        }
        myself = null;
    }


    public void sendMouseEvent(int x, int y, int metaState, int pointerMask, boolean rel) {
        //android.util.Log.d(TAG, "sendMouseEvent: " + x +"x" + y + "," + "metaState: " +
        //                   metaState + ", pointerMask: " + pointerMask);
        SpiceButtonEvent(x, y, metaState, pointerMask, rel);
    }

    public void sendSpiceKeyEvent(boolean keyDown, int virtualKeyCode) {
        Log.i(TAG, "sendSpiceKeyEvent: down: " + keyDown + " code: " + virtualKeyCode);
        SpiceKeyEvent(keyDown, virtualKeyCode);
    }

    public int framebufferWidth() {
        return width;
    }

    public int framebufferHeight() {
        return height;
    }

    public void setFramebufferWidth(int w) {
        width = w;
    }

    public void setFramebufferHeight(int h) {
        height = h;
    }

    public String desktopName() {
        // TODO Auto-generated method stub
        return "";
    }

    public void requestUpdate(boolean incremental) {
        // TODO Auto-generated method stub

    }

    public void writeClientCutText(String text) {
        // TODO Auto-generated method stub

    }

    public String getEncoding() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public void writePointerEvent(int x, int y, int metaState, int pointerMask, boolean rel) {
        sendMouseEvent(x, y, metaState, pointerMask, rel);
       /* remoteMetaState = metaState;
        if ((pointerMask & RemotePointer.POINTER_DOWN_MASK) != 0)
            sendModifierKeys(true);
        sendMouseEvent(x, y, metaState, pointerMask, rel);
        if ((pointerMask & RemotePointer.POINTER_DOWN_MASK) == 0)
            sendModifierKeys(false);*/
    }

    private void sendModifierKeys(boolean keyDown) {
        Log.i(TAG, "sendModifierKeys: " + keyDown);
        /*if ((remoteMetaState & RemoteKeyboard.CTRL_ON_MASK) != 0) {
            Log.e("SpiceCommunicator", "Sending CTRL: " + LCONTROL + " down: " + keyDown);
            sendSpiceKeyEvent(keyDown, LCONTROL);
        }
        if ((remoteMetaState & RemoteKeyboard.ALT_ON_MASK) != 0) {
            Log.e("SpiceCommunicator", "Sending ALT: " + LALT + " down: " + keyDown);
            sendSpiceKeyEvent(keyDown, LALT);
        }
        if ((remoteMetaState & RemoteKeyboard.ALTGR_ON_MASK) != 0) {
            Log.e("SpiceCommunicator", "Sending ALTGR: " + RALT + " down: " + keyDown);
            sendSpiceKeyEvent(keyDown, RALT);
        }
        if ((remoteMetaState & RemoteKeyboard.SUPER_ON_MASK) != 0) {
            Log.e("SpiceCommunicator", "Sending SUPER: " + LWIN + " down: " + keyDown);
            sendSpiceKeyEvent(keyDown, LWIN);
        }
        if ((remoteMetaState & RemoteKeyboard.SHIFT_ON_MASK) != 0) {
            Log.e("SpiceCommunicator", "Sending SHIFT: " + LSHIFT + " down: " + keyDown);
            sendSpiceKeyEvent(keyDown, LSHIFT);
        }*/
    }

    public void writeKeyEvent(int key, int metaState, boolean keyDown) {
        if (keyDown) {
            remoteMetaState = metaState;
            sendModifierKeys(true);
        }

        //android.util.Log.d("SpiceCommunicator", "Sending scanCode: " + key + ". Is it down: " + keyDown);
        sendSpiceKeyEvent(keyDown, key);

        if (!keyDown) {
            sendModifierKeys(false);
            remoteMetaState = 0;
        }
    }

    public void writeSetPixelFormat(int bitsPerPixel, int depth,
                                    boolean bigEndian, boolean trueColour, int redMax, int greenMax,
                                    int blueMax, int redShift, int greenShift, int blueShift,
                                    boolean fGreyScale) {
        // TODO Auto-generated method stub

    }

    public void writeFramebufferUpdateRequest(int x, int y, int w, int h, boolean b) {
        // TODO Auto-generated method stub
    }

    public void close() {
        disconnect();
    }

    @Override
    public boolean isCertificateAccepted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCertificateAccepted(boolean certificateAccepted) {
        // TODO Auto-generated method stub
    }

    public void requestResolution(int x, int y) {
        requestResolution();
    }

    public void requestResolution() {
        /*int currentWidth = this.width;
        int currentHeight = this.height;
        if (isRequestingNewDisplayResolution &&
                lastRequestedWidth == -1 && lastRequestedHeight == -1) {
            wrCanvas.get().waitUntilInflated();
            lastRequestedWidth = wrCanvas.get().getDesiredWidth();
            lastRequestedHeight = wrCanvas.get().getDesiredHeight();
            if (currentWidth != lastRequestedWidth || currentHeight != lastRequestedHeight) {
                Log.d(TAG, "Requesting new res: " + lastRequestedWidth + "x" + lastRequestedHeight);
                SpiceRequestResolution(lastRequestedWidth, lastRequestedHeight);
            } else {
                Log.d(TAG, "Resolution request was satisfied.");
                lastRequestedWidth = -1;
                lastRequestedHeight = -1;
            }
        } else {
            Log.d(TAG, "Resolution request disabled or last request unsatisfied (resolution request loop?).");
            lastRequestedWidth = -1;
            lastRequestedHeight = -1;
        }*/
    }

    /* Callbacks from jni and corresponding non-static methods */


   /* public static int openUsbDevice(int vid, int pid) throws InterruptedException {
        if (myself == null || myself.get() == null) return -1;
        Log.i(TAG, "Attempting to open a USB device and return a file descriptor.");

        if (!myself.get().usbEnabled) {
            return -1;
        }

        String mapKey = vid + ":" + pid;

        myself.get().deviceToFdMap.put(mapKey, 0);

        boolean deviceFound = false;
        UsbDevice device = null;
        HashMap<String, UsbDevice> stringDeviceMap;
        int timeout = RemoteClientLibConstants.usbDeviceTimeout;
        while (!deviceFound && timeout > 0) {
            stringDeviceMap = myself.get().mWRUsbManager.get().getDeviceList();
            Collection<UsbDevice> usbDevices = stringDeviceMap.values();

            for (UsbDevice ud : usbDevices) {
                Log.i(TAG, "DEVICE: " + ud.toString());
                if (ud.getVendorId() == vid && ud.getProductId() == pid) {
                    Log.i(TAG, "USB device successfully matched.");
                    deviceFound = true;
                    device = ud;
                    break;
                }
            }
            timeout -= 100;
            SystemClock.sleep(100);
        }

        int fd = -1;
        // If the device was located in the Java layer, we try to open it, and failing that
        // we request permission and wait for it to be granted or denied, or for a timeout to occur.
        if (device != null) {
            UsbDeviceConnection deviceConnection = myself.get().mWRUsbManager.get().openDevice(device);
            if (deviceConnection != null) {
                fd = deviceConnection.getFileDescriptor();
            } else {
                // Request permission to access the device.

                synchronized (Objects.requireNonNull(myself.get().deviceToFdMap.get(mapKey))) {
                    PendingIntent mPermissionIntent = PendingIntent.getBroadcast(myself.get().wrContext.get(), 0, new Intent(RemoteClientLibConstants.ACTION_USB_PERMISSION), 0);

                    // TODO: Try putting this intent filter into the activity in the manifest file.
//                    IntentFilter filter = new IntentFilter(RemoteClientLibConstants.ACTION_USB_PERMISSION);
//                    myself.get().wrContext.get().registerReceiver(myself.get().mUsbReceiver, filter);
                    Log.i(TAG, "openUsbDevice: 注册成功");
                    myself.get().mWRUsbManager.get().requestPermission(device, mPermissionIntent);
                    // Wait for permission with a timeout. 
                    Objects.requireNonNull(myself.get().deviceToFdMap.get(mapKey)).wait(RemoteClientLibConstants.usbDevicePermissionTimeout);

                    deviceConnection = myself.get().mWRUsbManager.get().openDevice(device);
                    if (deviceConnection != null) {
                        fd = deviceConnection.getFileDescriptor();
                    }
                }
            }
        }
        return fd;
    }*/

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


    /*private static void AddVm(String vmname) {
        if (myself == null || myself.get() == null) return;
        Log.d(TAG, "Adding VM: " + vmname + "to list of VMs");
        myself.get().vmNames.add(vmname);
    }*/

    public void onSettingsChanged(int width, int height, int bpp) {
        Log.i(TAG, "onSettingsChanged called, wxh: " + width + "x" + height);
        setFramebufferWidth(width);
        setFramebufferHeight(height);
//        wrCanvas.get().reallocateDrawable(width, height);
//        EventBus.getDefault().post(new MessageEvent(SPICE_GET_W_H, new BitmapAttr(width, height)));
//        EventBus.getDefault().post(new MessageEvent(SPICE_CONNECT_SUCCESS));
        if (myself == null || myself.get() == null) return;
        if (myself.get().spiceConnect != null) {
            if (!isConnectSucceed) {
                isConnectSucceed = true;
                myself.get().spiceConnect.onConnectSucceed();
            }
            myself.get().spiceConnect.onUpdateBitmapWH(width, height);
        }

        /*if (isRequestingNewDisplayResolution) {
            requestResolution();
        }*/
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

    private KSpiceConnect spiceConnect;

    public void setSpiceConnect(KSpiceConnect spiceConnect) {
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
//        EventBus.getDefault().post(new MessageEvent(MessageEvent.SPICE_BITMAP_UPDATE, new BitmapAttr(x, y, width, height)));
       /* if (myself == null || myself.get() == null) return;
        if (bitmap != null) {
            synchronized (myself.get().wrCanvas.get()) {
                myself.get().UpdateBitmap(bitmap, x, y, width, height);
            }
            myself.get().wrCanvas.get().reDraw(x, y, width, height);
        }*/
        //myself.onGraphicsUpdate(x, y, width, height);
    }
    /* END Callbacks from jni and corresponding non-static methods */

    private static void OnMouseUpdate(int x, int y) {
        Log.i(TAG, "OnMouseUpdate: X:" + x + ",Y:" + y);
        if (myself == null || myself.get() == null) return;
        //android.util.Log.i(TAG, "OnMouseUpdate called: " + x +", " + y);
        if (myself.get().spiceConnect != null) {
            myself.get().spiceConnect.onMouseUpdate(x, y);
        }
//        EventBus.getDefault().post(new MessageEvent(MessageEvent.SPICE_MOUSE_UPDATE, new MouseAttr(x, y)));
//        myself.get().wrCanvas.get().setMousePointerPosition(x, y);
    }

    private static void OnMouseMode(boolean relative) {
        if (myself == null || myself.get() == null) return;
        Log.i(TAG, "OnMouseMode called, relative: " + relative);
//        myself.get().wrCanvas.get().mouseMode(relative);
//        EventBus.getDefault().post(new MessageEvent(MessageEvent.SPICE_MOUSE_MODE_UPDATE, relative));
        if (myself.get().spiceConnect != null) {
            myself.get().spiceConnect.onMouseMode(relative);
        }
    }
}
