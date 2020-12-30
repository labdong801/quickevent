package com.onlineafterhome.quickevnet;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.onlineafterhome.quickevnet.ipc.QuickIPCSender;
import com.onlineafterhome.quickevnet.ipc.message.IPCEvent;
import com.onlineafterhome.quickevnet.util.L;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QuickEvent implements Runnable {

    private static QuickEvent sInstance = null;

    public static QuickEvent getDefault() {
        if (sInstance == null) {
            synchronized (QuickEvent.class) {
                if (sInstance == null)
                    sInstance = new QuickEvent();
            }
        }
        return sInstance;
    }

    /**
     * Queue size
     */
    private final static int QUEUE_SIZE = 1024;

    /**
     * Consumer limit
     */
    private final static int CONSUMER_SIZE = 4;

    /**
     * Event Queue
     */
    final LinkedBlockingQueue<Object> mEventQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    /**
     * Subscriber
     */
    final List<Object> subscriberList = new ArrayList<>();

    /**
     * Subscriber Lock
     */
    final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();

    /**
     * Event Handler Thread Pool
     */
    final ExecutorService mEventHandlerThreadPool = Executors.newFixedThreadPool(CONSUMER_SIZE);

    /**
     * Event Runner Thread Pool
     */
    final ExecutorService mEventRunnerThreadPool = Executors.newCachedThreadPool();

    /**
     * Main Thread Event Runner
     */
    final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * is Running flag
     */
    final AtomicBoolean isRunning = new AtomicBoolean(true);

    private QuickEvent() {
        mEventHandlerThreadPool.submit(this);
        register(this);
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                Object object = mEventQueue.take();
                handleEvent(object);
            } catch (InterruptedException e) {
                L.e(e);
            }
        }
    }

    public void init(){
        QuickIPCSender.getInstance().init();
    }

    public void register(Object subscriber) {
        try {
            mLock.writeLock().lock();
            subscriberList.add(subscriber);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    public synchronized void unregister(Object subscriber) {
        try {
            mLock.writeLock().lock();
            subscriberList.remove(subscriber);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    /**
     * Send Event
     *
     * @param object event
     */
    public void post(Object object) {
        post(object, true);
    }

    public void post(Object object, boolean route){
        mEventQueue.add(object);
        if(route)
            QuickIPCSender.getInstance().post(object);
    }

    /**
     * Send Request and get Response
     *
     * @param object request
     * @param <T>    response object type
     * @return response
     */
    public <T> T request(Object object, Class<T> t){
        return request(object, t, true);
    }

    public <T> T request(Object object, Class<T> t, boolean route) {
        try {
            mLock.readLock().lock();
            // 1.当前进程
            for (Object subscriber : subscriberList) {
                for (Method method : subscriber.getClass().getMethods()) {
                    QuickService annotation = method.getAnnotation(QuickService.class);
                    Class[] paramsClz = method.getParameterTypes();
                    if ((annotation != null) &&
                            (method.getReturnType().equals(t)) &&
                            (paramsClz.length == 1 && paramsClz[0].equals(object.getClass()))) {
                        try {
                            return (T) method.invoke(subscriber, object);
                        } catch (Throwable e) {
                            L.e(e);
                        }
                    }
                }
            }
            // 2.其他进程
            if(route)
                return QuickIPCSender.getInstance().request(object, t);
        } finally {
            mLock.readLock().unlock();
        }
        return null;
    }

    @QuickService
    public Integer bridgeEventFromOtherProcess(IPCEvent event){
        try {
            Object o = QuickIPCSender.getInstance().handleEvent(event);
            if(o != null)
                mEventQueue.add(o);
        }catch (Throwable e){
            L.e(e);
        }
        return 0;
    }

    private void handleEvent(final Object event) {
        try {
            mLock.readLock().lock();
            // 1.当前进程
            for (final Object subscriber : subscriberList) {
                for (final Method method : subscriber.getClass().getMethods()) {
                    Subscribe annotation = method.getAnnotation(Subscribe.class);
                    Class[] paramsClzs = method.getParameterTypes();
                    if(annotation != null && paramsClzs.length == 1){
                        final Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    method.invoke(subscriber, event);
                                } catch (Throwable e) {
                                    L.e(e);
                                }
                            }
                        };

                        switch (annotation.threadMode()){
                            case MAIN:
                                mMainThreadHandler.post(task);
                                break;
                            default:
                                mEventRunnerThreadPool.submit(task);
                                break;
                        }
                    }
                }
            }
        } finally {
            mLock.readLock().unlock();
        }
    }


}
