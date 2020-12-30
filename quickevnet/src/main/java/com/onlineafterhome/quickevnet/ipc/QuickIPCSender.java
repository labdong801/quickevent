package com.onlineafterhome.quickevnet.ipc;

import com.google.gson.Gson;
import com.onlineafterhome.quickevnet.ipc.message.IPCEvent;

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
        mProxy = createIPCSender();
        mProxy.init();
        isInit.set(true);
    }

    /**
     * 创建 IPC传输
     * @return
     */
    private IPCSender createIPCSender() {
        try {
            // Protobuf 版本的 IPCSender
            Class clz = Class.forName("com.onlineafterhome.quickevent_protobuf.TcpIPCSender");
            return (IPCSender) clz.newInstance();
        } catch (Exception e) {
        }
        return new TcpIPCSender();
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

    @Override
    public Object handleEvent(IPCEvent event){
        if(!isInit.get())
            return null;

        return mProxy.handleEvent(event);
    }
}
