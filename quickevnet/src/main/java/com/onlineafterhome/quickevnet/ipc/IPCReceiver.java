package com.onlineafterhome.quickevnet.ipc;

import com.google.gson.Gson;
import com.onlineafterhome.quickevnet.QuickEvent;
import com.onlineafterhome.quickevnet.ipc.message.IPCMessage;
import com.onlineafterhome.quickevnet.util.L;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IPCReceiver implements ITCPProtocol {
    // 缓冲区的长度
    private int bufSize;

    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    public IPCReceiver(int bufSize){
        this.bufSize = bufSize;
    }

    //服务端信道已经准备好了接收新的客户端连接
    public void handleAccept(SelectionKey key) throws IOException {
        SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
        clntChan.configureBlocking(false);
        //将选择器注册到连接到的客户端信道，并指定该信道key值的属性为OP_READ，同时为该信道指定关联的附件
        clntChan.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufSize));
    }

    //客户端信道已经准备好了从信道中读取数据到缓冲区
    public void handleRead(final SelectionKey key) throws IOException{
        final SocketChannel clntChan = (SocketChannel) key.channel();
        //获取该信道所关联的附件，这里为缓冲区
        //ByteBuffer buf = (ByteBuffer) key.attachment();
        ByteBuffer buf = ByteBuffer.allocate(bufSize);
        int bytesRead = -1;
        StringBuilder stringBuilder = new StringBuilder();
        while((bytesRead = clntChan.read(buf)) > 0){
            //如果缓冲区总读入了数据，则将该信道感兴趣的操作设置为为可读可写
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            buf.flip();
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();
            String tmp = decoder.decode(buf.asReadOnlyBuffer()).toString();
            stringBuilder.append(tmp);

        }

        final String value = stringBuilder.toString();
        if(value.length() > 0){
            // 接收到数据
            L.v("IPCReceiver:" + value);
            mThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    String resp = ipcHandle(value);
                    try {
                        if (resp != null) {
                            clntChan.write(ByteBuffer.wrap(resp.getBytes()));
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                    }
                }
            });
        }else{
            clntChan.close();
        }
    }

    //客户端信道已经准备好了将数据从缓冲区写入信道
    public void handleWrite(SelectionKey key) throws IOException {
//        //获取与该信道关联的缓冲区，里面有之前读取到的数据
//        ByteBuffer buf = (ByteBuffer) key.attachment();
//        //重置缓冲区，准备将数据写入信道
//        buf.flip();
//        SocketChannel clntChan = (SocketChannel) key.channel();
//        //将数据写入到信道中
//        clntChan.write(buf);
//        if (!buf.hasRemaining()){
//            //如果缓冲区中的数据已经全部写入了信道，则将该信道感兴趣的操作设置为可读
//            key.interestOps(SelectionKey.OP_READ);
//        }
//        //为读入更多的数据腾出空间
//        buf.compact();
    }

    private String ipcHandle(String value){
        Gson gson = new Gson();
        try {
            IPCMessage msg = gson.fromJson(value, IPCMessage.class);
            Object params = gson.fromJson(msg.getContent(), Class.forName(msg.getParamClz()));
            Object resp = QuickEvent.getDefault().request(params,
                    Class.forName(msg.getResponseClz()), false);

            if(resp != null){
                msg.setContent(gson.toJson(resp));
                return gson.toJson(msg);
            }
        }catch (Throwable e){
            L.e(e);
        }
        return gson.toJson(new IPCMessage(null, null, null));
    }
}
