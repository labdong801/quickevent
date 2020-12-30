package com.onlineafterhome.quickevent_protobuf;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.onlineafterhome.quickevent_protobuf.ipc.message.IPCProtoBuf;
import com.onlineafterhome.quickevnet.QuickEvent;
import com.onlineafterhome.quickevnet.util.L;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

/**
 * 使用ProtoBuf序列化的TcpIPCReceiver
 */
public class TcpIPCReceiver extends com.onlineafterhome.quickevnet.ipc.TcpIPCReceiver {
    public TcpIPCReceiver(int bufSize) {
        super(bufSize);
    }

    @Override
    protected byte[] ipcHandle(byte[] data, int size) throws UnsupportedEncodingException {
        try {
            IPCProtoBuf.IPCMessage msg = IPCProtoBuf.IPCMessage.parseFrom(data);

            if(msg != null && msg.getContent() != null && msg.getResponseClz() != null){
                Class clz = Class.forName(msg.getParamClz());
                Method method = clz.getMethod("parseFrom", byte[].class);
                Object params = method.invoke(null, msg.getContent().toByteArray());

                Object resp = QuickEvent.getDefault().request(params,
                        Class.forName(msg.getResponseClz()), false);

                if(resp instanceof GeneratedMessageV3) {
                    GeneratedMessageV3 o = (GeneratedMessageV3) resp;
                    IPCProtoBuf.IPCMessage msgResp = IPCProtoBuf.IPCMessage.newBuilder()
                            .setContent(ByteString.copyFrom(o.toByteArray()))
                            .setParamClz(msg.getParamClz())
                            .setResponseClz(msg.getResponseClz())
                            .build();

                    return msgResp.toByteArray();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.ipcHandle(data, size);
    }
}
