package com.onlineafterhome.quickevent;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.onlineafterhome.quickevent.bean.Account;
import com.onlineafterhome.quickevent.bean.DeviceIDMessage;
import com.onlineafterhome.quickevent.bean.LoginResult;
import com.onlineafterhome.quickevnet.QuickEvent;
import com.onlineafterhome.quickevnet.QuickService;
import com.onlineafterhome.quickevnet.Subscribe;
import com.onlineafterhome.quickevnet.ThreadMode;
import com.onlineafterhome.quickevnet.util.L;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QuickEvent.getDefault().init();
        setContentView(R.layout.activity_main);
        startService(new Intent(this, RemoteService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        QuickEvent.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        QuickEvent.getDefault().unregister(this);
    }

    public void testSend(View view){

        (new Thread(new Runnable() {
            @Override
            public void run() {
                final Account account = new Account("admin", "123456");
                final LoginResult ret = QuickEvent.getDefault().request(account, LoginResult.class);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(ret != null)
                            Toast.makeText(MainActivity.this, ret.getMessage(), Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Not found server", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        })).start();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void testReceive(String msg){
        L.v("["+ Thread.currentThread().getName() +"] Receive message:" + msg);
    }

    @QuickService
    public String testService(Integer a){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "hello " + a;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showID(DeviceIDMessage message){
        TextView idText = findViewById(R.id.deviceid);
        idText.setText(message.getAndroidId());
    }
}
