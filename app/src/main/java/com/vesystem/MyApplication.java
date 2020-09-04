package com.vesystem;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.os.StrictMode;

import com.vesystem.test.R;


/**
 * Created Date 2020/7/6.
 *
 * @author ChenRui
 * ClassDescription:
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()//检测Activity泄露
                .penaltyLog()//在Logcat中打印违规日志
                .build());
        /*if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }

        LeakCanary.install(this);*/
    }

    private String currentProcessName(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo runningAppProcess : manager.getRunningAppProcesses()) {
            if (runningAppProcess.pid == Process.myPid()) {
                return runningAppProcess.processName;
            }
        }
        return "";
    }
}
