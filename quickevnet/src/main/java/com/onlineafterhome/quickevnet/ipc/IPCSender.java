package com.onlineafterhome.quickevnet.ipc;

import com.onlineafterhome.quickevnet.ipc.message.IPCEvent;

public interface IPCSender {
    public void init();
    public <T> T request(Object object, Class<T> t);
    public void post(Object object);
    public Object handleEvent(IPCEvent event);
}
