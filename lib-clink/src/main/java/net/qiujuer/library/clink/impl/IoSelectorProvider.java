package net.qiujuer.library.clink.impl;

import net.qiujuer.library.clink.core.IoProvider;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: fangcong
 * @date: 2019/5/26
 */
public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    //同步注册过程
    private final AtomicBoolean inRegInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput = new AtomicBoolean(false);
    private Map<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private Map<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();
    private final Selector readHandSelector;
    private final Selector writeHanSelector;
    private final ExecutorService inputThreadPool;
    private final ExecutorService outputThreadPool;

    public IoSelectorProvider() throws IOException {
        this.readHandSelector = Selector.open();
        this.writeHanSelector = Selector.open();
        this.inputThreadPool = Executors.newFixedThreadPool(4,
                new IoPriorityThreadFactory("IoProvider-Input-Thread-"));
        this.outputThreadPool = Executors.newFixedThreadPool(4,
                new IoPriorityThreadFactory("IoProvider-Output-Thread-"));
        //开始输入输出的监听
        startRead();
        startWrite();
    }

    @Override
    public boolean registerInput(SocketChannel socketChannel, HandlerInputCallback handlerInputCallback) {
        return registerSelection(socketChannel, readHandSelector, SelectionKey.OP_READ,
                inputCallbackMap, handlerInputCallback, inRegInput) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel socketChannel, HandlerOutputCallback handlerOutputCallback) {
        return registerSelection(socketChannel, writeHanSelector, SelectionKey.OP_WRITE,
                                    outputCallbackMap, handlerOutputCallback, inRegOutput) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel socketChannel) {
        unRegisterSelection(socketChannel, readHandSelector, inputCallbackMap);
    }

    @Override
    public void unRegisterOutput(SocketChannel socketChannel) {
        unRegisterSelection(socketChannel, writeHanSelector, outputCallbackMap);
    }

    private void waitRegister(AtomicBoolean locker){
        synchronized (locker) {
            if(locker.equals(inRegOutput))
                System.out.println("write wait acquired lock");
            if(locker.equals(inRegInput))
                System.out.println("read wait acquired lock");
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startRead() {
        Thread thread = new Thread("clink IoSelector ReadSelector Thread"){
            @Override
            public void run(){
                while(!isClosed.get()){
                    try{
                        if(readHandSelector.select() == 0){
                            //等待注册完成
                            waitRegister(inRegInput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readHandSelector.selectedKeys();
                        for(SelectionKey key : selectionKeys){
                            if(key.isValid()){
                                System.out.println("channel accept ready");
                                handlerSelection(key, SelectionKey.OP_READ, inputCallbackMap, inputThreadPool);
                            }
                        }
                        selectionKeys.clear();
                    }catch (IOException e){

                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void startWrite() {
        Thread thread = new Thread("clink IoSelector WriteSelector Thread"){
            @Override
            public void run(){
                while(!isClosed.get()){
                    try{
                        if(writeHanSelector.select() == 0){
                            //等待注册完成
                            waitRegister(inRegOutput);
                            continue;
                        }
                        System.out.println("start write 中为可写状态");
                        Set<SelectionKey> selectionKeys = writeHanSelector.selectedKeys();
                        for(SelectionKey key : selectionKeys){
                            if(key.isValid()){
                                handlerSelection(key, SelectionKey.OP_WRITE, outputCallbackMap, outputThreadPool);
                            }
                        }
                        selectionKeys.clear();
                    }catch (IOException e){}
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

//    private SelectionKey registerSelection(SocketChannel channel, Selector selector, int keyOps, Map<SelectionKey, Runnable> map,
//                                           Runnable runnable, AtomicBoolean locker){
//        synchronized (locker){
//            locker.set(true);
//            if(keyOps == 4){
//                System.out.println("write register acquired lock");
//            }
//            if(keyOps == 1)
//                System.out.println("read register acquired lock");
//            //唤醒selector，以便让新注册的channel立即生效
//            selector.wakeup();
//            System.out.println("注册--" + keyOps);
//            SelectionKey key = null;
//            if(channel.isRegistered()){
//                //查询key是否已经注册过
//                key = channel.keyFor(selector);
//                if(key != null){
//                    //让selector重新监听
//                    key.interestOps(key.readyOps() | keyOps);
//                }
//            }
//            if(key == null){
//                try {
//                    //注册selector
//                    key = channel.register(selector, keyOps);
//                    //注册回调
//                    map.put(key, runnable);
//                } catch (ClosedChannelException e) {
//                    e.printStackTrace();
//                    return null;
//                }
//            }
//            if(keyOps == 4)
//            System.out.println("write register released lock");
//            if(keyOps == 1)
//                System.out.println("read register released lock");
//            locker.set(false);
//            locker.notify();
//            return key;
//        }
//    }

    private static SelectionKey registerSelection(SocketChannel channel, Selector selector,
                                                  int registerOps, Map<SelectionKey, Runnable> map,
                                                  Runnable runnable,AtomicBoolean locker) {

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker) {
            // 设置锁定状态
            locker.set(true);

            try {
                // 唤醒当前的selector，让selector不处于select()状态
                selector.wakeup();

                SelectionKey key = null;
                if (channel.isRegistered()) {
                    // 查询是否已经注册过
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if (key == null) {
                    // 注册selector得到Key
                    key = channel.register(selector, registerOps);
                    // 注册回调
                    map.put(key, runnable);
                }

                return key;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                // 解除锁定状态
                locker.set(false);
                try {
                    // 通知
                    locker.notify();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void unRegisterSelection(SocketChannel socketChannel, Selector selector, Map<SelectionKey, Runnable> map){
        if(socketChannel.isRegistered()){
            SelectionKey key = socketChannel.keyFor(selector);
            if(key != null){
                //取消socketChannel上的所有监听事件
                key.cancel();
                map.remove(key);
                //唤醒，让其进行下一轮的监听
                selector.wakeup();
            }
        }
    }
    private void handlerSelection(SelectionKey key, int keyOps, Map<SelectionKey, Runnable> map,
                                  ExecutorService pool){
        //重点
        //取消继续对keyOps的监听, 直到读取完成后在重新监听
        key.interestOps(key.readyOps() & ~keyOps);
        Runnable r = null;
        try{
            r = map.get(key);
        }catch (Exception e){

        }
        if(r != null && !pool.isShutdown()){
            //异步调度
            pool.execute(r);
        }
    }


    @Override
    public void close() {
        if(isClosed.compareAndSet(false, true)){
            inputThreadPool.shutdown();
            outputThreadPool.shutdown();
            inputCallbackMap.clear();
            outputCallbackMap.clear();
            readHandSelector.wakeup();
            writeHanSelector.wakeup();
            CloseUtils.close(readHandSelector, writeHanSelector);
        }
    }
}
class IoPriorityThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    IoPriorityThreadFactory(String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
