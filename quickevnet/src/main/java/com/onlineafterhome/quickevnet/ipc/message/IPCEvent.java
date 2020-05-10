package com.onlineafterhome.quickevnet.ipc.message;

public class IPCEvent {
    private String content;
    private String Clz;

    public IPCEvent(String content, String clz) {
        this.content = content;
        Clz = clz;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getClz() {
        return Clz;
    }

    public void setClz(String clz) {
        Clz = clz;
    }
}
