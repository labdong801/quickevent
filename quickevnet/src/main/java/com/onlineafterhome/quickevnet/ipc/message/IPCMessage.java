package com.onlineafterhome.quickevnet.ipc.message;

public class IPCMessage {
    private String content;
    private String paramClz;
    private String responseClz;

    public IPCMessage(String content, String paramClz, String responseClz) {
        this.content = content;
        this.paramClz = paramClz;
        this.responseClz = responseClz;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getParamClz() {
        return paramClz;
    }

    public void setParamClz(String paramClz) {
        this.paramClz = paramClz;
    }

    public String getResponseClz() {
        return responseClz;
    }

    public void setResponseClz(String responseClz) {
        this.responseClz = responseClz;
    }
}
