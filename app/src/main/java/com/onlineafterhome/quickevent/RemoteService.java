package com.onlineafterhome.quickevent;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.onlineafterhome.quickevent.bean.Account;
import com.onlineafterhome.quickevent.bean.Add;
import com.onlineafterhome.quickevent.bean.AddResult;
import com.onlineafterhome.quickevent.bean.DeviceIDMessage;
import com.onlineafterhome.quickevent.bean.LoginResult;
import com.onlineafterhome.quickevnet.QuickEvent;
import com.onlineafterhome.quickevnet.QuickService;

public class RemoteService extends Service {
    public RemoteService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("wbj", "Remote Service Start");
        QuickEvent.getDefault().init();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        QuickEvent.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        QuickEvent.getDefault().unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @QuickService
    public LoginResult login(Account account){
        QuickEvent.getDefault().post("welcome");
        QuickEvent.getDefault().post(new DeviceIDMessage(Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID)));
        if(account.getUsername().equals("admin") &&
                account.getPassword().equals("123456")){
            return new LoginResult(0,"Login Success");
        }else{
            return new LoginResult(-1, "Username or Password incorrect!");
        }
    }

    @QuickService
    public Login.LoginResult login(Login.Account account){
//        QuickEvent.getDefault().post("welcome");
        Login.DeviceIDMessage deviceIDMessage = Login.DeviceIDMessage.newBuilder()
                .setAndroidId(Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
                .build();
        QuickEvent.getDefault().post(deviceIDMessage);
        if(account.getUsername().equals("admin") &&
                account.getPassword().equals("123456")){
            return Login.LoginResult.newBuilder()
                    .setCode(0)
                    .setMessage("Login Success (ProtoBuf)").build();
        }else{
            return Login.LoginResult.newBuilder()
                    .setCode(-1)
                    .setMessage("Username or Password incorrect! (ProtoBuf)").build();
        }
    }

    @QuickService
    public AddResult add(Add add){
        return new AddResult(add.getA() + add.getB());
    }
}
