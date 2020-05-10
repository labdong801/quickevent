package com.onlineafterhome.quickevnet.ipc;

import java.util.concurrent.atomic.AtomicBoolean;

public class QuickIPCSender implements IPCSender {

    private static QuickIPCSender sInstance = null;
    public static QuickIPCSender getInstance(){
        if(sInstance == null){
            synchronized (QuickIPCSender.class){
                if(sInstance == null)
                    sInstance = new QuickIPCSender();
            }
        }
        return sInstance;
    }

    private QuickIPCSender(){

    }


    private AtomicBoolean isInit = new AtomicBoolean(false);
    private IPCSender mProxy = null;

    @Override
    public void init() {
        mProxy = new TcpIPCSender();
        mProxy.init();
        isInit.set(true);
    }

    @Override
    public <T> T request(Object object, Class<T> t) {
        if(!isInit.get())
            return null;

        return mProxy.request(object, t);
    }

    @Override
    public void post(Object object) {
        if(!isInit.get())
            return;

        mProxy.post(object);
    }
}
