package com.vesystem;

import android.app.Application;
import android.os.StrictMode;


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
}
