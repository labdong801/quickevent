package com.onlineafterhome.quickevnet.ipc.message;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class IPCMessageTest {


    @Test
    public void getName() {
        IPCMessage a = new IPCMessage("wbj",String.class.toString(),String.class.toString());
        Gson gson = new Gson();
        String json = gson.toJson(a);
        assertEquals("{\"content\":\"wbj\",\"paramClz\":\"class java.lang.String\",\"responseClz\":\"class java.lang.String\"}", json);
        System.out.println(json);

        IPCMessage b = gson.fromJson(json, IPCMessage.class);
        assertEquals("wbj", b.getContent());
        System.out.println(b.getContent());
    }
}