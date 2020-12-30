package com.onlineafterhome.quickevent_protobuf;

import android.util.Base64;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.onlineafterhome.quickevent_protobuf.ipc.message.IPCProtoBuf;
import com.onlineafterhome.quickevnet.ipc.message.IPCEvent;
import com.onlineafterhome.quickevnet.util.L;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

/**
 * 使用ProtoBuf序列化的TcpIPCSender
 */
public class TcpIPCSender extends com.onlineafterhome.quickevnet.ipc.TcpIPCSender{

    @Override
    protected <T> T deSerialize(byte[] response, int size) throws ClassNotFoundException, UnsupportedEncodingException {
        // 使用 ProtoBuf 反序列化
        try {
            byte[] temp = new byte[size];
            System.arraycopy(response, 0, temp, 0, size);
            IPCProtoBuf.IPCMessage msg = IPCProtoBuf.IPCMessage.parseFrom(temp);
            if(msg != null && msg.getContent() != null && msg.getResponseClz() != null){
                L.v("wbj  ProtoBuf TcpIPCSender deSerialize");
                Class clz = Class.forName(msg.getResponseClz());
                Method method = clz.getMethod("parseFrom", byte[].class);
                return (T) method.invoke(null, msg.getContent().toByteArray());
            }
        } catch (Exception e) {
        }

        return super.deSerialize(response, size);
    }

    @Override
    protected <T> byte[] serialize(Object object, String responseClz) {
        // 使用 ProtoBuf 序列化
        if(object instanceof GeneratedMessageV3) {
            L.v("wbj  ProtoBuf TcpIPCSender serialize");
            GeneratedMessageV3 o = (GeneratedMessageV3) object;
            IPCProtoBuf.IPCMessage msg = IPCProtoBuf.IPCMessage.newBuilder()
                    .setContent(ByteString.copyFrom(o.toByteArray()))
                    .setParamClz(object.getClass().getName())
                    .setResponseClz(responseClz)
                    .build();
            return msg.toByteArray();
        }
        // 使用 Json 序列化
        return super.serialize(object, responseClz);
    }

    @Override
    protected TcpIPCReceiver createProtocol() {
        return new TcpIPCReceiver(BUFSIZE);
    }

    @Override
    public void post(Object object) {
        if(object instanceof GeneratedMessageV3) {
            L.v("wbj  ProtoBuf TcpIPCSender post serialize");
            GeneratedMessageV3 o = (GeneratedMessageV3) object;
            // TODO 暂时使用Base64
            IPCEvent event = new IPCEvent(Base64.encodeToString(o.toByteArray(), Base64.DEFAULT), object.getClass().getName());
            request(event, Integer.class);
            return;
        }
        super.post(object);
    }

    @Override
    public Object handleEvent(IPCEvent event) {

        try {
            Class clz = Class.forName(event.getClz());
            Method method = clz.getMethod("parseFrom", byte[].class);
            return method.invoke(null, Base64.decode(event.getContent(),Base64.DEFAULT));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.handleEvent(event);
    }
}
