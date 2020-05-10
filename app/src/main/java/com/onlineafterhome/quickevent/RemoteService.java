package com.onlineafterhome.quickevent;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.onlineafterhome.quickevent.bean.Account;
import com.onlineafterhome.quickevent.bean.Add;
import com.onlineafterhome.quickevent.bean.AddResult;
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
        if(account.getUsername().equals("admin") &&
                account.getPassword().equals("123456")){
            return new LoginResult(0,"Login Success");
        }else{
            return new LoginResult(-1, "Username or Password incorrect!");
        }
    }

    @QuickService
    public AddResult add(Add add){
        return new AddResult(add.getA() + add.getB());
    }
}
