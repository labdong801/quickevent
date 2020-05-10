package com.onlineafterhome.quickevnet.ipc;

public interface IPCSender {
    public void init();
    public <T> T request(Object object, Class<T> t);
    public void post(Object object);
}
