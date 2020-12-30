package com.onlineafterhome.quickevnet.ipc;

import android.os.Looper;

import com.google.gson.Gson;
import com.onlineafterhome.quickevnet.ipc.message.IPCEvent;
import com.onlineafterhome.quickevnet.ipc.message.IPCMessage;
import com.onlineafterhome.quickevnet.util.L;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
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
    protected static final int BUFSIZE = 256;
    protected static final int TIMEOUT = 3000;
    protected static final String FILE_PRE = "QuickEvent-";
    protected static final String FILE_STUFX = "PORT-";

    private ExecutorService mThreadPool = Executors.newFixedThreadPool(1);;
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private String mPortFile;

    public TcpIPCSender() {
    }

    @Override
    public void run() {
        try{
            Selector selector = Selector.open();
            ServerSocketChannel listenChannel = ServerSocketChannel.open();
            listenChannel.socket().bind(new InetSocketAddress(0));
            final int localPort = listenChannel.socket().getLocalPort();

            listenChannel.configureBlocking(false);
            //将选择器注册到各个信道
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);

            ITCPProtocol protocol = createProtocol();

            // 写入文件
            File portFile = File.createTempFile(FILE_PRE, FILE_STUFX + localPort);
            mPortFile = portFile.getAbsolutePath();
            L.v("Listen:" + mPortFile);

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

    protected TcpIPCReceiver createProtocol() {
        return new TcpIPCReceiver(BUFSIZE);
    }

    @Override
    public void init() {
        isRunning.set(true);
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                deleteCashPortFiles();
            }
        });
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
            try {
                Socket soket = new Socket("127.0.0.1", port);
                soket.setSoTimeout(30 * 1000);
                InputStream inputStream = soket.getInputStream();
                OutputStream outputStream = soket.getOutputStream();
                outputStream.write(serialize(object, t.getName()));
                outputStream.flush();
                byte[] buffer = new byte[1024];
                int sum = 0;
                //StringBuilder stringBuilder = new StringBuilder();
                while (true){
                    if(inputStream.available() > 0){
                        int len = soket.getInputStream().read(buffer, sum, 1024);
                        sum += len;
                        //stringBuilder.append(new String(buffer, 0, len,"UTF-8"));
                        if(len < 1024){
                            break;
                        }else{
                            int newSize = buffer.length + 1024;
                            byte[] newBuffer = new byte[newSize];
                            System.arraycopy(buffer, 0,newBuffer, 0, sum);
                            buffer = newBuffer;
                        }
                    }
                }

                final String response = new String(buffer, 0, sum,"UTF-8");

                L.v("remote response:" + response);

                inputStream.close();
                outputStream.close();
                soket.close();

                return deSerialize(buffer, sum);
            } catch (Throwable e) {
                L.e(e);
            }
        }

        return null;
    }

    /**
     * 反序列化
     * @param response
     * @param <T>
     * @return
     * @throws ClassNotFoundException
     */
    protected  <T> T deSerialize(byte[] response, int size) throws ClassNotFoundException, UnsupportedEncodingException {
        IPCMessage msg;
        Gson gson = new Gson();
        msg = gson.fromJson(new String(response, 0, size, "UTF-8"), IPCMessage.class);
        if(msg != null && msg.getContent() != null && msg.getResponseClz() != null){
            return (T) gson.fromJson(msg.getContent(), Class.forName(msg.getResponseClz()));
        }
        return null;
    }

    /**
     * 序列化
     * @param object
     * @param responseClz
     * @param <T>
     * @return
     */
    protected <T> byte[] serialize(Object object, String responseClz) {
        Gson gson = new Gson();
        IPCMessage msg = new IPCMessage(gson.toJson(object), object.getClass().getName(), responseClz);
        return gson.toJson(msg).getBytes();
    }

    @Override
    public void post(Object object) {
        Gson gson = new Gson();
        IPCEvent event = new IPCEvent(gson.toJson(object), object.getClass().getName());
        request(event, Integer.class);
    }

    @Override
    public Object handleEvent(IPCEvent event) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(event.getContent(), Class.forName(event.getClz()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
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
                                L.v("Delete:" + f.getAbsolutePath());
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
        File parentDir = portFile.getParentFile();
        if(mPortFile != null && parentDir != null){
            try {
                File[] listFiels = parentDir.listFiles();
                for (File file : listFiels) {
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
        L.v("checkPortVaild: " + port);
        try {
            InetAddress Address = InetAddress.getByName("127.0.0.1");
            Socket socket = new Socket(Address, port);
//            DatagramSocket socket = new DatagramSocket(port);
//            socket.getLocalPort();
            return true;
        } catch (Throwable e) {
            L.e(e);
        }finally{
        }
        return false;
    }

}
