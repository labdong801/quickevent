package com.onlineafterhome.quickevnet.ipc;

import android.os.Looper;

import com.google.gson.Gson;
import com.onlineafterhome.quickevnet.ipc.message.IPCEvent;
import com.onlineafterhome.quickevnet.ipc.message.IPCMessage;
import com.onlineafterhome.quickevnet.util.L;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpIPCSender implements IPCSender, Runnable {
    private static final int BUFSIZE = 256;
    private static final int TIMEOUT = 3000;
    private static final String FILE_PRE = "QuickEvent-";
    private static final String FILE_STUFX = "PORT-";

    private ExecutorService mThreadPool = null;
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private String mPortFile;

    public TcpIPCSender() {
        deleteCashPortFiles();
    }

    @Override
    public void run() {
        try{
            Selector selector = Selector.open();
            ServerSocketChannel listenChannel = ServerSocketChannel.open();
            listenChannel.socket().bind(new InetSocketAddress(0));
            final int localPort = listenChannel.socket().getLocalPort();
            File portFile = File.createTempFile(FILE_PRE, FILE_STUFX + localPort);
            mPortFile = portFile.getAbsolutePath();
            L.v("Listen:" + mPortFile);
            listenChannel.configureBlocking(false);
            //将选择器注册到各个信道
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);

            ITCPProtocol protocol = new IPCReceiver(BUFSIZE);

            while (isRunning.get()){
                if (selector.select(TIMEOUT) == 0){
                    L.v(".");
                    continue;
                }

                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
                while (keyIter.hasNext()){
                    SelectionKey key = keyIter.next();
                    if (key.isAcceptable()){
                        protocol.handleAccept(key);
                    }
                    if (key.isReadable()){
                        protocol.handleRead(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        protocol.handleWrite(key);
                    }
                    keyIter.remove();
                }

            }
        }catch (Throwable e){
            L.e(e);
        }
    }

    @Override
    public void init() {
        isRunning.set(true);
        mThreadPool = Executors.newFixedThreadPool(1);
        mThreadPool.submit(this);
    }


    @Override
    public <T> T request(Object object, Class<T> t) {
        // IPC can not in main thread
        if(isMainThread())
            return null;

        List<Integer> localPorts = getLocalPorts();
        for(int port : localPorts){
            L.v("find port:" + port);
            Gson gson = new Gson();
            try {
                Socket soket = new Socket("127.0.0.1", port);
                soket.setSoTimeout(30 * 1000);
                IPCMessage msg = new IPCMessage(gson.toJson(object), object.getClass().getName(), t.getName());
                InputStream inputStream = soket.getInputStream();
                OutputStream outputStream = soket.getOutputStream();
                outputStream.write(gson.toJson(msg).getBytes());
                outputStream.flush();
                byte[] buffer = new byte[1024];
                StringBuilder stringBuilder = new StringBuilder();
                while (true){
                    if(inputStream.available() > 0){
                        int len = soket.getInputStream().read(buffer);
                        stringBuilder.append(new String(buffer, 0, len,"UTF-8"));
                        if(len < 1024){
                            break;
                        }
                    }
                }
                L.v("remote response:" + stringBuilder.toString());

                inputStream.close();
                outputStream.close();
                soket.close();


                msg = gson.fromJson(stringBuilder.toString(), IPCMessage.class);
                if(msg != null && msg.getContent() != null && msg.getResponseClz() != null){
                    return (T) gson.fromJson(msg.getContent(), Class.forName(msg.getResponseClz()));
                }
            } catch (Throwable e) {
                L.e(e);
            }
        }

        return null;
    }

    @Override
    public void post(Object object) {
        Gson gson = new Gson();
        IPCEvent event = new IPCEvent(gson.toJson(object), object.getClass().getName());
        request(event, Integer.class);
    }

    /**
     * @return
     */
    private boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private void deleteCashPortFiles(){
        try {
            File file = File.createTempFile(FILE_PRE, "clean").getParentFile();
            if(file != null) {
                for (File f : file.listFiles()) {

                    String [] nameSlipt = f.getName().split(FILE_STUFX);
                    if(nameSlipt.length == 2){
                        try {
                            int port = Integer.parseInt(nameSlipt[1]);
                            // Check this port
                            if(!checkPortVaild(port)){
                                f.delete();
                            }
                        }catch (Throwable e){
                            L.e(e);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            L.e(e);
        }

    }

    private List<Integer> getLocalPorts(){
        List<Integer> lists = new ArrayList<>();
        File portFile = new File(mPortFile);
        if(mPortFile != null){
            try {
                for (File file : portFile.getParentFile().listFiles()) {
                    if (file.getName().equals(portFile.getName()))
                        continue;

                    String[] nameSlipt = file.getName().split(FILE_STUFX);
                    if (nameSlipt.length == 2) {
                        int port = Integer.parseInt(nameSlipt[1]);
                        lists.add(port);
                    }
                }
            }catch (Throwable e){
                L.e(e);
            }
        }

        return lists;
    }

    private boolean checkPortVaild(int port){
        try {
            DatagramSocket  socket = new DatagramSocket(port);
            return true;
        } catch (Throwable e) {

        }
        return false;
    }

}
